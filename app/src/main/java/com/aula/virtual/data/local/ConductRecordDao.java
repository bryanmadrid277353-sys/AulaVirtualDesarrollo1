package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.aula.virtual.data.entity.ConductRecord;

import java.util.List;

@Dao
public interface ConductRecordDao {
    @Query("SELECT * FROM conduct_records WHERE studentId = :studentId AND subjectId = :subjectId ORDER BY period ASC")
    List<ConductRecord> getRecordsByStudentAndSubject(int studentId, int subjectId);

    @Query("SELECT * FROM conduct_records WHERE studentId = :studentId AND subjectId = :subjectId AND period = :period LIMIT 1")
    ConductRecord getRecord(int studentId, int subjectId, int period);

    @Query("SELECT * FROM conduct_records WHERE subjectId = :subjectId ORDER BY studentId ASC, period ASC")
    List<ConductRecord> getRecordsBySubject(int subjectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ConductRecord record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConductRecord> records);

    @Update
    void update(ConductRecord record);

    @Query("DELETE FROM conduct_records WHERE subjectId = :subjectId")
    void deleteBySubject(int subjectId);
}
