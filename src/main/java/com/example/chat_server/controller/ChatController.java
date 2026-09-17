package com.example.chat_server.controller;

import com.example.chat_server.model.ChatMessage;
import com.example.chat_server.repository.MessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Date;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate, MessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
    }

    // ==========================================
    // 1. QUẢN LÝ PHÒNG CHUNG (PUBLIC / BROADCAST)
    // ==========================================

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        return withTrustedSender(chatMessage, principal);
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, Principal principal,
                               SimpMessageHeaderAccessor headerAccessor) {
        withTrustedSender(chatMessage, principal);
        headerAccessor.getSessionAttributes().put("username", principal.getName());
        return chatMessage;
    }

    // ==========================================
    // 2. MÔ HÌNH CHAT 1-1 VÀ CHAT NHÓM
    // ==========================================

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if (isBlank(chatMessage.getRecipient())) {
            return;
        }
        withTrustedSender(chatMessage, principal);
        chatMessage.setId(null);
        chatMessage.setGroupId(null);
        messageRepository.save(chatMessage);

        messagingTemplate.convertAndSendToUser(chatMessage.getRecipient(), "/queue/messages", chatMessage);
    }

    @MessageMapping("/chat.group")
    public void sendGroupMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if (isBlank(chatMessage.getGroupId())) {
            return;
        }
        withTrustedSender(chatMessage, principal);
        chatMessage.setId(null);
        chatMessage.setRecipient(null);
        messageRepository.save(chatMessage);

        messagingTemplate.convertAndSend("/topic/group/" + chatMessage.getGroupId(), chatMessage);
    }

    // ==========================================
    // 3. TRẠNG THÁI ĐANG GÕ (TYPING INDICATOR)
    // ==========================================

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatMessage chatMessage, Principal principal) {
        withTrustedSender(chatMessage, principal);
        chatMessage.setType(ChatMessage.MessageType.TYPING);
        relay(chatMessage);
    }

    // ==========================================
    // 4. WEBRTC SIGNALING (AUDIO / VIDEO CALL)
    // ==========================================

    @MessageMapping("/chat.signal")
    public void handleSignaling(@Payload ChatMessage signalMessage, Principal principal) {
        withTrustedSender(signalMessage, principal);
        relay(signalMessage);
    }

    private void relay(ChatMessage message) {
        if (!isBlank(message.getRecipient())) {
            messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        } else if (!isBlank(message.getGroupId())) {
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroupId(), message);
        }
    }

    // Client-supplied sender/timestamp are untrusted; identity comes from the JWT bound at CONNECT
    private ChatMessage withTrustedSender(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setTimestamp(new Date());
        return message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
