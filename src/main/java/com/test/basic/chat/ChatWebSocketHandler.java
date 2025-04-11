package com.test.basic.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;  // Redis로 메시지를 전송할 서비스

    // 여러 사용자의 WebSocket 연결 관리
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // WebSocket 연결이 성립되면 세션에서 인증 정보 가져오기
        String userId = getUserIdFromSession(session);
        sessions.put(userId, session);  // 사용자 세션을 맵에 저장
        logger.info("✅ WebSocket 연결됨: {}", session.getId());

        // 기존 채팅 불러오기
        // 1️⃣ 채팅방 ID 가져오기
//        String roomId = getRoomIdFromSession(session);

        // 2️⃣ Redis에서 채팅방의 최근 메시지 가져오기
        List<String> messages = chatService.getChatMessages("1");

        // 3️⃣ 새로 입장한 사용자에게만 기존 메시지 전송
        if (messages != null && !messages.isEmpty()) {
            for (String message : messages) {
                // 메시지 전송 (서버 -> 클라)
                session.sendMessage(new TextMessage(message)); // 🔥 해당 세션에만 메시지 전송!
            }
        }
    }

    @Override
    @Description("클라이언트가 보낸 메시지를 받아서 다른 클라이언트에 브로드캐스트")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = getUserIdFromSession(session);
        String username = getUserNameFromSession(session);
        String messageContent = message.getPayload();
        logger.info("📩 메시지 수신: [{}] {}", userId, messageContent);

        // 메시지를 Redis로 전송
        chatService.sendMessage("1", userId, username, new TextMessage(messageContent));
    }

    // WebSocketSession에 저장된 사용자 인증 정보 반환 (JWT 등)
    private String getUserIdFromSession(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    private String getUserNameFromSession(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }

    public void broadcastWithSender(String message) throws IOException {
        logger.info("📩 메시지 브로드캐스트 : [{}] {}", message);
        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.remove(userId);  // 세션 종료 시 제거
        logger.info("❌ WebSocket 연결 종료: [{}] {}", userId, session.getId());
    }
}
