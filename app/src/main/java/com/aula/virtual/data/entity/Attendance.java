package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attendances")
public class Attendance {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int studentId;
    public int subjectId;
    public String date;
    public String status;

    public Attendance() {}

    public Attendance(int studentId, int subjectId, String date, String status) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.date = date;
        this.status = status != null ? status : "PRESENT";
    }
}
