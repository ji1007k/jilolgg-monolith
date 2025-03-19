package com.test.basic.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final StringRedisTemplate redisTemplate;
    private final RedisPublisher redisPublisher;

    public ChatService(StringRedisTemplate redisTemplate, RedisPublisher redisPublisher) {
        this.redisTemplate = redisTemplate;
        this.redisPublisher = redisPublisher;
    }

    public void sendMessage(String roomId, String userId, String message) {
        // 사용자 정보와 함께 메시지 저장 (여기서는 간단히 JSON 포맷으로 저장)
        String messageWithUser = String.format("{\"userId\":\"%s\", \"message\":\"%s\"}", userId, message);

        // 채팅방 메시지 리스트에 메시지 추가
        redisTemplate.opsForList().rightPush("chat:room:" + roomId + ":messages", messageWithUser);

        // Redis Publisher를 통해 채팅방 채널에 메시지 발송
        redisPublisher.publish("chat:room:" + roomId, messageWithUser);

        // 사용자 상태 업데이트
//        redisTemplate.opsForHash().put("chat:room:" + roomId + ":user:" + userId, "status", "active");
    }

}
