package com.test.basic.lol.domain.match;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchCacheService {
    private static final Logger logger = LoggerFactory.getLogger(MatchCacheService.class);

    private final RedisTemplate<String, List<MatchDto>> matchRedisTemplate;
    
    private static final Duration TTL = Duration.ofMinutes(10);
    
    private List<MatchDto> cachedMatches = null;
    private Instant lastFetchedTime = null;



    // Redis 캐시 ========================
    public List<MatchDto> getCachedMatchList(String key) {
        return matchRedisTemplate.opsForValue().get(key);
    }

    // 캐싱 (연도별 리그 데이터 조회에서 사용 후 조회 소요 시간 1~2초 이상 -> 100ms 미만)
    // key: "match:leagueId:startDate:endDate" 로 저장
    public void cacheMatchList(String key, List<MatchDto> matches) {
        matchRedisTemplate.opsForValue().set(key, matches, TTL);
        logger.info(">>> Cached matches for key: {}", key);
    }


    // 메모리 캐시 ========================
    public List<MatchDto> getMemoryCachedMatches() {
        if (cachedMatches != null && lastFetchedTime != null &&
                Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            logger.info(">>> 메모리 캐시 Hit");
            return cachedMatches;
        }
        logger.info(">>> 메모리 캐시 Miss");
        return null;
    }

    public void setMemoryCachedMatches(List<MatchDto> matches) {
        cachedMatches = matches;
        lastFetchedTime = Instant.now();
        logger.info(">>> 메모리 캐시 저장 완료");
    }

    /**
     * 경기 일정 캐시 무효화
     */
    public void invalidateAllCaches() {
        // 1. Redis 캐시 삭제
        Set<String> keys = matchRedisTemplate.keys("match:*");
        if (keys != null && !keys.isEmpty()) {
            matchRedisTemplate.delete(keys);
            logger.info(">>> Redis 경기 일정 캐시 삭제 완료: {} 개 키", keys.size());
        }

        // 2. 메모리 캐시 초기화
        cachedMatches = null;
        lastFetchedTime = null;

        logger.info(">>> 경기 일정 캐시 무효화 완료");
    }

}
