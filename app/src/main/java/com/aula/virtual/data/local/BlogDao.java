package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import java.util.List;

@Dao
public interface BlogDao {
    @Query("SELECT * FROM blog_entries WHERE subjectId = :subjectId ORDER BY position ASC")
    List<BlogEntry> getEntriesBySubject(int subjectId);

    @Query("SELECT * FROM blog_entries")
    List<BlogEntry> getAllEntries();

    @Query("SELECT * FROM blog_comments WHERE blogEntryId = :entryId ORDER BY id ASC")
    List<BlogComment> getCommentsByEntry(int entryId);

    @Query("SELECT * FROM blog_comments")
    List<BlogComment> getAllComments();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntry(BlogEntry entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntries(List<BlogEntry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(BlogComment comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComments(List<BlogComment> comments);

    @Query("DELETE FROM blog_entries WHERE id = :id")
    void deleteEntryById(int id);

    @Query("DELETE FROM blog_comments WHERE id = :id")
    void deleteCommentById(int id);

    @Query("DELETE FROM blog_comments WHERE blogEntryId = :entryId")
    void deleteCommentsByEntry(int entryId);

    @Query("DELETE FROM blog_entries WHERE subjectId = :subjectId")
    void deleteEntriesBySubject(int subjectId);

    @Query("DELETE FROM blog_entries")
    void deleteAllEntries();

    @Query("DELETE FROM blog_comments")
    void deleteAllComments();
}
