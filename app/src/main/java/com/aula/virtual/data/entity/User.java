package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String carnet;
    public String name;
    public String password;
    public String role;
    public String address;
    public String personal_email;
    public String profile_image;
    public Integer can_change_photo = 1;

    public User() {}

    public User(String carnet, String name, String password, String role) {
        this.carnet = carnet;
        this.name = name;
        this.password = password;
        this.role = role;
    }
}
