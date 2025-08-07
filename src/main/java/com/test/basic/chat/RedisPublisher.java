package com.test.basic.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 채널에 메시지 발행 - Redis Pub/Sub 계층
 */
@Service
public class RedisPublisher {

    private final StringRedisTemplate redisTemplate;

    public RedisPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String messageWithUser) {
        redisTemplate.convertAndSend(channel, messageWithUser);
    }
}
