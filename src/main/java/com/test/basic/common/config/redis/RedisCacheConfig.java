package com.test.basic.common.config.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.test.basic.lol.api.esports.dto.StandingsResponse;
import com.test.basic.lol.domain.league.LeagueDto;
import com.test.basic.lol.domain.team.TeamDto;
import com.test.basic.lol.domain.tournament.TournamentDto;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    private final ObjectMapper objectMapper;


    /**
     * Spring Cache 전용 ObjectMapper 설정
     * - DTO 전용으로 사용하여 순환 참조 방지
     * - activateDefaultTyping 제거 > 타입 정보 저장 비활성화로 보안 및 호환성 개선
     */
    public RedisCacheConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 날짜를 ISO 문자열로 저장. 타임스탬프 x
    }

    /**
     * Spring Data Redis 캐시 매니저 (단일 캐시 매니저 사용)
     * 각 캐시별 타입 지정된 직렬화 설정
     */
    @Bean
    @Primary  // Redisson 캐시 매니저보다 우선 사용
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // 순위표 캐시 30분
        configs.put("standings", createCacheConfig(Duration.ofMinutes(30), StandingsResponse.StandingsData.class));
        // 리그, 토너먼트 캐시 3일
        configs.put("leagues", createListCacheConfig(Duration.ofDays(3), LeagueDto.class));
        configs.put("tournaments", createListCacheConfig(Duration.ofDays(3), TournamentDto.class));
        // 팀 캐시 7일
        configs.put("teams", createListCacheConfig(Duration.ofDays(7), TeamDto.class));
        // 스케줄러 동기화 타이밍 계산용 캐시
        configs.put("firstMatchTime", createCacheConfig(Duration.ofMinutes(10), LocalDateTime.class));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(createCacheConfig(Duration.ofMinutes(30), Object.class)) // 기본 30분 TTL
                .withInitialCacheConfigurations(configs)
                .build();
    }

    /**
     * List 컬렉션 타입 캐시 설정 생성
     * @param ttl TTL 시간
     * @param valueType List 요소의 타입 (예: LeagueDto.class)
     */
    private RedisCacheConfiguration createListCacheConfig(Duration ttl, Class<?> valueType) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, valueType);
        return createCacheConfig(ttl, listType);
    }

    /**
     * 단일 클래스 타입 캐시 설정 생성
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl, Class<?> valueType) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                // Redis에 저장할 객체 직렬화 설정
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))   // 캐시 키를 문자열로 저장. 예: "teams::league1_[slug1,slug2]"
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, valueType)));   // 값은 json 형태로 저장
    }

    /**
     * 제네릭 타입 캐시 설정 생성 (List, Map 등)
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl, JavaType valueType) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, valueType)));
    }

}
