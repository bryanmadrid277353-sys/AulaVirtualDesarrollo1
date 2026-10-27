package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "facilities")
public class Facility {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String code;
    public String name;
    public String type; // Aulas, Laboratorio, Deportes, Biblioteca
    public String description;

    public Facility() {}

    public Facility(String code, String name, String type, String description) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
    }

    @Ignore
    public Facility(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }
}
