package com.test.basic.lol.domain.match;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncMatchService {
    private static Logger logger = LoggerFactory.getLogger(SyncMatchService.class);

    private final MatchSyncWorker matchSyncWorker;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void syncTodaysMatchesFromLolEsportsApi(List<Match> matches) {
        try {
            for (Match match : matches) {
                matchSyncWorker.syncTodaysMatchFromLolEsportsApi(match);
            }
        } finally {
            cleanup();
        }
    }

    @PreDestroy
    public void cleanup() {
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
        try {
            for (String leagueId : leagueIds) {
                matchSyncWorker.syncMatchesByLeagueIdAndYearExternalApi(leagueId, year);
            }
        } catch (Exception e) {
            logger.error("리그 동기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } finally {
            cleanup();
        }
    }
}
