package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String message;
    public String targetType; // "ALL", "ROLE_STUDENTS", "ROLE_PROFESSORS", "ROLE_ADMINS", "FACULTY", "SUBJECT", "FACILITY"
    public String targetValue; // Name/ID of Faculty, Subject or Facility
    public String senderName;
    public String timestamp;
    public String iconType; // "GENERAL", "ACADEMIC", "ALERT", "SYSTEM", "SUBJECT"

    public Notification() {}

    public Notification(String title, String message, String targetType, String targetValue, String senderName, String timestamp) {
        this(title, message, targetType, targetValue, senderName, timestamp, "GENERAL");
    }

    public Notification(String title, String message, String targetType, String targetValue, String senderName, String timestamp, String iconType) {
        this.title = title;
        this.message = message;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.iconType = iconType != null ? iconType : "GENERAL";
    }
}
