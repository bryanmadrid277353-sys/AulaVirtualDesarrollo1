package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.ModificationLog;
import java.util.List;

@Dao
public interface ModificationLogDao {
    @Query("SELECT * FROM modification_logs WHERE entityKey = :key LIMIT 1")
    ModificationLog getLog(String key);

    @Query("SELECT * FROM modification_logs")
    List<ModificationLog> getAllLogsList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLog(ModificationLog log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ModificationLog> logs);
}