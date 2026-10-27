package com.aula.virtual;

import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.AttendanceState;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.excel.ExcelTemplateGenerator;
import com.aula.virtual.excel.GradeExcelImporter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AttendanceExcelImporterTest {

    @Test
    public void testAttendanceStateParsingTolerantAccentsAndCasing() {
        assertEquals(Integer.valueOf(1), AttendanceState.getValueForLabel("ASISTIÓ"));
        assertEquals(Integer.valueOf(1), AttendanceState.getValueForLabel("Asistió"));
        assertEquals(Integer.valueOf(1), AttendanceState.getValueForLabel("asistio"));
        assertEquals(Integer.valueOf(1), AttendanceState.getValueForLabel("A"));
        assertEquals(Integer.valueOf(1), AttendanceState.getValueForLabel("1"));

        assertEquals(Integer.valueOf(2), AttendanceState.getValueForLabel("NO ASISTIÓ"));
        assertEquals(Integer.valueOf(2), AttendanceState.getValueForLabel("No asistió"));
        assertEquals(Integer.valueOf(2), AttendanceState.getValueForLabel("noasistio"));
        assertEquals(Integer.valueOf(2), AttendanceState.getValueForLabel("NA"));
        assertEquals(Integer.valueOf(2), AttendanceState.getValueForLabel("2"));

        assertEquals(Integer.valueOf(3), AttendanceState.getValueForLabel("JUSTIFICADO"));
        assertEquals(Integer.valueOf(3), AttendanceState.getValueForLabel("Justificado"));
        assertEquals(Integer.valueOf(3), AttendanceState.getValueForLabel("J"));
        assertEquals(Integer.valueOf(3), AttendanceState.getValueForLabel("3"));

        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("SIN EVALUAR"));
        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("Sin evaluar"));
        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("SE"));
        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("0"));
    }

    @Test
    public void testInvalidAttendanceStateDefaultsToZero() {
        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("INVÁLIDO"));
        assertEquals(Integer.valueOf(0), AttendanceState.getValueForLabel("Desconocido"));
    }

    @Test
    public void testAttendancePercentageCalculation() {
        int attended = 8;
        int absent = 1;
        int excused = 1;
        int totalEvaluated = attended + absent + excused;

        double percentage = ((double) (attended + excused) / totalEvaluated) * 100.0;
        assertEquals(90.0, percentage, 0.001);
    }

    @Test
    public void testVersion3WorkbookStructureGenerated() throws Exception {
        Subject subject = new Subject();
        subject.id = 1;
        subject.code = "ASIGN10001";
        subject.name = "Matemáticas 5º";

        User student = new User();
        student.id = 1;
        student.carnet = "EST6197088";
        student.name = "Diego Madrid";

        Enrollment enrollment = new Enrollment();
        enrollment.studentId = 1;
        enrollment.subjectId = 1;

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
        assertNotNull(readWorkbook.getSheet("Asistencia"));
        assertNotNull(readWorkbook.getSheet("Justificaciones"));
        assertNotNull(readWorkbook.getSheet("Rubrica"));

        readWorkbook.close();
    }

    @Test
    public void testIdempotentHashCalculationForAttendance() throws Exception {
        byte[] fileData = "Carnet,Nombre,Asistencia\nEST1,Diego,Asistió".getBytes();
        String hash1 = GradeExcelImporter.calculateSHA256(fileData);
        String hash2 = GradeExcelImporter.calculateSHA256(fileData);

        assertEquals(hash1, hash2);

        byte[] modifiedData = "Carnet,Nombre,Asistencia\nEST1,Diego,No asistió".getBytes();
        String hash3 = GradeExcelImporter.calculateSHA256(modifiedData);

        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testNewStudentRegenerationWithAttendance() throws Exception {
        Subject subject = new Subject();
        subject.id = 1;
        subject.code = "ASIGN10001";

        List<StudentGradeInfo> gradeInfos = new ArrayList<>();

        User s1 = new User(); s1.id = 1; s1.carnet = "EST100"; s1.name = "Alumno Uno";
        StudentGradeInfo info1 = new StudentGradeInfo(); info1.student = s1; info1.subject = subject;
        gradeInfos.add(info1);

        User s2 = new User(); s2.id = 2; s2.carnet = "EST200"; s2.name = "Alumno Dos";
        StudentGradeInfo info2 = new StudentGradeInfo(); info2.student = s2; info2.subject = subject;
        gradeInfos.add(info2);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelTemplateGenerator.generateTemplate(null, subject, gradeInfos, bos);

        Workbook readWorkbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
        Sheet attSheet = readWorkbook.getSheet("Asistencia");

        // 1 header row + 2 student rows = 2 student data rows
        assertEquals(2, attSheet.getLastRowNum());
        readWorkbook.close();
    }
}
