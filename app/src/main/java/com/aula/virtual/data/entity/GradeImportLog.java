package com.aula.virtual.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grade_import_log")
public class GradeImportLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int subjectId;
    @ColumnInfo(name = "sheetName", defaultValue = "Notas")
    public String sheetName = "Notas";
    public String fileName;
    public String fileHash;
    public String importedAt;
    public int rowsOk;
    public int rowsError;
    public String errorDetail;

    public GradeImportLog() {}

    public GradeImportLog(int subjectId, String sheetName, String fileName, String fileHash, String importedAt, int rowsOk, int rowsError, String errorDetail) {
        this.subjectId = subjectId;
        this.sheetName = sheetName != null ? sheetName : "Notas";
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.importedAt = importedAt;
        this.rowsOk = rowsOk;
        this.rowsError = rowsError;
        this.errorDetail = errorDetail;
    }

    public GradeImportLog(int subjectId, String fileName, String fileHash, String importedAt, int rowsOk, int rowsError, String errorDetail) {
        this(subjectId, "Notas", fileName, fileHash, importedAt, rowsOk, rowsError, errorDetail);
    }
}
