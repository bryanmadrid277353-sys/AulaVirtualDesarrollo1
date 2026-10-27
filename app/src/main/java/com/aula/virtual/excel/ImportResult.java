package com.aula.virtual.excel;

public class ImportResult {
    public boolean isSuccess;
    public String subjectCode;
    public String section;
    public String fileHash;
    public String fileName;
    public int rowsOk;
    public int rowsError;
    public String errorDetail;
    public String message;

    public ImportResult() {}

    public ImportResult(boolean isSuccess, String subjectCode, String section, String fileHash, String fileName, int rowsOk, int rowsError, String errorDetail, String message) {
        this.isSuccess = isSuccess;
        this.subjectCode = subjectCode;
        this.section = section;
        this.fileHash = fileHash;
        this.fileName = fileName;
        this.rowsOk = rowsOk;
        this.rowsError = rowsError;
        this.errorDetail = errorDetail;
        this.message = message;
    }
}
