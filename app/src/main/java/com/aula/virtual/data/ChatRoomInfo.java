package com.aula.virtual.data;

public class ChatRoomInfo {
    public String chatRoomId;
    public int contactId;
    public String contactName;
    public String contactRole;
    public String contactPhoto;
    public String contactSubjectOrGrade;
    public String lastMessage;
    public String lastMessageTimestamp;
    public int unreadCount;
    public int initiatorId;

    public ChatRoomInfo() {}

    public ChatRoomInfo(String chatRoomId, int contactId, String contactName, String contactRole,
                        String contactPhoto, String contactSubjectOrGrade, String lastMessage,
                        String lastMessageTimestamp, int unreadCount, int initiatorId) {
        this.chatRoomId = chatRoomId;
        this.contactId = contactId;
        this.contactName = contactName;
        this.contactRole = contactRole;
        this.contactPhoto = contactPhoto;
        this.contactSubjectOrGrade = contactSubjectOrGrade;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.initiatorId = initiatorId;
    }
}
