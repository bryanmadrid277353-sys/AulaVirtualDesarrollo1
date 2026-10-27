package com.aula.virtual.excel;

import android.content.Context;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.AttendanceState;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.GradeImportLog;
import com.aula.virtual.data.entity.NonSchoolDay;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GradeExcelImporter {

    public static ImportResult importGradesFromStream(Context context, InputStream inputStream, String fileName, Subject targetSubject) throws Exception {
        byte[] fileBytes = readAllBytes(inputStream);
        String fileHash = calculateSHA256(fileBytes);

        AppDatabase db = AppDatabase.getInstance(context);

        Workbook workbook;
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            workbook = new XSSFWorkbook(is);
        } catch (Exception e) {
            return new ImportResult(false, null, null, fileHash, fileName, 0, 1,
                    "El archivo no es un documento Excel (.xlsx) válido: " + e.getMessage(),
                    "Error al abrir archivo Excel.");
        }

        // 1. Read Meta Sheet
        Sheet metaSheet = workbook.getSheet(ExcelTemplateGenerator.META_SHEET_NAME);
        if (metaSheet == null) {
            workbook.close();
            return new ImportResult(false, null, null, fileHash, fileName, 0, 1,
                    "No se encontró la hoja oculta '_meta' de metadatos.",
                    "Archivo no es una plantilla válida de Aula Virtual.");
        }

        String metaSubjectCode = getCellValueAsString(metaSheet.getRow(0), 0);
        String metaSection = getCellValueAsString(metaSheet.getRow(1), 0);
        String metaVersion = getCellValueAsString(metaSheet.getRow(2), 0);

        boolean isVersionValid = "1.0".equals(metaVersion) || "1".equals(metaVersion)
                || "2.0".equals(metaVersion) || "2".equals(metaVersion)
                || "3.0".equals(metaVersion) || "3".equals(metaVersion);

        if (!isVersionValid) {
            workbook.close();
            return new ImportResult(false, metaSubjectCode, metaSection, fileHash, fileName, 0, 1,
                    "Versión de plantilla no soportada (" + metaVersion + "). Se requiere versión 1.0, 2.0 o 3.0",
                    "Versión de plantilla incompatible.");
        }

        if (targetSubject != null && targetSubject.code != null && !targetSubject.code.equalsIgnoreCase(metaSubjectCode)) {
            workbook.close();
            return new ImportResult(false, metaSubjectCode, metaSection, fileHash, fileName, 0, 1,
                    "El código de materia en el archivo (" + metaSubjectCode + ") no coincide con la materia seleccionada (" + targetSubject.code + ").",
                    "Materia del archivo no coincide.");
        }

        int subjectId = targetSubject != null ? targetSubject.id : 0;

        SimpleDateFormat sdfTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdfYmd = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String currentTimestamp = sdfTimestamp.format(new Date());
        String todayYmd = sdfYmd.format(new Date());

        // 2. Read Sheet "Notas"
        Sheet mainSheet = workbook.getSheet("Notas");
        if (mainSheet == null) {
            mainSheet = workbook.getSheetAt(0);
        }

        List<Enrollment> enrollmentsToUpdate = new ArrayList<>();
        StringBuilder errorDetailBuilder = new StringBuilder();
        int rowsOk = 0;
        int rowsError = 0;

        int totalRows = mainSheet.getLastRowNum();
        for (int r = 1; r <= totalRows; r++) {
            Row row = mainSheet.getRow(r);
            if (row == null) continue;

            String carnet = getCellValueAsString(row, 0);
            if (carnet == null || carnet.trim().isEmpty()) {
                continue;
            }
            carnet = carnet.trim();

            Student student = db.studentDao().getStudentByCarnet(carnet);
            if (student == null) {
                rowsError++;
                errorDetailBuilder.append("Notas - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                continue;
            }

            Enrollment enrollment = db.enrollmentDao().getByStudentAndSubject(student.id, subjectId);
            if (enrollment == null) {
                rowsError++;
                errorDetailBuilder.append("Notas - Fila ").append(r + 1).append(": Estudiante '").append(student.name).append("' (").append(carnet).append(") no está inscrito en esta materia.\n");
                continue;
            }

            Double[] parsedGrades = new Double[5];
            boolean rowHasFormatError = false;

            for (int g = 0; g < 5; g++) {
                Cell gradeCell = row.getCell(2 + g);
                if (gradeCell == null || gradeCell.getCellType() == CellType.BLANK) {
                    parsedGrades[g] = null;
                } else {
                    Double gradeVal = parseGradeCellValue(gradeCell);
                    if (gradeVal == null || gradeVal < 0.0 || gradeVal > 10.0) {
                        rowHasFormatError = true;
                        errorDetailBuilder.append("Notas - Fila ").append(r + 1).append(": Nota ").append(g + 1)
                                .append(" inválida para '").append(carnet).append("'. Debe ser un número entre 0.0 y 10.0.\n");
                        break;
                    }
                    parsedGrades[g] = gradeVal;
                }
            }

            if (rowHasFormatError) {
                rowsError++;
                continue;
            }

            enrollment.grade1 = parsedGrades[0];
            enrollment.grade2 = parsedGrades[1];
            enrollment.grade3 = parsedGrades[2];
            enrollment.grade4 = parsedGrades[3];
            enrollment.grade5 = parsedGrades[4];

            enrollmentsToUpdate.add(enrollment);
            rowsOk++;
        }

        // 3. Read Sheet "Conducta" (if present)
        Sheet conductSheet = workbook.getSheet("Conducta");
        boolean hasConductSheet = conductSheet != null;

        List<ConductRecord> conductRecordsToUpsert = new ArrayList<>();
        StringBuilder conductErrorDetailBuilder = new StringBuilder();
        int conductRowsOk = 0;
        int conductRowsError = 0;

        if (hasConductSheet) {
            int totalConductRows = conductSheet.getLastRowNum();
            for (int r = 1; r <= totalConductRows; r++) {
                Row row = conductSheet.getRow(r);
                if (row == null) continue;

                String carnet = getCellValueAsString(row, 0);
                if (carnet == null || carnet.trim().isEmpty()) {
                    continue;
                }
                carnet = carnet.trim();

                Student student = db.studentDao().getStudentByCarnet(carnet);
                if (student == null) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                    continue;
                }

                Enrollment enrollment = db.enrollmentDao().getByStudentAndSubject(student.id, subjectId);
                if (enrollment == null) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Estudiante '").append(student.name).append("' (").append(carnet).append(") no está inscrito en esta materia.\n");
                    continue;
                }

                int period = parsePeriodCellValue(row.getCell(2));
                if (period < 1 || period > 5) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Período inválido '").append(getCellValueAsString(row, 2)).append("' para '").append(carnet).append("'. Debe ser de 1 a 5.\n");
                    continue;
                }

                Integer[] parsedAspects = new Integer[5];
                boolean rowHasAspectError = false;

                for (int a = 0; a < 5; a++) {
                    Cell aspectCell = row.getCell(3 + a);
                    Integer val = parseConductLevelCellValue(aspectCell);
                    if (val != null && val == -1) {
                        rowHasAspectError = true;
                        String levelStr = getCellValueAsString(row, 3 + a);
                        conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Nivel de conducta '").append(levelStr)
                                .append("' inválido en aspecto ").append(a + 1).append(" para '").append(carnet).append("'.\n");
                        break;
                    }
                    parsedAspects[a] = val;
                }

                if (rowHasAspectError) {
                    conductRowsError++;
                    continue;
                }

                String observations = getCellValueAsString(row, 9);
                if (observations != null && observations.trim().isEmpty()) {
                    observations = null;
                }

                ConductRecord record = db.conductRecordDao().getRecord(student.id, subjectId, period);
                if (record == null) {
                    record = new ConductRecord();
                    record.studentId = student.id;
                    record.subjectId = subjectId;
                    record.period = period;
                }

                record.attention_participation = parsedAspects[0];
                record.rules_routines = parsedAspects[1];
                record.self_regulation_effort = parsedAspects[2];
                record.respect_collaboration = parsedAspects[3];
                record.responsibility = parsedAspects[4];
                record.observations = observations;
                record.updatedAt = currentTimestamp;
                record.updateComputedAverage();

                conductRecordsToUpsert.add(record);
                conductRowsOk++;
            }
        }

        // 4. Read Sheet "Justificaciones" (if present)
        Sheet justSheet = workbook.getSheet("Justificaciones");
        Map<String, String> justificationMap = new HashMap<>();

        if (justSheet != null) {
            int totalJustRows = justSheet.getLastRowNum();
            for (int r = 1; r <= totalJustRows; r++) {
                Row row = justSheet.getRow(r);
                if (row == null) continue;

                String carnet = getCellValueAsString(row, 0);
                String rawDateStr = getCellValueAsString(row, 2);
                String reason = getCellValueAsString(row, 3);

                if (carnet != null && !carnet.trim().isEmpty() && rawDateStr != null && !rawDateStr.trim().isEmpty()) {
                    String cleanDate = normalizeDateString(rawDateStr.trim(), sdfYmd);
                    if (cleanDate != null && reason != null && !reason.trim().isEmpty()) {
                        justificationMap.put(carnet.trim() + "_" + cleanDate, reason.trim());
                    }
                }
            }
        }

        // 5. Read Sheet "Asistencia" (if present)
        Sheet attendanceSheet = workbook.getSheet("Asistencia");
        boolean hasAttendanceSheet = attendanceSheet != null;

        List<AttendanceRecord> attendanceRecordsToUpsert = new ArrayList<>();
        StringBuilder attendanceErrorDetailBuilder = new StringBuilder();
        int attendanceRowsOk = 0;
        int attendanceRowsError = 0;

        if (hasAttendanceSheet) {
            Row attHeaderRow = attendanceSheet.getRow(0);
            if (attHeaderRow != null) {
                int lastCellNum = attHeaderRow.getLastCellNum();
                Map<Integer, String> colToDateMap = new HashMap<>();

                // Match date columns starting from index 7
                for (int c = 7; c < lastCellNum; c++) {
                    Cell headerCell = attHeaderRow.getCell(c);
                    if (headerCell == null) continue;
                    String parsedDate = parseHeaderClassDate(headerCell, sdfYmd);
                    if (parsedDate != null) {
                        colToDateMap.put(c, parsedDate);
                    }
                }

                int totalAttRows = attendanceSheet.getLastRowNum();
                for (int r = 1; r <= totalAttRows; r++) {
                    Row row = attendanceSheet.getRow(r);
                    if (row == null) continue;

                    String carnet = getCellValueAsString(row, 0);
                    if (carnet == null || carnet.trim().isEmpty()) {
                        continue;
                    }
                    carnet = carnet.trim();

                    Student student = db.studentDao().getStudentByCarnet(carnet);
                    if (student == null) {
                        attendanceRowsError++;
                        attendanceErrorDetailBuilder.append("Asistencia - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                        continue;
                    }

                    Enrollment enrollment = db.enrollmentDao().getByStudentAndSubject(student.id, subjectId);
                    if (enrollment == null) {
                        attendanceRowsError++;
                        attendanceErrorDetailBuilder.append("Asistencia - Fila ").append(r + 1).append(": Estudiante '").append(student.name).append("' (").append(carnet).append(") no está inscrito en esta materia.\n");
                        continue;
                    }

                    for (Map.Entry<Integer, String> entry : colToDateMap.entrySet()) {
                        int colIdx = entry.getKey();
                        String classDate = entry.getValue();

                        Cell statusCell = row.getCell(colIdx);
                        Integer statusVal = AttendanceState.getValueForLabel(getCellValueAsString(row, colIdx));

                        AttendanceRecord existingRecord = db.attendanceRecordDao().getRecord(student.id, subjectId, classDate);

                        // If status is "Sin evaluar" (0) and NO existing record in DB -> skip inserting
                        if (statusVal == 0 && existingRecord == null) {
                            continue;
                        }

                        // Check warning: Justificado without reason
                        String justification = justificationMap.get(carnet + "_" + classDate);
                        if (statusVal == 3 && (justification == null || justification.isEmpty())) {
                            attendanceErrorDetailBuilder.append("Advertencia - Asistencia Fila ").append(r + 1)
                                    .append(": '").append(carnet).append("' registrado como 'Justificado' el ").append(classDate).append(" sin motivo registrado en hoja Justificaciones.\n");
                        }

                        // Check warning: Status marked on future date
                        if (statusVal != 0 && classDate.compareTo(todayYmd) > 0) {
                            attendanceErrorDetailBuilder.append("Advertencia - Asistencia Fila ").append(r + 1)
                                    .append(": '").append(carnet).append("' tiene asistencia registrada en fecha futura (").append(classDate).append(").\n");
                        }

                        if (existingRecord == null) {
                            existingRecord = new AttendanceRecord();
                            existingRecord.studentId = student.id;
                            existingRecord.subjectId = subjectId;
                            existingRecord.classDate = classDate;
                        }

                        existingRecord.status = statusVal;
                        existingRecord.justification = justification != null ? justification : existingRecord.justification;
                        existingRecord.updatedAt = currentTimestamp;

                        attendanceRecordsToUpsert.add(existingRecord);
                        attendanceRowsOk++;
                    }
                }
            }
        }

        workbook.close();

        // 6. Execute Database Updates in Single Transaction
        db.runInTransaction(() -> {
            for (Enrollment en : enrollmentsToUpdate) {
                db.enrollmentDao().update(en);
            }
            for (ConductRecord cr : conductRecordsToUpsert) {
                db.conductRecordDao().insert(cr);
            }
            for (AttendanceRecord ar : attendanceRecordsToUpsert) {
                db.attendanceRecordDao().insert(ar);
            }
        });

        // 7. Record Import Logs
        String errorDetailNotas = errorDetailBuilder.toString().trim();
        String errorDetailConducta = conductErrorDetailBuilder.toString().trim();
        String errorDetailAsistencia = attendanceErrorDetailBuilder.toString().trim();

        GradeImportLog logNotas = new GradeImportLog(subjectId, "Notas", fileName, fileHash, currentTimestamp, rowsOk, rowsError, errorDetailNotas);
        GradeImportLog logConducta = hasConductSheet
                ? new GradeImportLog(subjectId, "Conducta", fileName, fileHash, currentTimestamp, conductRowsOk, conductRowsError, errorDetailConducta)
                : null;
        GradeImportLog logAsistencia = hasAttendanceSheet
                ? new GradeImportLog(subjectId, "Asistencia", fileName, fileHash, currentTimestamp, attendanceRowsOk, attendanceRowsError, errorDetailAsistencia)
                : null;

        db.runInTransaction(() -> {
            db.gradeImportLogDao().insert(logNotas);
            if (logConducta != null) db.gradeImportLogDao().insert(logConducta);
            if (logAsistencia != null) db.gradeImportLogDao().insert(logAsistencia);
        });

        boolean isSuccess = (rowsOk > 0 || rowsError == 0)
                && (!hasConductSheet || conductRowsOk > 0 || conductRowsError == 0)
                && (!hasAttendanceSheet || attendanceRowsOk > 0 || attendanceRowsError == 0);

        StringBuilder messageBuilder = new StringBuilder("Importación completada - ");
        messageBuilder.append("Notas: ").append(rowsOk).append(" ok, ").append(rowsError).append(" err.");
        if (hasConductSheet) {
            messageBuilder.append(" Conducta: ").append(conductRowsOk).append(" ok, ").append(conductRowsError).append(" err.");
        }
        if (hasAttendanceSheet) {
            messageBuilder.append(" Asistencia: ").append(attendanceRowsOk).append(" ok, ").append(attendanceRowsError).append(" err.");
        }

        StringBuilder combinedErrorDetail = new StringBuilder(errorDetailNotas);
        if (hasConductSheet && !errorDetailConducta.isEmpty()) {
            if (combinedErrorDetail.length() > 0) combinedErrorDetail.append("\n");
            combinedErrorDetail.append(errorDetailConducta);
        }
        if (hasAttendanceSheet && !errorDetailAsistencia.isEmpty()) {
            if (combinedErrorDetail.length() > 0) combinedErrorDetail.append("\n");
            combinedErrorDetail.append(errorDetailAsistencia);
        }

        int totalOk = rowsOk + conductRowsOk + attendanceRowsOk;
        int totalErr = rowsError + conductRowsError + attendanceRowsError;

        return new ImportResult(isSuccess, metaSubjectCode, metaSection, fileHash, fileName, totalOk, totalErr, combinedErrorDetail.toString().trim(), messageBuilder.toString());
    }

    private static String parseHeaderClassDate(Cell cell, SimpleDateFormat sdfYmd) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            if (d != null) return sdfYmd.format(d);
        }

        String raw = cell.toString().trim();
        return normalizeDateString(raw, sdfYmd);
    }

    private static String normalizeDateString(String raw, SimpleDateFormat sdfYmd) {
        if (raw == null || raw.isEmpty()) return null;

        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return raw;
        }

        try {
            Date d = sdfYmd.parse(raw);
            if (d != null) return sdfYmd.format(d);
        } catch (Exception ignored) {}

        // Try extracting yyyy-MM-dd pattern from string
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(raw);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    public static Integer parseConductLevelCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            int val = (int) cell.getNumericCellValue();
            if (val >= 1 && val <= 5) return val;
            return -1;
        }
        if (cell.getCellType() == CellType.STRING) {
            String str = cell.getStringCellValue();
            if (str == null || str.trim().isEmpty()) return null;
            Integer val = ConductLevel.getValueForLabel(str);
            if (val != null) return val;
            try {
                int num = Integer.parseInt(str.trim());
                if (num >= 1 && num <= 5) return num;
            } catch (NumberFormatException ignored) {}
            return -1;
        }
        return -1;
    }

    private static int parsePeriodCellValue(Cell cell) {
        if (cell == null) return -1;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static Double parseGradeCellValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                String str = cell.getStringCellValue().trim();
                if (str.isEmpty()) return null;
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String getCellValueAsString(Row row, int cellIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString();
    }

    public static String calculateSHA256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
