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

    private final StringRedisTemplate redisTemplate;    // 문자열(String) 데이터만 저장
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

        // 특정 채널 구독(Subscribe)
        // chat:room:1 으로 들어온 모든 메시지를 redisMessageListener로 전달
        // 채팅방마다 다른 채널 사용 -> 리소스 사용량 증가
        container.addMessageListener(new MessageListenerAdapter(redisMessageListener),
                new ChannelTopic("chat:room:1"));
        return container;
    }


   /* @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // RedisTemplate<String, Object>을 사용해 객체(Object) 저장 가능
        // 다양한 데이터 유형(예: 사용자 정보, 이미지, 설정 등) 관리
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public ChannelTopic topic() {
        // 채팅방 구분 없이 모든 메시지가 "websocket-channel" 하나의 채널에서 관리됨
        // -> 리소스 절약 (대규모 시스템에 적합)
        return new ChannelTopic("websocket-channel");
    }*/

}
