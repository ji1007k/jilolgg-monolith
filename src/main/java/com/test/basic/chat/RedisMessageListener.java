package com.test.basic.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RedisMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private final ChatWebSocketHandler chatHandler;

    @Autowired
    private ObjectMapper om;

    // WebSocket 핸들러 인젝션
    public RedisMessageListener(ChatWebSocketHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());  // 채널 이름
//        String messageBody = new String(message.getBody());  // 메시지 본문

        // 메시지를 클라이언트로 전달하는 처리
        try {
            // JSON 파싱하여 userId와 message 추출
            JsonNode jsonNode = om.readTree(message.getBody());
            String userId = jsonNode.get("userId").asText();  // userId 추출
            String msg = jsonNode.get("message").asText();    // message 추출

            // 로그로 userId와 메시지 확인
            logger.info("📬 Redis 받은 메시지: room = {}, userId = {}, message = {}", channel, userId, msg);

            // 메시지를 클라이언트로 전달하는 처리
            chatHandler.broadcast(msg, userId);  // 채팅 핸들러로 메시지 전송
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
