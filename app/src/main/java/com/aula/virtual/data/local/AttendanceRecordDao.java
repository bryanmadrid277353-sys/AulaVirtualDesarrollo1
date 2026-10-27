package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.aula.virtual.data.entity.AttendanceRecord;

import java.util.List;

@Dao
public interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND subjectId = :subjectId ORDER BY classDate ASC")
    List<AttendanceRecord> getRecordsByStudentAndSubject(int studentId, int subjectId);

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND subjectId = :subjectId AND classDate = :classDate LIMIT 1")
    AttendanceRecord getRecord(int studentId, int subjectId, String classDate);

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY classDate ASC, studentId ASC")
    List<AttendanceRecord> getRecordsBySubject(int subjectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AttendanceRecord record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AttendanceRecord> records);

    @Update
    void update(AttendanceRecord record);

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    void deleteBySubject(int subjectId);
}
