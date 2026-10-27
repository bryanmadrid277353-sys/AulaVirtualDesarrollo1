package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class Subject {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String code; // e.g. "ASIGN12345"
    public String name;
    public String description;
    public Integer professorId;
    public String section; // e.g. "01"
    public String color; // hex or name, default "BLUE"

    public Subject() {}

    public Subject(String name, String description, Integer professorId) {
        this.name = name;
        this.description = description;
        this.professorId = professorId;
        this.section = "01";
        this.color = "BLUE";
    }

    public Subject(String code, String name, String description, Integer professorId) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.professorId = professorId;
        this.section = "01";
        this.color = "BLUE";
    }
}
