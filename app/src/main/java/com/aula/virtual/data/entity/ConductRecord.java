package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "conduct_records",
    foreignKeys = {
        @ForeignKey(entity = Student.class, parentColumns = "id", childColumns = "studentId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Subject.class, parentColumns = "id", childColumns = "subjectId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = {"studentId", "subjectId", "period"}, unique = true)
    }
)
public class ConductRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int studentId;
    public int subjectId;
    public int period;

    public Integer attention_participation;
    public Integer rules_routines;
    public Integer self_regulation_effort;
    public Integer respect_collaboration;
    public Integer responsibility;

    public String observations;
    public String updatedAt;
    public Double average;

    public ConductRecord() {}

    public Double calculateAverage() {
        int count = 0;
        double sum = 0;
        if (attention_participation != null) { sum += attention_participation; count++; }
        if (rules_routines != null) { sum += rules_routines; count++; }
        if (self_regulation_effort != null) { sum += self_regulation_effort; count++; }
        if (respect_collaboration != null) { sum += respect_collaboration; count++; }
        if (responsibility != null) { sum += responsibility; count++; }

        if (count == 0) return null;
        return sum / count;
    }

    public void updateComputedAverage() {
        this.average = calculateAverage();
    }
}
