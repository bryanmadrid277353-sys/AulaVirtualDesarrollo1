package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.User;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'STUDENT' OR UPPER(TRIM(role)) = 'ENCARGADO'")
    List<User> getStudents();

    @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'ENCARGADO'")
    List<User> getEncargadosOnly();

    @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'ENCARGADO' OR UPPER(TRIM(role)) = 'STUDENT'")
    List<User> getEncargados();

    @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'ADMIN'")
    List<User> getAdmins();

    @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'PROFESSOR'")
    List<User> getProfessors();

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE (LOWER(TRIM(carnet)) = LOWER(TRIM(:carnet)) OR LOWER(TRIM(name)) = LOWER(TRIM(:carnet))) AND (password = :password OR TRIM(password) = TRIM(:password)) LIMIT 1")
    User login(String carnet, String password);

    @Query("SELECT * FROM users WHERE LOWER(TRIM(carnet)) = LOWER(TRIM(:identifier)) OR LOWER(TRIM(name)) = LOWER(TRIM(:identifier)) LIMIT 1")
    User getUserByCarnetOrName(String identifier);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM users WHERE role = :role")
    void deleteByRole(String role);

    @Query("DELETE FROM users")
    void deleteAll();
}
