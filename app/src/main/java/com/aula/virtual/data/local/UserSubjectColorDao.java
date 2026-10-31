package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.UserSubjectColor;

@Dao
public interface UserSubjectColorDao {
    @Query("SELECT * FROM user_subject_colors")
    java.util.List<UserSubjectColor> getAllColors();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserSubjectColor userSubjectColor);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(java.util.List<UserSubjectColor> colors);

    @Query("SELECT * FROM user_subject_colors WHERE userId = :userId AND subjectId = :subjectId ORDER BY id DESC LIMIT 1")
    UserSubjectColor get(int userId, int subjectId);

    @Query("DELETE FROM user_subject_colors")
    void deleteAll();

    @Query("DELETE FROM user_subject_colors WHERE subjectId = :subjectId")
    void deleteBySubjectId(int subjectId);

    @Query("DELETE FROM user_subject_colors WHERE userId = :userId")
    void deleteByUserId(int userId);
}
