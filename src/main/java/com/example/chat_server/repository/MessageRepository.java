package com.example.chat_server.repository;

import com.example.chat_server.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<ChatMessage, String> {
    // Lấy lịch sử chat 1-1 giữa 2 user
    List<ChatMessage> findBySenderAndRecipientOrRecipientAndSender(String s1, String r1, String s2, String r2);

    // Lấy lịch sử chat nhóm
    List<ChatMessage> findByGroupId(String groupId);
}