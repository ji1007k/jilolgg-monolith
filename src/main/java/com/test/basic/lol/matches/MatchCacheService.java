package com.test.basic.lol.matches;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

}
