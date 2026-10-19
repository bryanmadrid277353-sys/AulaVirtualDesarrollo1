package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Attendance;
import java.util.List;

@Dao
public interface AttendanceDao {
    @Query("SELECT * FROM attendances WHERE studentId = :studentId")
    List<Attendance> getByStudentId(int studentId);

    @Query("SELECT * FROM attendances WHERE studentId = :studentId AND subjectId = :subjectId")
    List<Attendance> getByStudentAndSubject(int studentId, int subjectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Attendance attendance);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Attendance> attendances);
}
