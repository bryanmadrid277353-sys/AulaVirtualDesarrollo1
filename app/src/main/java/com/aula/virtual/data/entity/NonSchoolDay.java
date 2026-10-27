package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "non_school_days",
    indices = {@Index(value = {"day"}, unique = true)}
)
public class NonSchoolDay {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String day;
    public String reason;

    public NonSchoolDay() {}

    public NonSchoolDay(String day, String reason) {
        this.day = day;
        this.reason = reason;
    }
}
