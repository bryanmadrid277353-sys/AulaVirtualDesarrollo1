package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enrollments")
public class Enrollment {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int studentId;
    public int subjectId;
    public Double grade1;
    public Double grade2;
    public Double grade3;
    public Double grade4;
    public Double grade5;
    public Double grade6;
    public Double grade7;
    public Double grade8;
    public Double average;
    public String color = "BLUE";
    public String cycle = "Ciclo 2025";
    public int periodsCount = 5;

    public Enrollment() {}

    public Enrollment(int studentId, int subjectId) {
        this(studentId, subjectId, "Ciclo 2025");
    }

    public Enrollment(int studentId, int subjectId, String cycle) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.cycle = cycle != null ? cycle : "Ciclo 2025";
        this.periodsCount = 5;
    }
}
