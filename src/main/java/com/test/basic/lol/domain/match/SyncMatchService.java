package com.test.basic.lol.domain.match;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SyncMatchService {
    private static Logger logger = LoggerFactory.getLogger(SyncMatchService.class);

    private final MatchSyncWorker matchSyncWorker;

    private final RedissonClient redissonClient;
    private RLock lock;
    private String LOCK_KEY = "sync-matches-lock";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void syncTodaysMatchesFromLolEsportsApi(List<Match> matches) {
        // Redisson Lock 획득
        lock = redissonClient.getLock(LOCK_KEY); // lightweight 프록시 객체
        boolean isLocked = false;

        try {
            long startTime = System.currentTimeMillis();
            // 도중에 컨테이너가 죽으면 락이 leaseTime(10분)간 Redis에 계속 남음
            //  -> 다른 인스턴스가 tryLock 시도 시 "다른 애가 락 잡고 있음" -> 오래된 락
//            isLocked = lock.tryLock(1, 600, TimeUnit.SECONDS);  // 1분 대기 및 최대 600초(10분)간 락 유지
            // leaseTime 생략 → watchdog 활성화
            //  -> Redisson이 백그라운드에서 락을 유지하고, 컨테이너/JVM 죽으면 watchdog도 종료 → 락 자동 해제
            isLocked = lock.tryLock(1, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            logger.info(">>> 락 획득 결과: {}, 대기 시간: {}ms", isLocked, (endTime - startTime));

            if (! isLocked) {
                long ttl = lock.remainTimeToLive();
                logger.warn(">>> 락 획득 실패. 현재 락 TTL: {}ms", ttl);
                return;
            }

            logger.info(">>> 락 획득 성공.");
            logger.info(">>> LOL Esports API로부터 경기 정보 동기화 시작");

            for (Match match : matches) {
                matchSyncWorker.syncTodaysMatchFromLolEsportsApi(match);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(">> 락 획득 중 예외 발생: {}", e.getMessage());
        } finally {
            cleanup();
        }
    }

    @PreDestroy
    public void cleanup() {
        // 현재 스레드가 잡은 락인지 보장하기 위해 isHeldByCurrentThread() 꼭 확인
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                logger.info(">>> 락 해제 완료");
            } catch (IllegalMonitorStateException e) {
                logger.warn(">>> 락 해제 중 예외 발생 (이미 해제되었거나 다른 쓰레드가 보유): {}", e.getMessage());
            }
        } else {
            logger.warn(">>> 락 해제 시도 없음 (락 획득 실패 또는 타임아웃에 의한 자동 해제)");
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();


        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        logger.info(">>> GC 전 JVM 힙 사용량: {} MB", used);

        try {
            System.gc();
            Thread.sleep(200);  // Full GC 대기
        } catch(InterruptedException ie) {
            logger.error("Thread 정지 중 에러 발생: {}", ie.getMessage());
        }

        used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        logger.info(">>> GC 후 JVM 힙 사용량: {} MB", used);
    }


    // 리그id, 연도별 데이터 동기화(블로킹 방식)
    // 파라미터로 전달된 YEAR 데이터까지만 갱신
    // EX) 2022를 전달한 경우, 금년 2025부터~2024,2023,2022 데이터 갱신
    @Transactional
    public void syncMatchesByLeagueIdsAndYear(List<String> leagueIds, String year) {
        lock = redissonClient.getLock(LOCK_KEY);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!isLocked) {
                long ttl = lock.remainTimeToLive();
                logger.warn(">>> 락 획득 실패. 현재 락 TTL: {}ms", ttl);
                throw new RuntimeException("다른 동기화 작업이 실행 중입니다.");
            }

            for (String leagueId : leagueIds) {
                matchSyncWorker.syncMatchesByLeagueIdAndYearExternalApi(leagueId, year);
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("동기화 락 획득 중 인터럽트 발생", ie);
        } catch (Exception e) {
            logger.error("리그 동기화 실패: {}", e.getMessage(), e);
        } finally {
            cleanup();
        }
    }
}
