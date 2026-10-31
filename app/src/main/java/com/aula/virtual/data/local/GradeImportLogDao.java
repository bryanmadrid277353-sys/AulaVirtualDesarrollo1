package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.GradeImportLog;
import java.util.List;

@Dao
public interface GradeImportLogDao {
    @Query("SELECT * FROM grade_import_log WHERE subjectId = :subjectId ORDER BY id DESC")
    List<GradeImportLog> getLogsForSubject(int subjectId);

    @Query("SELECT * FROM grade_import_log WHERE subjectId = :subjectId ORDER BY id DESC LIMIT 1")
    GradeImportLog getLastLogForSubject(int subjectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GradeImportLog log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GradeImportLog> logs);

    @Query("SELECT * FROM grade_import_log")
    List<GradeImportLog> getAllLogs();

    @Query("DELETE FROM grade_import_log WHERE subjectId = :subjectId")
    void deleteLogsForSubject(int subjectId);

    @Query("DELETE FROM grade_import_log")
    void deleteAll();
}
