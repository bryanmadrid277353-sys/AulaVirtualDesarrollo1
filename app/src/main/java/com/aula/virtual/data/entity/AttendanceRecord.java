package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "attendance_records",
    foreignKeys = {
        @ForeignKey(entity = Student.class, parentColumns = "id", childColumns = "studentId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Subject.class, parentColumns = "id", childColumns = "subjectId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = {"studentId", "subjectId", "classDate"}, unique = true),
        @Index(value = {"subjectId", "classDate"})
    }
)
public class AttendanceRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int studentId;
    public int subjectId;
    public String classDate;
    public int status; // 0: Sin evaluar, 1: Asistió, 2: No asistió, 3: Justificado
    public String justification;
    public String updatedAt;

    public AttendanceRecord() {}
}
