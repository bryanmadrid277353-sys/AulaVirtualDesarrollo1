package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.NonSchoolDay;

import java.util.List;

@Dao
public interface AcademicCalendarDao {
    @Query("SELECT * FROM academic_periods ORDER BY period ASC")
    List<AcademicPeriod> getAllPeriods();

    @Query("SELECT * FROM academic_periods ORDER BY period ASC")
    androidx.lifecycle.LiveData<List<AcademicPeriod>> getAllPeriodsLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPeriod(AcademicPeriod period);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPeriods(List<AcademicPeriod> periods);

    @Query("DELETE FROM academic_periods")
    void deleteAllPeriods();

    @Query("SELECT * FROM non_school_days ORDER BY day ASC")
    List<NonSchoolDay> getAllNonSchoolDays();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNonSchoolDay(NonSchoolDay day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllNonSchoolDays(List<NonSchoolDay> days);

    @Query("DELETE FROM non_school_days WHERE id = :id")
    void deleteNonSchoolDay(int id);

    @Query("DELETE FROM non_school_days")
    void deleteAllNonSchoolDays();
}
