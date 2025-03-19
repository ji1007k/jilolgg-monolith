package com.test.basic.common.config;

import com.test.basic.chat.RedisMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListener redisMessageListener;

    public RedisConfig(StringRedisTemplate redisTemplate, RedisMessageListener redisMessageListener) {
        this.redisTemplate = redisTemplate;
        this.redisMessageListener = redisMessageListener;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean
//    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
        container.setConnectionFactory(redisTemplate.getConnectionFactory());

        // 특정 채널에 대한 메시지를 구독
        container.addMessageListener(new MessageListenerAdapter(redisMessageListener),
                new ChannelTopic("chat:room:1"));
        return container;
    }
}
