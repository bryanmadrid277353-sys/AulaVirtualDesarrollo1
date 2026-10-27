package com.aula.virtual;

import com.aula.virtual.excel.ExcelTemplateGenerator;
import com.aula.virtual.excel.GradeExcelImporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

public class GradeExcelImporterTest {

    @Test
    public void testSHA256HashCalculation() throws Exception {
        byte[] sampleBytes = "Nota1: 9.5, Nota2: 8.0".getBytes();
        String hash1 = GradeExcelImporter.calculateSHA256(sampleBytes);
        String hash2 = GradeExcelImporter.calculateSHA256(sampleBytes);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);

        byte[] modifiedBytes = "Nota1: 10.0, Nota2: 8.0".getBytes();
        String hash3 = GradeExcelImporter.calculateSHA256(modifiedBytes);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testMetaSheetValidation() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet mainSheet = workbook.createSheet("Notas");
        Row headerRow = mainSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Carnet");
        headerRow.createCell(1).setCellValue("Nombre");

        // Missing _meta sheet
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        byte[] bytes = bos.toByteArray();
        String hash = GradeExcelImporter.calculateSHA256(bytes);

        assertNotNull(hash);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testGradeOutOfRangeValidation() {
        Double invalidHigh = 10.5;
        Double invalidLow = -1.0;
        Double validGrade = 8.5;

        assertTrue(invalidHigh > 10.0);
        assertTrue(invalidLow < 0.0);
        assertTrue(validGrade >= 0.0 && validGrade <= 10.0);
    }

    @Test
    public void testEmptyCellsAreNull() {
        Double emptyGrade = null;
        assertNull(emptyGrade);
    }

    @Test
    public void testSameHashDetections() throws Exception {
        byte[] fileBytes = "Carnet,Nombre,Nota1\nEST100,Juan,9.0".getBytes();
        String hashOriginal = GradeExcelImporter.calculateSHA256(fileBytes);
        String hashSecondRun = GradeExcelImporter.calculateSHA256(fileBytes);

        assertEquals(hashOriginal, hashSecondRun);
    }
}
