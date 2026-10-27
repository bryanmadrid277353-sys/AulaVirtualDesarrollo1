package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.aula.virtual.data.entity.Student;
import java.util.List;

@Dao
public interface StudentDao {
    @Query("SELECT * FROM students")
    List<Student> getAllStudents();

    @Query("SELECT * FROM students WHERE encargadoId = :encargadoId")
    List<Student> getStudentsByEncargado(int encargadoId);

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    Student getStudentById(int id);

    @Query("SELECT * FROM students WHERE carnet = :carnet LIMIT 1")
    Student getStudentByCarnet(String carnet);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Student student);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Student> students);

    @Update
    void update(Student student);

    @Query("DELETE FROM students WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM students WHERE encargadoId = :encargadoId")
    void deleteByEncargadoId(int encargadoId);

    @Query("DELETE FROM students")
    void deleteAll();
}
