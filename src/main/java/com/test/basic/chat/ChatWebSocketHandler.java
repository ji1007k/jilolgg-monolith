package com.test.basic.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 클라이언트와 실시간 통신 - 웹소켓 통신 계층
 * - 웹소켓 연결/해제 관리
 * - WebScoketSession 관리
 * - 브로드캐스팅 (모든 세션에 메시지 전송)
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;  // Redis로 메시지를 전송할 서비스
    private final ObjectMapper om;

    // 여러 사용자의 WebSocket 연결 관리
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper om) {
        this.chatService = chatService;
        this.om = om;
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
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.remove(userId);  // 세션 종료 시 제거
        logger.info("❌ WebSocket 연결 종료: [{}] {}", userId, session.getId());
    }

    @Override
    @Description("클라이언트가 보낸 메시지를 받아서 다른 클라이언트에 브로드캐스트")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        String userId = getUserIdFromSession(session);
        String username = getUserNameFromSession(session);
        String payload = message.getPayload();

        String messageType = om.readTree(payload).get("type").asText();

        // ping 무시
        if (messageType.equals("ping")) return;

        String messageContent = om.readTree(payload).get("message").asText();
        logger.info("📩 메시지 수신: [{}] {}", userId, messageContent);

        // 메시지를 Redis로 전송 (데이터 영속성 + 확장성)
        // +) 단일 인스턴스 환경인 경우, 바로 this.broadcastWithSender 호출 가능.
        chatService.sendMessage("1", userId, username, new TextMessage(messageContent));
    }

    public void broadcastWithSender(String message) throws IOException {
        logger.info("📩 메시지 브로드캐스트 : [{}] {}", message);
        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    // WebSocketSession에 저장된 사용자 인증 정보 반환 (JWT 등)
    private String getUserIdFromSession(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    private String getUserNameFromSession(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }
}

/**
 * 채팅 메시지 플로우
 *
 * 1. 클라이언트가 WebSocket으로 채팅 전송
 * 2. ChatWebSocketHandler.handleTextMessage() 메시지 수신
 * 3. ChatService.sendMessage() 비즈니스 로직 처리
 * 4. RedisPublisher.publish() Redis 채널에 발행
 * 5. Redis Pub/Sub 채널 전달
 * 6. RedisSubscriber.onMessage() 구독해서 메시지 수신
 * 7. ChatHandler.broadcastWithSender() 호출
 * 8. 모든 WebSocket 세션들이 메시지 수신
 */
