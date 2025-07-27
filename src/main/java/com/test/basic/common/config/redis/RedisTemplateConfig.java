package com.test.basic.common.config.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.test.basic.chat.RedisMessageListener;
import com.test.basic.lol.domain.match.MatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RedisTemplateConfig {

    private final StringRedisTemplate redisTemplate;    // 문자열(String) 데이터만 저장
    private final RedisMessageListener redisMessageListener;


    // [1] 채팅용 ==================================================================
    // [1-1] 채팅 메시지 문자열 저장용 레디스 템플릿 등록
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    // TODO 채널 키값에서 roomId 제거 및 단일 채널에서 roomId로 방구분하기
    // Redis Pub/Sub 메시지를 처리하는 리스너를 등록
    // 채팅, 알림, 실시간 이벤트 같은 "메시지 브로드캐스트"에만 사용
    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());

        // 특정 채널(topic) 구독(Subscribe)
        // chat:room:1 으로 들어온 모든 메시지를 redisMessageListener로 전달
        container.addMessageListener(new MessageListenerAdapter(redisMessageListener),
                topic());

        // Ex) 채팅방마다 다른 채널 사용 -> 리소스 사용량 증가
        // new ChannelTopic("chat:room:1")
        // new ChannelTopic("chat:room:2")
        // new ChannelTopic("chat:room:3")

        return container;
    }

    @Bean
    public ChannelTopic topic() {
        // 채팅방 구분 없이 모든 메시지가 "chat:room:1" 하나의 채널에서 관리됨
        // -> 리소스 절약 (대규모 시스템에 적합)
        return new ChannelTopic("chat:room:1");
    }

    // [2] 롤 경기일정용 ==================================================================
    // [2-1] MatchDto 직렬화용 RedisTemplate 등록
    // - pub/sub를 사용하지 않고 get/set과 같은 단순 데이터 저장/조회만 하면 되므로 리스너 등록 불필요
    @Bean
    public RedisTemplate<String, List<MatchDto>> matchRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, List<MatchDto>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 직렬화 대상 데이터 타입 명시
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, MatchDto.class);
        Jackson2JsonRedisSerializer<List<MatchDto>> serializer = new Jackson2JsonRedisSerializer<>(type);

        template.setValueSerializer(serializer);
        return template;
    }


    // TODO [3] 롤 전적검색용 ==================================================================
    /*@Bean
    public RedisTemplate<String, SummonerDto> summonerRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, SummonerDto> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // RedisTemplate 설정 시 key serializer가 없으면 key가 바이트로 직렬화됨 → �� 바이트 에러 발생
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SummonerDto.class));
        return template;
    }*/


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
