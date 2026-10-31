package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Enrollment;
import java.util.List;

@Dao
public interface EnrollmentDao {
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    List<Enrollment> getByStudentId(int studentId);

    @Query("SELECT * FROM enrollments")
    List<Enrollment> getAllEnrollments();

    @Query("SELECT * FROM enrollments WHERE subjectId = :subjectId")
    List<Enrollment> getBySubjectId(int subjectId);

    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND subjectId = :subjectId LIMIT 1")
    Enrollment getByStudentAndSubject(int studentId, int subjectId);

    @Query("SELECT * FROM enrollments WHERE id = :id LIMIT 1")
    Enrollment getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Enrollment enrollment);

    @androidx.room.Update
    void update(Enrollment enrollment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Enrollment> enrollments);

    @Query("DELETE FROM enrollments WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM enrollments WHERE studentId = :studentId")
    void deleteByStudentId(int studentId);

    @Query("DELETE FROM enrollments WHERE subjectId = :subjectId")
    void deleteBySubjectId(int subjectId);

    @Query("DELETE FROM enrollments")
    void deleteAll();
}
