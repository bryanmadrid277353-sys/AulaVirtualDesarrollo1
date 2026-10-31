package com.aula.virtual.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "modification_logs")
public class ModificationLog {
    @PrimaryKey
    @NonNull
    public String entityKey;
    public String logText;
    public String updatedAt;
    public String updatedBy;

    public ModificationLog() {
        this.entityKey = "";
    }

    public ModificationLog(@NonNull String entityKey, String logText, String updatedAt, String updatedBy) {
        this.entityKey = entityKey;
        this.logText = logText;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}