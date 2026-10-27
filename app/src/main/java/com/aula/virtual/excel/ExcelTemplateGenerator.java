package com.aula.virtual.excel;

import android.content.Context;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.AttendanceState;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.NonSchoolDay;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExcelTemplateGenerator {
    public static final String TEMPLATE_VERSION = "3.0";
    public static final String META_SHEET_NAME = "_meta";

    public static void generateTemplate(Context context, Subject subject, List<StudentGradeInfo> gradeInfos, OutputStream outputStream) throws Exception {
        if (context != null) {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<AcademicPeriod> periods = db.academicCalendarDao().getAllPeriods();
                if (periods == null || periods.size() < 5) {
                    throw new IllegalStateException("No se ha configurado el Calendario Académico (Períodos 1 a 5). Ingrese desde el menú Administrador -> Calendario Académico para definir las fechas del ciclo escolar.");
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception ignored) {}
        }

        Workbook workbook = new XSSFWorkbook();

        // Styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));

        // 1. Sheet "Notas"
        Sheet mainSheet = workbook.createSheet("Notas");
        Row headerRow = mainSheet.createRow(0);
        String[] headers = {"Carnet", "Nombre", "Nota1", "Nota2", "Nota3", "Nota4", "Nota5", "Promedio"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        if (gradeInfos != null) {
            for (StudentGradeInfo info : gradeInfos) {
                if (info == null || info.student == null) continue;
                Row row = mainSheet.createRow(rowIndex);
                int excelRowNum = rowIndex + 1;

                Cell cellCarnet = row.createCell(0);
                cellCarnet.setCellValue(info.student.carnet != null ? info.student.carnet : "");

                Cell cellNombre = row.createCell(1);
                cellNombre.setCellValue(info.student.name != null ? info.student.name : "");

                Double[] grades = {
                        info.enrollment != null ? info.enrollment.grade1 : null,
                        info.enrollment != null ? info.enrollment.grade2 : null,
                        info.enrollment != null ? info.enrollment.grade3 : null,
                        info.enrollment != null ? info.enrollment.grade4 : null,
                        info.enrollment != null ? info.enrollment.grade5 : null
                };

                for (int g = 0; g < 5; g++) {
                    Cell cellGrade = row.createCell(2 + g);
                    if (grades[g] != null) {
                        cellGrade.setCellValue(grades[g]);
                    }
                }

                Cell cellAvg = row.createCell(7);
                String formula = "IF(COUNT(C" + excelRowNum + ":G" + excelRowNum + ")=0,\"\",AVERAGE(C" + excelRowNum + ":G" + excelRowNum + "))";
                cellAvg.setCellFormula(formula);

                rowIndex++;
            }
        }

        if (rowIndex > 1) {
            DataValidationHelper validationHelper = mainSheet.getDataValidationHelper();
            DataValidationConstraint constraint = validationHelper.createDecimalConstraint(
                    DataValidationConstraint.OperatorType.BETWEEN, "0.0", "10.0"
            );
            CellRangeAddressList addressList = new CellRangeAddressList(1, rowIndex - 1, 2, 6);
            DataValidation validation = validationHelper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.createErrorBox("Nota Inválida", "La nota debe ser un número decimal entre 0.0 y 10.0");
            mainSheet.addValidationData(validation);

            mainSheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 7));
        }
        mainSheet.createFreezePane(0, 1);

        mainSheet.setColumnWidth(0, 16 * 256);
        mainSheet.setColumnWidth(1, 32 * 256);
        mainSheet.setColumnWidth(2, 12 * 256);
        mainSheet.setColumnWidth(3, 12 * 256);
        mainSheet.setColumnWidth(4, 12 * 256);
        mainSheet.setColumnWidth(5, 12 * 256);
        mainSheet.setColumnWidth(6, 12 * 256);
        mainSheet.setColumnWidth(7, 14 * 256);

        // 2. Sheet "Conducta"
        Sheet conductSheet = workbook.createSheet("Conducta");
        Row conductHeaderRow = conductSheet.createRow(0);
        String[] conductHeaders = {
                "Carnet", "Nombre", "Periodo",
                "Atención y participación", "Normas y rutinas",
                "Autorregulación y esfuerzo", "Respeto y colaboración",
                "Responsabilidad", "Promedio", "Observaciones"
        };

        for (int i = 0; i < conductHeaders.length; i++) {
            Cell cell = conductHeaderRow.createCell(i);
            cell.setCellValue(conductHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        Drawing<?> drawing = conductSheet.createDrawingPatriarch();
        CreationHelper factory = workbook.getCreationHelper();
        String[] aspectComments = {
                "Atención y participación: escucha, sigue instrucciones, participa y mantiene el foco en tareas curriculares.",
                "Normas y rutinas: respeta turnos, tiempos, silencios, organización de la clase y procedimientos establecidos.",
                "Autorregulación y esfuerzo: persiste ante dificultades, controla impulsos, espera turnos y maneja la frustración en ejercicios y evaluaciones.",
                "Respeto y colaboración: trabaja en equipo, escucha, comparte materiales, respeta opiniones y resuelve conflictos adecuadamente.",
                "Responsabilidad: cuida sus útiles, cumple tareas, trae lo necesario, entrega a tiempo y sigue indicaciones en evaluaciones."
        };

        for (int a = 0; a < 5; a++) {
            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(3 + a);
            anchor.setCol2(3 + a + 2);
            anchor.setRow1(0);
            anchor.setRow2(3);

            Comment comment = drawing.createCellComment(anchor);
            comment.setString(factory.createRichTextString(aspectComments[a]));
            Cell cell = conductHeaderRow.getCell(3 + a);
            cell.setCellComment(comment);
        }

        Map<String, ConductRecord> conductMap = new HashMap<>();
        if (context != null && subject != null) {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<ConductRecord> existingRecords = db.conductRecordDao().getRecordsBySubject(subject.id);
                if (existingRecords != null) {
                    for (ConductRecord cr : existingRecords) {
                        conductMap.put(cr.studentId + "_" + cr.period, cr);
                    }
                }
            } catch (Exception ignored) {}
        }

        int conductRowIndex = 1;
        if (gradeInfos != null) {
            for (StudentGradeInfo info : gradeInfos) {
                if (info == null || info.student == null) continue;

                for (int period = 1; period <= 5; period++) {
                    Row row = conductSheet.createRow(conductRowIndex);
                    int excelRowNum = conductRowIndex + 1;

                    Cell cellCarnet = row.createCell(0);
                    cellCarnet.setCellValue(info.student.carnet != null ? info.student.carnet : "");

                    Cell cellNombre = row.createCell(1);
                    cellNombre.setCellValue(info.student.name != null ? info.student.name : "");

                    Cell cellPeriod = row.createCell(2);
                    cellPeriod.setCellValue(period);

                    ConductRecord record = conductMap.get(info.student.id + "_" + period);

                    Integer[] aspectValues = {
                            record != null ? record.attention_participation : null,
                            record != null ? record.rules_routines : null,
                            record != null ? record.self_regulation_effort : null,
                            record != null ? record.respect_collaboration : null,
                            record != null ? record.responsibility : null
                    };

                    for (int a = 0; a < 5; a++) {
                        Cell aspectCell = row.createCell(3 + a);
                        String label = ConductLevel.getLabelForValue(aspectValues[a]);
                        if (!"Sin evaluar".equals(label)) {
                            aspectCell.setCellValue(label);
                        }
                    }

                    Cell cellAvg = row.createCell(8);
                    String conductFormula = "IF(COUNTA(D" + excelRowNum + ":H" + excelRowNum + ")=0,\"\",(COUNTIF(D" + excelRowNum + ":H" + excelRowNum + ",\"Excelente\")*5+COUNTIF(D" + excelRowNum + ":H" + excelRowNum + ",\"Muy bueno\")*4+COUNTIF(D" + excelRowNum + ":H" + excelRowNum + ",\"Bueno\")*3+COUNTIF(D" + excelRowNum + ":H" + excelRowNum + ",\"Regular\")*2+COUNTIF(D" + excelRowNum + ":H" + excelRowNum + ",\"Malo\")*1)/COUNTA(D" + excelRowNum + ":H" + excelRowNum + "))";
                    cellAvg.setCellFormula(conductFormula);

                    Cell cellObs = row.createCell(9);
                    cellObs.setCellValue(record != null && record.observations != null ? record.observations : "");

                    conductRowIndex++;
                }
            }
        }

        if (conductRowIndex > 1) {
            DataValidationHelper validationHelper = conductSheet.getDataValidationHelper();
            DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(
                    new String[]{"Excelente", "Muy bueno", "Bueno", "Regular", "Malo"}
            );
            CellRangeAddressList addressList = new CellRangeAddressList(1, conductRowIndex - 1, 3, 7);
            DataValidation validation = validationHelper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.createErrorBox("Nivel Inválido", "Seleccione un nivel válido de la lista desplegable.");
            conductSheet.addValidationData(validation);

            SheetConditionalFormatting conditionalFormatting = conductSheet.getSheetConditionalFormatting();

            ConditionalFormattingRule ruleExcelente = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Excelente\"");
            PatternFormatting fillExcelente = ruleExcelente.createPatternFormatting();
            fillExcelente.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            fillExcelente.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleMuyBueno = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Muy bueno\"");
            PatternFormatting fillMuyBueno = ruleMuyBueno.createPatternFormatting();
            fillMuyBueno.setFillBackgroundColor(IndexedColors.LIME.getIndex());
            fillMuyBueno.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleBueno = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Bueno\"");
            PatternFormatting fillBueno = ruleBueno.createPatternFormatting();
            fillBueno.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            fillBueno.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleRegular = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Regular\"");
            PatternFormatting fillRegular = ruleRegular.createPatternFormatting();
            fillRegular.setFillBackgroundColor(IndexedColors.GOLD.getIndex());
            fillRegular.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleMalo = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Malo\"");
            PatternFormatting fillMalo = ruleMalo.createPatternFormatting();
            fillMalo.setFillBackgroundColor(IndexedColors.ROSE.getIndex());
            fillMalo.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            CellRangeAddress[] regions = { new CellRangeAddress(1, conductRowIndex - 1, 3, 7) };
            conditionalFormatting.addConditionalFormatting(regions, ruleExcelente);
            conditionalFormatting.addConditionalFormatting(regions, ruleMuyBueno);
            conditionalFormatting.addConditionalFormatting(regions, ruleBueno);
            conditionalFormatting.addConditionalFormatting(regions, ruleRegular);
            conditionalFormatting.addConditionalFormatting(regions, ruleMalo);

            conductSheet.setAutoFilter(new CellRangeAddress(0, conductRowIndex - 1, 0, 9));
        }

        conductSheet.createFreezePane(0, 1);

        conductSheet.setColumnWidth(0, 16 * 256);
        conductSheet.setColumnWidth(1, 32 * 256);
        conductSheet.setColumnWidth(2, 10 * 256);
        conductSheet.setColumnWidth(3, 24 * 256);
        conductSheet.setColumnWidth(4, 20 * 256);
        conductSheet.setColumnWidth(5, 26 * 256);
        conductSheet.setColumnWidth(6, 24 * 256);
        conductSheet.setColumnWidth(7, 20 * 256);
        conductSheet.setColumnWidth(8, 14 * 256);
        conductSheet.setColumnWidth(9, 35 * 256);

        // 3. Sheet "Asistencia"
        Sheet attendanceSheet = workbook.createSheet("Asistencia");

        // Calculate Class Dates
        Set<String> nonSchoolDaysSet = new HashSet<>();
        List<AcademicPeriod> periodsList = new ArrayList<>();
        Set<Integer> scheduleDaysOfWeek = new HashSet<>();
        Map<String, AttendanceRecord> existingAttendanceMap = new HashMap<>();

        if (context != null && subject != null) {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                periodsList = db.academicCalendarDao().getAllPeriods();

                List<NonSchoolDay> nsdList = db.academicCalendarDao().getAllNonSchoolDays();
                if (nsdList != null) {
                    for (NonSchoolDay nsd : nsdList) {
                        if (nsd.day != null) nonSchoolDaysSet.add(nsd.day);
                    }
                }

                List<FacilitySchedule> schedules = db.facilityScheduleDao().getBySubjectId(subject.id);
                if (schedules != null) {
                    for (FacilitySchedule fs : schedules) {
                        scheduleDaysOfWeek.addAll(parseScheduleDays(fs.days));
                    }
                }

                List<AttendanceRecord> existingAttRecords = db.attendanceRecordDao().getRecordsBySubject(subject.id);
                if (existingAttRecords != null) {
                    for (AttendanceRecord ar : existingAttRecords) {
                        existingAttendanceMap.put(ar.studentId + "_" + ar.classDate, ar);
                    }
                }
            } catch (Exception ignored) {}
        }

        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEE dd/MM", new Locale("es", "ES"));

        TreeSet<String> classDatesSet = new TreeSet<>();

        if (periodsList != null && !scheduleDaysOfWeek.isEmpty()) {
            for (AcademicPeriod period : periodsList) {
                if (period.startDate == null || period.endDate == null) continue;
                try {
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(ymdFormat.parse(period.startDate));

                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(ymdFormat.parse(period.endDate));

                    while (!startCal.after(endCal)) {
                        int dow = startCal.get(Calendar.DAY_OF_WEEK);
                        String dateStr = ymdFormat.format(startCal.getTime());

                        if (scheduleDaysOfWeek.contains(dow) && !nonSchoolDaysSet.contains(dateStr)) {
                            classDatesSet.add(dateStr);
                        }
                        startCal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Preserve dates with existing data that disappeared from schedule
        for (AttendanceRecord ar : existingAttendanceMap.values()) {
            if (ar.classDate != null && !classDatesSet.contains(ar.classDate)) {
                classDatesSet.add(ar.classDate);
            }
        }

        List<String> sortedClassDates = new ArrayList<>(classDatesSet);

        // Header Row for Asistencia
        Row attHeaderRow = attendanceSheet.createRow(0);
        String[] fixedAttHeaders = {"Carnet", "Nombre", "Asistió", "No asistió", "Justificado", "Sin evaluar", "% Asistencia"};
        for (int i = 0; i < fixedAttHeaders.length; i++) {
            Cell cell = attHeaderRow.createCell(i);
            cell.setCellValue(fixedAttHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < sortedClassDates.size(); i++) {
            Cell cell = attHeaderRow.createCell(7 + i);
            String rawDateStr = sortedClassDates.get(i);
            try {
                Date d = ymdFormat.parse(rawDateStr);
                cell.setCellValue(d != null ? headerDateFormat.format(d) : rawDateStr);
            } catch (Exception e) {
                cell.setCellValue(rawDateStr);
            }
            cell.setCellStyle(headerStyle);
        }

        int attRowIndex = 1;
        if (gradeInfos != null) {
            for (StudentGradeInfo info : gradeInfos) {
                if (info == null || info.student == null) continue;
                Row row = attendanceSheet.createRow(attRowIndex);
                int excelRowNum = attRowIndex + 1;

                Cell cellCarnet = row.createCell(0);
                cellCarnet.setCellValue(info.student.carnet != null ? info.student.carnet : "");

                Cell cellNombre = row.createCell(1);
                cellNombre.setCellValue(info.student.name != null ? info.student.name : "");

                String lastColLetter = sortedClassDates.isEmpty() ? "H" : getExcelColumnName(7 + sortedClassDates.size() - 1);

                // Formulas:
                Cell cellAtt = row.createCell(2);
                cellAtt.setCellFormula("COUNTIF(H" + excelRowNum + ":" + lastColLetter + excelRowNum + ",\"Asistió\")");

                Cell cellAbs = row.createCell(3);
                cellAbs.setCellFormula("COUNTIF(H" + excelRowNum + ":" + lastColLetter + excelRowNum + ",\"No asistió\")");

                Cell cellExc = row.createCell(4);
                cellExc.setCellFormula("COUNTIF(H" + excelRowNum + ":" + lastColLetter + excelRowNum + ",\"Justificado\")");

                Cell cellUnev = row.createCell(5);
                cellUnev.setCellFormula("COUNTIF(H" + excelRowNum + ":" + lastColLetter + excelRowNum + ",\"Sin evaluar\")");

                Cell cellPct = row.createCell(6);
                cellPct.setCellFormula("IFERROR((C" + excelRowNum + "+E" + excelRowNum + ")/(C" + excelRowNum + "+D" + excelRowNum + "+E" + excelRowNum + "),\"\")");
                cellPct.setCellStyle(percentStyle);

                // Date Columns values
                for (int i = 0; i < sortedClassDates.size(); i++) {
                    String classDate = sortedClassDates.get(i);
                    Cell dateCell = row.createCell(7 + i);

                    AttendanceRecord ar = existingAttendanceMap.get(info.student.id + "_" + classDate);
                    String stateLabel = AttendanceState.getLabelForValue(ar != null ? ar.status : 0);
                    dateCell.setCellValue(stateLabel);
                }

                attRowIndex++;
            }
        }

        if (attRowIndex > 1 && !sortedClassDates.isEmpty()) {
            DataValidationHelper validationHelper = attendanceSheet.getDataValidationHelper();
            DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(
                    new String[]{"Sin evaluar", "Asistió", "No asistió", "Justificado"}
            );
            CellRangeAddressList addressList = new CellRangeAddressList(1, attRowIndex - 1, 7, 7 + sortedClassDates.size() - 1);
            DataValidation validation = validationHelper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.createErrorBox("Estado Inválido", "Seleccione un estado válido de la lista desplegable.");
            attendanceSheet.addValidationData(validation);

            // Conditional Formatting for Attendance States
            SheetConditionalFormatting conditionalFormatting = attendanceSheet.getSheetConditionalFormatting();

            ConditionalFormattingRule ruleAsistio = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Asistió\"");
            PatternFormatting fillAsistio = ruleAsistio.createPatternFormatting();
            fillAsistio.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            fillAsistio.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleNoAsistio = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"No asistió\"");
            PatternFormatting fillNoAsistio = ruleNoAsistio.createPatternFormatting();
            fillNoAsistio.setFillBackgroundColor(IndexedColors.ROSE.getIndex());
            fillNoAsistio.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleJustificado = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Justificado\"");
            PatternFormatting fillJustificado = ruleJustificado.createPatternFormatting();
            fillJustificado.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            fillJustificado.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            ConditionalFormattingRule ruleSinEvaluar = conditionalFormatting.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Sin evaluar\"");
            PatternFormatting fillSinEvaluar = ruleSinEvaluar.createPatternFormatting();
            fillSinEvaluar.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            fillSinEvaluar.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            CellRangeAddress[] regions = { new CellRangeAddress(1, attRowIndex - 1, 7, 7 + sortedClassDates.size() - 1) };
            conditionalFormatting.addConditionalFormatting(regions, ruleAsistio);
            conditionalFormatting.addConditionalFormatting(regions, ruleNoAsistio);
            conditionalFormatting.addConditionalFormatting(regions, ruleJustificado);
            conditionalFormatting.addConditionalFormatting(regions, ruleSinEvaluar);

            attendanceSheet.setAutoFilter(new CellRangeAddress(0, attRowIndex - 1, 0, 6 + sortedClassDates.size()));
        }

        attendanceSheet.createFreezePane(7, 1);

        attendanceSheet.setColumnWidth(0, 16 * 256);
        attendanceSheet.setColumnWidth(1, 32 * 256);
        attendanceSheet.setColumnWidth(2, 12 * 256);
        attendanceSheet.setColumnWidth(3, 12 * 256);
        attendanceSheet.setColumnWidth(4, 12 * 256);
        attendanceSheet.setColumnWidth(5, 12 * 256);
        attendanceSheet.setColumnWidth(6, 14 * 256);
        for (int i = 0; i < sortedClassDates.size(); i++) {
            attendanceSheet.setColumnWidth(7 + i, 14 * 256);
        }

        // 4. Sheet "Justificaciones"
        Sheet justSheet = workbook.createSheet("Justificaciones");
        Row justHeaderRow = justSheet.createRow(0);
        String[] justHeaders = {"Carnet", "Nombre", "Fecha", "Motivo / Documento de Justificación"};
        for (int i = 0; i < justHeaders.length; i++) {
            Cell cell = justHeaderRow.createCell(i);
            cell.setCellValue(justHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int justRowIndex = 1;

        // Populate existing justifications
        for (AttendanceRecord ar : existingAttendanceMap.values()) {
            if (ar.status == 3 || (ar.justification != null && !ar.justification.trim().isEmpty())) {
                Row row = justSheet.createRow(justRowIndex);
                StudentGradeInfo matchingInfo = null;
                if (gradeInfos != null) {
                    for (StudentGradeInfo info : gradeInfos) {
                        if (info != null && info.student != null && info.student.id == ar.studentId) {
                            matchingInfo = info;
                            break;
                        }
                    }
                }

                row.createCell(0).setCellValue(matchingInfo != null && matchingInfo.student.carnet != null ? matchingInfo.student.carnet : "");
                row.createCell(1).setCellValue(matchingInfo != null && matchingInfo.student.name != null ? matchingInfo.student.name : "");
                row.createCell(2).setCellValue(ar.classDate != null ? ar.classDate : "");
                row.createCell(3).setCellValue(ar.justification != null ? ar.justification : "");

                justRowIndex++;
            }
        }

        justSheet.createFreezePane(0, 1);
        justSheet.setColumnWidth(0, 16 * 256);
        justSheet.setColumnWidth(1, 32 * 256);
        justSheet.setColumnWidth(2, 16 * 256);
        justSheet.setColumnWidth(3, 60 * 256);

        // 5. Sheet "Rubrica"
        Sheet rubricaSheet = workbook.createSheet("Rubrica");

        Row rubHeaderRow = rubricaSheet.createRow(0);
        Cell rCell0 = rubHeaderRow.createCell(0);
        rCell0.setCellValue("Aspecto Evaluado");
        rCell0.setCellStyle(headerStyle);

        Cell rCell1 = rubHeaderRow.createCell(1);
        rCell1.setCellValue("Descripción Detallada de la Conducta Esperada");
        rCell1.setCellStyle(headerStyle);

        String[][] rubricaData = {
                {"Atención y participación", "Escucha, sigue instrucciones, participa y mantiene el foco en tareas curriculares."},
                {"Normas y rutinas", "Respeta turnos, tiempos, silencios, organización de la clase y procedimientos establecidos."},
                {"Autorregulación y esfuerzo", "Persiste ante dificultades, controla impulsos, espera turnos y maneja la frustración en ejercicios y evaluaciones."},
                {"Respeto y colaboración", "Trabaja en equipo, escucha, comparte materiales, respeta opiniones y resuelve conflictos adecuadamente."},
                {"Responsabilidad", "Cuida sus útiles, cumple tareas, trae lo necesario, entrega a tiempo y sigue indicaciones en evaluaciones."}
        };

        for (int r = 0; r < rubricaData.length; r++) {
            Row rRow = rubricaSheet.createRow(r + 1);
            Cell c0 = rRow.createCell(0);
            c0.setCellValue(rubricaData[r][0]);
            c0.setCellStyle(headerStyle);

            Cell c1 = rRow.createCell(1);
            c1.setCellValue(rubricaData[r][1]);
        }

        Row levelsHeaderRow = rubricaSheet.createRow(7);
        Cell lCell0 = levelsHeaderRow.createCell(0);
        lCell0.setCellValue("Nivel de Conducta");
        lCell0.setCellStyle(headerStyle);

        Cell lCell1 = levelsHeaderRow.createCell(1);
        lCell1.setCellValue("Valor Numérico en Sistema (BD)");
        lCell1.setCellStyle(headerStyle);

        String[][] levelsData = {
                {"Excelente", "5"},
                {"Muy bueno", "4"},
                {"Bueno", "3"},
                {"Regular", "2"},
                {"Malo", "1"}
        };

        for (int l = 0; l < levelsData.length; l++) {
            Row lRow = rubricaSheet.createRow(8 + l);
            lRow.createCell(0).setCellValue(levelsData[l][0]);
            lRow.createCell(1).setCellValue(levelsData[l][1]);
        }

        rubricaSheet.setColumnWidth(0, 32 * 256);
        rubricaSheet.setColumnWidth(1, 85 * 256);

        // 6. Hidden Meta Sheet
        Sheet metaSheet = workbook.createSheet(META_SHEET_NAME);
        Row metaRow0 = metaSheet.createRow(0);
        metaRow0.createCell(0).setCellValue(subject != null && subject.code != null ? subject.code : "");

        Row metaRow1 = metaSheet.createRow(1);
        metaRow1.createCell(0).setCellValue(subject != null && subject.section != null ? subject.section : "01");

        Row metaRow2 = metaSheet.createRow(2);
        metaRow2.createCell(0).setCellValue(TEMPLATE_VERSION);

        int metaIndex = workbook.getSheetIndex(META_SHEET_NAME);
        if (metaIndex != -1) {
            workbook.setSheetHidden(metaIndex, true);
        }

        // Write output
        if (outputStream != null) {
            workbook.write(outputStream);
            outputStream.flush();
        }

        workbook.close();
    }

    private static String getExcelColumnName(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        while (columnIndex >= 0) {
            sb.insert(0, (char) ('A' + (columnIndex % 26)));
            columnIndex = (columnIndex / 26) - 1;
        }
        return sb.toString();
    }

    private static Set<Integer> parseScheduleDays(String daysStr) {
        Set<Integer> daysSet = new HashSet<>();
        if (daysStr == null) return daysSet;
        String clean = java.text.Normalizer.normalize(daysStr, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (clean.contains("lunes")) daysSet.add(Calendar.MONDAY);
        if (clean.contains("martes")) daysSet.add(Calendar.TUESDAY);
        if (clean.contains("miercoles")) daysSet.add(Calendar.WEDNESDAY);
        if (clean.contains("jueves")) daysSet.add(Calendar.THURSDAY);
        if (clean.contains("viernes")) daysSet.add(Calendar.FRIDAY);
        if (clean.contains("sabado")) daysSet.add(Calendar.SATURDAY);
        if (clean.contains("domingo")) daysSet.add(Calendar.SUNDAY);
        return daysSet;
    }
}
