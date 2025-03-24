package com.test.basic.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO 객체 저장 RedisTemplate<String, Object> 사용 - 추후 이미지, 파일 등 저장 위함
@Service
public class ChatService {
    private final StringRedisTemplate redisTemplate;
    private final RedisPublisher redisPublisher;

    public ChatService(StringRedisTemplate redisTemplate, RedisPublisher redisPublisher) {
        this.redisTemplate = redisTemplate;
        this.redisPublisher = redisPublisher;
    }

    public void sendMessage(String roomId, String userId, TextMessage message) {
        Instant instant = Instant.now();
        // 로컬 시간대 (사용자의 시스템 시간대)로 변환
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("a h:mm"); // "오전 1:30", "오후 5:15" 형식
        // 시간 문자열 포맷팅
        String formattedTime = localDateTime.format(formatter);

        // 사용자 정보와 함께 메시지 저장 (여기서는 간단히 JSON 포맷으로 저장)
        String messageWithUser = String.format(
                "{\"userId\":\"%s\", \"message\":\"%s\", \"time\":\"%s\"}"
                , userId
                , message.getPayload()
                , formattedTime
        );

        // 채팅방 메시지 리스트에 메시지 추가
        String chatRoomKey = "chat:room:" + roomId + ":messages";
        redisTemplate.opsForList().rightPush(chatRoomKey, messageWithUser);
        redisTemplate.opsForList().trim(chatRoomKey, -100, -1); // 최근 100개만 유지

        // 메시지 TTL(Time To Live) 설정 (10분 후 자동 삭제)
        // TTL을 설정하지 않으면 데이터가 자동으로 사라지지 않음. -> OOM 발생 가능성
        redisTemplate.expire(chatRoomKey, 10, TimeUnit.MINUTES);

        // Redis Publisher를 통해 채팅방 채널에 메시지 발송
        redisPublisher.publish("chat:room:" + roomId, messageWithUser);

        // 사용자 상태 업데이트 (채팅방에 있는 사용자 상태 저장)
//        String userStatusKey = "chat:room:" + roomId + ":user:" + userId;
//        redisTemplate.opsForHash().put(userStatusKey, "status", "active");
//
//    // 6️⃣ 사용자 상태 TTL 설정 (10분 후 자동 삭제)
//    redisTemplate.expire(userStatusKey, 10, TimeUnit.MINUTES);
    }

    // getChatHistory
    public List<String> getChatMessages(String roomId) {
        // Redis에서 채팅방 전체 메시지 불러오기
        String chatRoomKey = "chat:room:" + roomId + ":messages";
        List<String> messages = redisTemplate.opsForList().range(chatRoomKey, 0, -1);

        // 메시지가 없을 경우 빈 리스트 반환
        return messages != null ? messages : new ArrayList<>();
    }
}

