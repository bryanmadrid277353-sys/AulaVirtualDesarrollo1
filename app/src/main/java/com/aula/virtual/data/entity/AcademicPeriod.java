package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "academic_periods")
public class AcademicPeriod {
    @PrimaryKey
    public int period;
    public String startDate;
    public String endDate;

    public AcademicPeriod() {}

    public AcademicPeriod(int period, String startDate, String endDate) {
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
