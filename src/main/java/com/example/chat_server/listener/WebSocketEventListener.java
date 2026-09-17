package com.example.chat_server.listener;

import com.example.chat_server.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("--> Có một kết nối WebSocket mới vừa thiết lập.");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Lấy lại username đã lưu trong session lúc người dùng đăng nhập/vào phòng
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            System.out.println("--> User ngắt kết nối: " + username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setContent(username + " đã rời mạng.");

            // Broadcast thông báo offline cho tất cả mọi người ở kênh public/room chung
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}