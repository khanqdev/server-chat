package com.example.chat_server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "messages")
public class ChatMessage {
    @Id
    private String id;
    private String sender;      // Người gửi
    private String recipient;   // Người nhận (Dùng cho chat 1-1, để trống nếu là chat nhóm)
    private String groupId;     // ID nhóm (Dùng cho chat nhóm, để trống nếu là chat 1-1)
    private String content;     // Nội dung tin nhắn
    private MessageType type;   // CHAT, JOIN, LEAVE, CALL_SIGNAL...
    private Date timestamp = new Date();

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING,
        CALL_OFFER,     // Lời mời gọi (Audio/Video offer)
        CALL_ANSWER,    // Phản hồi chấp nhận cuộc gọi
        ICE_CANDIDATE,  // Ứng viên đường truyền mạng (ICE)
        CALL_DECLINE,   // Từ chối cuộc gọi
        CALL_END        // Kết thúc cuộc gọi
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}