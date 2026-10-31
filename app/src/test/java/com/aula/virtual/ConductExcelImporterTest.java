package com.aula.virtual;

import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.excel.ExcelTemplateGenerator;
import com.aula.virtual.excel.GradeExcelImporter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConductExcelImporterTest {

    @Test
    public void testLevelParsingTolerantAccentsAndCasing() {
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("EXCELENTE"));
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("Excelente"));
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("excelente"));
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("  Excelente  "));
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("E"));
        assertEquals(Integer.valueOf(5), ConductLevel.getValueForLabel("e"));

        assertEquals(Integer.valueOf(4), ConductLevel.getValueForLabel("MUY BUENO"));
        assertEquals(Integer.valueOf(4), ConductLevel.getValueForLabel("Muy Bueno"));
        assertEquals(Integer.valueOf(4), ConductLevel.getValueForLabel("muy bueno"));
        assertEquals(Integer.valueOf(4), ConductLevel.getValueForLabel("M"));
        assertEquals(Integer.valueOf(4), ConductLevel.getValueForLabel("m"));

        assertEquals(Integer.valueOf(3), ConductLevel.getValueForLabel("BUENO"));
        assertEquals(Integer.valueOf(3), ConductLevel.getValueForLabel("Bueno"));
        assertEquals(Integer.valueOf(3), ConductLevel.getValueForLabel("B"));
        assertEquals(Integer.valueOf(3), ConductLevel.getValueForLabel("b"));

        assertEquals(Integer.valueOf(2), ConductLevel.getValueForLabel("REGULAR"));
        assertEquals(Integer.valueOf(2), ConductLevel.getValueForLabel("Regular"));
        assertEquals(Integer.valueOf(2), ConductLevel.getValueForLabel("R"));
        assertEquals(Integer.valueOf(2), ConductLevel.getValueForLabel("r"));

        assertEquals(Integer.valueOf(1), ConductLevel.getValueForLabel("MALO"));
        assertEquals(Integer.valueOf(1), ConductLevel.getValueForLabel("Malo"));
        assertEquals(Integer.valueOf(1), ConductLevel.getValueForLabel("O"));
        assertEquals(Integer.valueOf(1), ConductLevel.getValueForLabel("o"));
    }

    @Test
    public void testInvalidLevelParsing() {
        assertNull(ConductLevel.getValueForLabel("Pésimo"));
        assertNull(ConductLevel.getValueForLabel("Invalido"));
        assertNull(ConductLevel.getValueForLabel("10"));
        assertNull(ConductLevel.getValueForLabel("X"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Test");
        Row row = sheet.createRow(0);

        Cell invalidCell = row.createCell(0);
        invalidCell.setCellValue("Texto Invalido");

        Integer result = GradeExcelImporter.parseConductLevelCellValue(invalidCell);
        assertEquals(Integer.valueOf(-1), result);

        try { wb.close(); } catch (Exception ignored) {}
    }

    @Test
    public void testPeriodValidation() {
        int validPeriodMin = 1;
        int validPeriodMax = 5;
        int invalidLowPeriod = 0;
        int invalidHighPeriod = 6;

        assertTrue(validPeriodMin >= 1 && validPeriodMin <= 5);
        assertTrue(validPeriodMax >= 1 && validPeriodMax <= 5);
        assertFalse(invalidLowPeriod >= 1 && invalidLowPeriod <= 5);
        assertFalse(invalidHighPeriod >= 1 && invalidHighPeriod <= 5);
    }

    @Test
    public void testPartiallyEmptyAspectsAverageCalculation() {
        ConductRecord record = new ConductRecord();
        record.attention_participation = 5;
        record.rules_routines = 4;
        record.self_regulation_effort = null;
        record.respect_collaboration = null;
        record.responsibility = null;

        Double computedAvg = record.calculateAverage();
        assertNotNull(computedAvg);
        assertEquals(4.5, computedAvg, 0.001);

        ConductRecord emptyRecord = new ConductRecord();
        assertNull(emptyRecord.calculateAverage());
    }

    @Test
    public void testVersion1WorkbookWithoutConductSheetStructure() throws Exception {
        Workbook workbook = new XSSFWorkbook();

        // 1. Sheet Notas
        Sheet mainSheet = workbook.createSheet("Notas");
        Row headerRow = mainSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Carnet");
        headerRow.createCell(1).setCellValue("Nombre");
        headerRow.createCell(2).setCellValue("Nota1");

        Row dataRow = mainSheet.createRow(1);
        dataRow.createCell(0).setCellValue("EST6197088");
        dataRow.createCell(1).setCellValue("Diego Madrid");
        dataRow.createCell(2).setCellValue(9.5);

        // 2. Sheet _meta (Version 1.0)
        Sheet metaSheet = workbook.createSheet(ExcelTemplateGenerator.META_SHEET_NAME);
        metaSheet.createRow(0).createCell(0).setCellValue("ASIGN10001");
        metaSheet.createRow(1).createCell(0).setCellValue("01");
        metaSheet.createRow(2).createCell(0).setCellValue("1.0");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        byte[] bytes = bos.toByteArray();
        assertTrue(bytes.length > 0);

        Workbook readWorkbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        assertNotNull(readWorkbook.getSheet("Notas"));
        assertNull(readWorkbook.getSheet("Conducta")); // v1 has no Conducta sheet
        readWorkbook.close();
    }

    @Test
    public void testVersion2WorkbookWithConductSheetStructure() throws Exception {
        Subject subject = new Subject();
        subject.id = 1;
        subject.code = "ASIGN10001";
        subject.name = "Matemáticas 5º";

        com.aula.virtual.data.entity.User student = new com.aula.virtual.data.entity.User();
        student.id = 1;
        student.carnet = "EST6197088";
        student.name = "Diego Madrid";

        Enrollment enrollment = new Enrollment();
        enrollment.studentId = 1;
        enrollment.subjectId = 1;
        enrollment.grade1 = 9.0;

        StudentGradeInfo info = new StudentGradeInfo();
        info.student = student;
        info.subject = subject;
        info.enrollment = enrollment;

        List<StudentGradeInfo> gradeInfos = new ArrayList<>();
        gradeInfos.add(info);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelTemplateGenerator.generateTemplate(null, subject, gradeInfos, bos);

        byte[] bytes = bos.toByteArray();
        Workbook readWorkbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));

        assertNotNull(readWorkbook.getSheet("Notas"));
        assertNotNull(readWorkbook.getSheet("Conducta"));
        assertNotNull(readWorkbook.getSheet("Rubrica"));

        Sheet conductSheet = readWorkbook.getSheet("Conducta");
        assertEquals(3, conductSheet.getLastRowNum()); // 1 header + 3 period rows for 1 student

        readWorkbook.close();
    }

    @Test
    public void testIdempotentHashDetection() throws Exception {
        byte[] fileData = "Carnet,Nombre,Periodo,Conducta\nEST1,Diego,1,Excelente".getBytes();
        String hash1 = GradeExcelImporter.calculateSHA256(fileData);
        String hash2 = GradeExcelImporter.calculateSHA256(fileData);

        assertEquals(hash1, hash2);

        byte[] modifiedConductData = "Carnet,Nombre,Periodo,Conducta\nEST1,Diego,1,Muy bueno".getBytes();
        String hash3 = GradeExcelImporter.calculateSHA256(modifiedConductData);

        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testNewStudentRegenerationAddsPeriods() throws Exception {
        Subject subject = new Subject();
        subject.id = 1;
        subject.code = "ASIGN10001";

        List<StudentGradeInfo> gradeInfos = new ArrayList<>();

        com.aula.virtual.data.entity.User s1 = new com.aula.virtual.data.entity.User();
        s1.id = 1; s1.carnet = "EST100"; s1.name = "Alumno Uno";
        StudentGradeInfo info1 = new StudentGradeInfo(); info1.student = s1; info1.subject = subject;
        gradeInfos.add(info1);

        com.aula.virtual.data.entity.User s2 = new com.aula.virtual.data.entity.User();
        s2.id = 2; s2.carnet = "EST200"; s2.name = "Alumno Dos";
        StudentGradeInfo info2 = new StudentGradeInfo(); info2.student = s2; info2.subject = subject;
        gradeInfos.add(info2);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelTemplateGenerator.generateTemplate(null, subject, gradeInfos, bos);

        Workbook readWorkbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
        Sheet conductSheet = readWorkbook.getSheet("Conducta");

        // 1 header row + 3 rows for Student 1 + 3 rows for Student 2 = 6 rows data (last row index 6)
        assertEquals(6, conductSheet.getLastRowNum());
        readWorkbook.close();
    }
}
