package com.test.basic.common.config.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.test.basic.lol.domain.match.MatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RedisTemplateConfig {

    // [1] 롤 경기일정용 ==================================================================
    // [1-1] MatchDto 직렬화용 RedisTemplate 등록
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
}
