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
        String userName = getUserNameFromSession(session);  // 세션에서 사용자 이름 추출
        sessions.put(userName, session);  // 사용자 세션을 맵에 저장
        logger.info("✅ WebSocket 연결됨: {}", session.getId());
    }

    @Override
    @Description("메시지를 받아서 모든 클라이언트에 브로드캐스트")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String messageContent = message.getPayload();
        String sender = getUserNameFromSession(session);  // 메시지를 보낸 사람의 이름 추출
        logger.info("📩 메시지 수신: [{}] {}", sender, message.getPayload());

        // FIXME REDIS 거치도록 수정 후 삭제
//        broadcast(messageContent, sender);
        
        // TODO 메시지를 Redis로 전송
        chatService.sendMessage("1", sender, messageContent);
    }

    // FIXME JWT나 인증 정보 사용하도록 수정
    // WebSocket에서 `sender` 정보를 얻는 메서드
    private String getUserNameFromSession(WebSocketSession session) {
//        return (String) session.getAttributes().get("userName");
        return session.getId();
    }

    
    // FIXME JWT에서 사용자 정보 가져와서 비교하도록 수정
    // Redis로부터 받은 메시지를 처리하여 다른 클라이언트에게 브로드캐스트
    public void broadcast(String message, String sender) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (!session.getId().equals(sender) && session.isOpen()) {
                session.sendMessage(new TextMessage(message));  // 자기 자신에게는 메시지를 보내지 않음
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userName = getUserNameFromSession(session);
        sessions.remove(userName);  // 세션 종료 시 제거
//        sessions.remove(session.getId());  // 연결이 끊어지면 세션 제거
        logger.info("❌ WebSocket 연결 종료: [{}] {}", userName, session.getId());
    }
}
