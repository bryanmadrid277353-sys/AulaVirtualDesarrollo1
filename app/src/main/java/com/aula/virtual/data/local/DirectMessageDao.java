package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.DirectMessage;
import java.util.List;

@Dao
public interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages WHERE senderId = :userId OR receiverId = :userId ORDER BY id DESC")
    List<DirectMessage> getMessagesForUser(int userId);

    @Query("SELECT * FROM direct_messages WHERE (senderId = :userId AND receiverId = :contactId) OR (senderId = :contactId AND receiverId = :userId) ORDER BY id ASC")
    List<DirectMessage> getHistory(int userId, int contactId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DirectMessage message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DirectMessage> messages);

    @Query("DELETE FROM direct_messages WHERE chatRoomId = :chatRoomId")
    void deleteChatRoom(String chatRoomId);

    @Query("DELETE FROM direct_messages")
    void deleteAll();
}
