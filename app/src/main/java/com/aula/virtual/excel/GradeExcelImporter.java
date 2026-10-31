package com.aula.virtual.excel;

import android.content.Context;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.AttendanceState;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.GradeImportLog;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.ui.ThemeHelper;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GradeExcelImporter {

    public static ImportResult importGradesFromStream(Context context, InputStream inputStream, String fileName, Subject targetSubject) throws Exception {
        byte[] fileBytes = readAllBytes(inputStream);
        String fileHash = calculateSHA256(fileBytes);

        boolean isLocal = context != null && ThemeHelper.isLocalMode(context);
        AppDatabase db = context != null ? AppDatabase.getInstance(context) : null;

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

        // Pre-fetch server data in Server Mode
        Map<String, Integer> serverStudentIdMap = new HashMap<>();
        Map<Integer, Enrollment> serverEnrollmentMap = new HashMap<>();
        Map<String, ConductRecord> serverConductMap = new HashMap<>();
        Map<String, AttendanceRecord> serverAttendanceMap = new HashMap<>();

        if (!isLocal && context != null) {
            try {
                com.aula.virtual.data.ApiService api = com.aula.virtual.data.RetrofitClient.getApiService(context);
                try {
                    retrofit2.Response<List<com.aula.virtual.data.StudentGradeInfo>> enResp = api.getSubjectEnrollments(subjectId).execute();
                    if (enResp.isSuccessful() && enResp.body() != null) {
                        for (com.aula.virtual.data.StudentGradeInfo info : enResp.body()) {
                            if (info.student != null && info.student.carnet != null) {
                                String c = info.student.carnet.trim().toUpperCase(Locale.ROOT);
                                serverStudentIdMap.put(c, info.student.id);
                                if (c.startsWith("EST")) {
                                    serverStudentIdMap.put(c.substring(3), info.student.id);
                                } else {
                                    serverStudentIdMap.put("EST" + c, info.student.id);
                                }
                            }
                            if (info.student != null && info.enrollment != null) {
                                serverEnrollmentMap.put(info.student.id, info.enrollment);
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    retrofit2.Response<List<Student>> stResp = api.getAllStudentRecords().execute();
                    if (stResp.isSuccessful() && stResp.body() != null) {
                        for (Student st : stResp.body()) {
                            if (st.carnet != null) {
                                String c = st.carnet.trim().toUpperCase(Locale.ROOT);
                                serverStudentIdMap.put(c, st.id);
                                if (c.startsWith("EST")) {
                                    serverStudentIdMap.put(c.substring(3), st.id);
                                } else {
                                    serverStudentIdMap.put("EST" + c, st.id);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    retrofit2.Response<List<User>> usrResp = api.getStudents().execute();
                    if (usrResp.isSuccessful() && usrResp.body() != null) {
                        for (User u : usrResp.body()) {
                            if (u.carnet != null) {
                                String c = u.carnet.trim().toUpperCase(Locale.ROOT);
                                serverStudentIdMap.put(c, u.id);
                                if (c.startsWith("EST")) {
                                    serverStudentIdMap.put(c.substring(3), u.id);
                                } else {
                                    serverStudentIdMap.put("EST" + c, u.id);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    retrofit2.Response<List<ConductRecord>> condResp = api.getSubjectConductRecords(subjectId).execute();
                    if (condResp.isSuccessful() && condResp.body() != null) {
                        for (ConductRecord cr : condResp.body()) {
                            serverConductMap.put(cr.studentId + "_" + cr.period, cr);
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    retrofit2.Response<List<AttendanceRecord>> attResp = api.getSubjectAttendanceRecords(subjectId).execute();
                    if (attResp.isSuccessful() && attResp.body() != null) {
                        for (AttendanceRecord ar : attResp.body()) {
                            serverAttendanceMap.put(ar.studentId + "_" + ar.classDate, ar);
                        }
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }

        // 2. Read Sheet "Notas"
        Sheet mainSheet = workbook.getSheet("Notas");
        if (mainSheet == null) {
            mainSheet = workbook.getSheetAt(0);
        }

        int activePeriodsCount = 5;
        if (isLocal && db != null) {
            try {
                List<AcademicPeriod> activeList = db.academicCalendarDao().getAllPeriods();
                if (activeList != null && !activeList.isEmpty()) {
                    activePeriodsCount = Math.min(activeList.size(), 5);
                }
            } catch (Exception ignored) {}
        }

        Row mainHeaderRow = mainSheet.getRow(0);
        if (mainHeaderRow != null) {
            int countInHeader = 0;
            for (int c = 2; c < mainHeaderRow.getLastCellNum(); c++) {
                String hVal = getCellValueAsString(mainHeaderRow, c);
                if (hVal != null && hVal.toLowerCase().contains("nota")) {
                    countInHeader++;
                } else if (hVal != null && hVal.toLowerCase().contains("promedio")) {
                    break;
                }
            }
            if (countInHeader > 0) {
                activePeriodsCount = Math.min(countInHeader, 5);
            }
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

            Integer targetStudentId = null;
            if (isLocal && db != null) {
                Student student = findStudentByCarnet(db, carnet);
                if (student != null) {
                    targetStudentId = student.id;
                }
            } else {
                targetStudentId = findServerStudentId(serverStudentIdMap, carnet);
            }

            if (targetStudentId == null) {
                rowsError++;
                errorDetailBuilder.append("Notas - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                continue;
            }

            Enrollment enrollment;
            if (isLocal && db != null) {
                enrollment = db.enrollmentDao().getByStudentAndSubject(targetStudentId, subjectId);
                if (enrollment == null) {
                    enrollment = new Enrollment();
                    enrollment.studentId = targetStudentId;
                    enrollment.subjectId = subjectId;
                    db.enrollmentDao().insert(enrollment);
                    enrollment = db.enrollmentDao().getByStudentAndSubject(targetStudentId, subjectId);
                }
            } else {
                enrollment = serverEnrollmentMap.get(targetStudentId);
                if (enrollment == null) {
                    enrollment = new Enrollment();
                    enrollment.studentId = targetStudentId;
                    enrollment.subjectId = subjectId;
                    enrollment.id = 0;
                }
            }

            if (enrollment == null) {
                rowsError++;
                errorDetailBuilder.append("Notas - Fila ").append(r + 1).append(": No se pudo resolver la inscripción de '").append(carnet).append("'.\n");
                continue;
            }

            Double[] parsedGrades = new Double[5];
            boolean rowHasFormatError = false;

            for (int g = 0; g < activePeriodsCount; g++) {
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

                Integer targetStudentId = null;
                if (isLocal && db != null) {
                    Student student = findStudentByCarnet(db, carnet);
                    if (student != null) targetStudentId = student.id;
                } else {
                    targetStudentId = findServerStudentId(serverStudentIdMap, carnet);
                }

                if (targetStudentId == null) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                    continue;
                }

                int period = parsePeriodCellValue(row.getCell(2));
                if (period < 1 || period > activePeriodsCount) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Conducta - Fila ").append(r + 1).append(": Período inválido '").append(getCellValueAsString(row, 2)).append("' para '").append(carnet).append("'. Debe estar entre 1 y ").append(activePeriodsCount).append(".\n");
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

                ConductRecord record;
                if (isLocal && db != null) {
                    record = db.conductRecordDao().getRecord(targetStudentId, subjectId, period);
                } else {
                    record = serverConductMap.get(targetStudentId + "_" + period);
                }

                if (record == null) {
                    record = new ConductRecord();
                    record.studentId = targetStudentId;
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
                    if (cleanDate != null) {
                        String finalReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Ausencia Justificada";
                        justificationMap.put(carnet.trim() + "_" + cleanDate, finalReason);
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

                    Integer targetStudentId = null;
                    if (isLocal && db != null) {
                        Student student = findStudentByCarnet(db, carnet);
                        if (student != null) targetStudentId = student.id;
                    } else {
                        targetStudentId = findServerStudentId(serverStudentIdMap, carnet);
                    }

                    if (targetStudentId == null) {
                        attendanceRowsError++;
                        attendanceErrorDetailBuilder.append("Asistencia - Fila ").append(r + 1).append(": Carnet '").append(carnet).append("' no existe en el sistema.\n");
                        continue;
                    }

                    for (Map.Entry<Integer, String> entry : colToDateMap.entrySet()) {
                        int colIdx = entry.getKey();
                        String classDate = entry.getValue();

                        Cell statusCell = row.getCell(colIdx);
                        Integer statusVal = AttendanceState.getValueForLabel(getCellValueAsString(row, colIdx));

                        AttendanceRecord existingRecord;
                        if (isLocal && db != null) {
                            existingRecord = db.attendanceRecordDao().getRecord(targetStudentId, subjectId, classDate);
                        } else {
                            existingRecord = serverAttendanceMap.get(targetStudentId + "_" + classDate);
                        }

                        if (statusVal == 0 && existingRecord == null) {
                            continue;
                        }

                        String justification = justificationMap.get(carnet + "_" + classDate);
                        if (statusVal == 3 && (justification == null || justification.trim().isEmpty())) {
                            justification = "Ausencia Justificada";
                        }

                        if (statusVal != 0 && classDate.compareTo(todayYmd) > 0) {
                            attendanceErrorDetailBuilder.append("Advertencia - Asistencia Fila ").append(r + 1)
                                    .append(": '").append(carnet).append("' tiene asistencia registrada en fecha futura (").append(classDate).append(").\n");
                        }

                        if (existingRecord == null) {
                            existingRecord = new AttendanceRecord();
                            existingRecord.studentId = targetStudentId;
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

        // 6. Execute Updates
        if (isLocal) {
            if (db != null) {
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
            }
        } else if (context != null) {
            com.aula.virtual.data.ApiService api = com.aula.virtual.data.RetrofitClient.getApiService(context);

            for (Enrollment en : enrollmentsToUpdate) {
                try {
                    retrofit2.Response<Enrollment> resp;
                    if (en.id != 0) {
                        resp = api.updateEnrollment(en.id, en).execute();
                    } else {
                        resp = api.enrollStudent(en).execute();
                    }
                    if (!resp.isSuccessful()) {
                        rowsError++;
                        errorDetailBuilder.append("Error servidor al actualizar nota de estudiante ").append(en.studentId).append(" (Código ").append(resp.code()).append(").\n");
                    }
                } catch (Exception e) {
                    rowsError++;
                    errorDetailBuilder.append("Error de conexión al actualizar notas: ").append(e.getMessage()).append("\n");
                }
            }

            for (ConductRecord cr : conductRecordsToUpsert) {
                try {
                    retrofit2.Response<ConductRecord> resp = api.upsertConductRecord(cr).execute();
                    if (!resp.isSuccessful()) {
                        conductRowsError++;
                        conductErrorDetailBuilder.append("Error servidor al actualizar conducta para estudiante ").append(cr.studentId).append(".\n");
                    }
                } catch (Exception e) {
                    conductRowsError++;
                    conductErrorDetailBuilder.append("Error de conexión al actualizar conducta: ").append(e.getMessage()).append("\n");
                }
            }

            for (AttendanceRecord ar : attendanceRecordsToUpsert) {
                try {
                    retrofit2.Response<AttendanceRecord> resp = api.saveAttendanceRecord(ar).execute();
                    if (!resp.isSuccessful()) {
                        attendanceRowsError++;
                        attendanceErrorDetailBuilder.append("Error servidor al actualizar asistencia del ").append(ar.classDate).append(".\n");
                    }
                } catch (Exception e) {
                    attendanceRowsError++;
                    attendanceErrorDetailBuilder.append("Error de conexión al actualizar asistencia: ").append(e.getMessage()).append("\n");
                }
            }
        }

        String errorDetailNotas = errorDetailBuilder.toString().trim();
        String errorDetailConducta = conductErrorDetailBuilder.toString().trim();
        String errorDetailAsistencia = attendanceErrorDetailBuilder.toString().trim();

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

    private static Integer findServerStudentId(Map<String, Integer> serverStudentIdMap, String rawCarnet) {
        if (rawCarnet == null || rawCarnet.trim().isEmpty()) return null;
        String clean = rawCarnet.trim().toUpperCase(Locale.ROOT);
        Integer id = serverStudentIdMap.get(clean);
        if (id != null) return id;
        if (clean.startsWith("EST")) {
            id = serverStudentIdMap.get(clean.substring(3));
        } else {
            id = serverStudentIdMap.get("EST" + clean);
        }
        return id;
    }

    private static String parseHeaderClassDate(Cell cell, SimpleDateFormat sdfYmd) {
        if (cell == null) return null;

        try {
            if (cell.getCellComment() != null && cell.getCellComment().getString() != null) {
                String commentText = cell.getCellComment().getString().getString().trim();
                String isoDate = normalizeDateString(commentText, sdfYmd);
                if (isoDate != null) return isoDate;
            }
        } catch (Exception ignored) {}

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                if (d != null) return sdfYmd.format(d);
            }
        } catch (Exception ignored) {}

        String raw = cell.toString().trim();
        return normalizeDateString(raw, sdfYmd);
    }

    private static String normalizeDateString(String raw, SimpleDateFormat sdfYmd) {
        if (raw == null || raw.trim().isEmpty()) return null;
        raw = raw.trim();

        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return raw;
        }

        java.util.regex.Matcher mIso = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(raw);
        if (mIso.find()) {
            return mIso.group(1);
        }

        try {
            Date d = sdfYmd.parse(raw);
            if (d != null) return sdfYmd.format(d);
        } catch (Exception ignored) {}

        java.util.regex.Matcher mFullDate = java.util.regex.Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2,4})").matcher(raw);
        if (mFullDate.find()) {
            try {
                int day = Integer.parseInt(mFullDate.group(1));
                int month = Integer.parseInt(mFullDate.group(2));
                int year = Integer.parseInt(mFullDate.group(3));
                if (year < 100) year += 2000;
                return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
            } catch (Exception ignored) {}
        }

        java.util.regex.Matcher mShortDate = java.util.regex.Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})").matcher(raw);
        if (mShortDate.find()) {
            try {
                int day = Integer.parseInt(mShortDate.group(1));
                int month = Integer.parseInt(mShortDate.group(2));
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                return String.format(Locale.US, "%04d-%02d-%02d", currentYear, month, day);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private static Student findStudentByCarnet(AppDatabase db, String rawCarnet) {
        if (db == null || rawCarnet == null || rawCarnet.trim().isEmpty()) return null;
        String clean = rawCarnet.trim();
        String carnetFormatted = clean.toUpperCase(Locale.ROOT);
        if (!carnetFormatted.startsWith("EST") && carnetFormatted.matches("\\d+")) {
            carnetFormatted = "EST" + carnetFormatted;
        }

        // 1. Search in studentDao
        Student student = db.studentDao().getStudentByCarnet(clean);
        if (student != null) return student;

        student = db.studentDao().getStudentByCarnet(carnetFormatted);
        if (student != null) return student;

        List<Student> all = db.studentDao().getAllStudents();
        if (all != null) {
            for (Student s : all) {
                if (s.carnet != null && (s.carnet.equalsIgnoreCase(clean) || s.carnet.equalsIgnoreCase(carnetFormatted))) {
                    return s;
                }
            }
        }

        // 2. Search in userDao (Users table)
        User u = db.userDao().getUserByCarnetOrName(clean);
        if (u == null) u = db.userDao().getUserByCarnetOrName(carnetFormatted);
        if (u == null) {
            List<User> allUsers = db.userDao().getAllUsers();
            if (allUsers != null) {
                for (User user : allUsers) {
                    if (user.carnet != null && (user.carnet.equalsIgnoreCase(clean) || user.carnet.equalsIgnoreCase(carnetFormatted))) {
                        u = user;
                        break;
                    }
                }
            }
        }

        if (u != null) {
            Student s = new Student(u.carnet, u.name, "5º Primaria", null);
            s.id = u.id;
            try {
                db.studentDao().insert(s);
            } catch (Exception ignored) {}
            Student inserted = db.studentDao().getStudentById(u.id);
            if (inserted != null) return inserted;
            return s;
        }

        return null;
    }

    public static Integer parseConductLevelCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.BLANK) return null;

        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        if (type == CellType.NUMERIC) {
            int val = (int) cell.getNumericCellValue();
            if (val >= 1 && val <= 5) return val;
            return -1;
        }

        String str = getCellStringValue(cell);
        if (str == null || str.trim().isEmpty()) return null;

        Integer val = ConductLevel.getValueForLabel(str);
        if (val != null) return val;

        try {
            int num = Integer.parseInt(str.trim());
            if (num >= 1 && num <= 5) return num;
        } catch (NumberFormatException ignored) {}

        return -1;
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) {
            return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
        }
        if (type == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    private static int parsePeriodCellValue(Cell cell) {
        if (cell == null) return -1;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (type == CellType.STRING) {
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
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (type == CellType.STRING) {
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
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) {
            return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
        } else if (type == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return cell.toString().trim();
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
