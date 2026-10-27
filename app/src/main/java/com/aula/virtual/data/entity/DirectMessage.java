package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "direct_messages")
public class DirectMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String chatRoomId;
    public int senderId;
    public int receiverId;
    public String message;
    public String timestamp;
    public int isRead;
    public int initiatorId;

    public DirectMessage() {}

    public DirectMessage(String chatRoomId, int senderId, int receiverId, String message, String timestamp, int isRead, int initiatorId) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.initiatorId = initiatorId;
    }
}
