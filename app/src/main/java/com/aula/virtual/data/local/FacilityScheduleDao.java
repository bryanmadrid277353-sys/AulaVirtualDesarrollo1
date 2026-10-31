package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.FacilitySchedule;
import java.util.List;

@Dao
public interface FacilityScheduleDao {
    @Query("SELECT * FROM facility_schedules")
    List<FacilitySchedule> getAllSchedules();

    @Query("SELECT * FROM facility_schedules WHERE facilityId = :facId")
    List<FacilitySchedule> getByFacilityId(int facId);

    @Query("SELECT * FROM facility_schedules WHERE subjectId = :subId")
    List<FacilitySchedule> getBySubjectId(int subId);

    @Query("SELECT * FROM facility_schedules WHERE professorId = :profId")
    List<FacilitySchedule> getByProfessorId(int profId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FacilitySchedule schedule);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FacilitySchedule> schedules);

    @Query("DELETE FROM facility_schedules WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM facility_schedules WHERE facilityId = :facId")
    void deleteByFacilityId(int facId);

    @Query("DELETE FROM facility_schedules WHERE subjectId = :subjectId")
    void deleteBySubjectId(int subjectId);

    @Query("DELETE FROM facility_schedules WHERE professorId = :profId")
    void deleteByProfessorId(int profId);

    @Query("DELETE FROM facility_schedules")
    void deleteAll();
}
