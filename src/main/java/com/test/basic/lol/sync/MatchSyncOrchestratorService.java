package com.test.basic.lol.sync;

import com.test.basic.lol.batch.service.BatchJobService;
import com.test.basic.lol.domain.league.LeagueService;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchCacheService;
import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.domain.match.PlaceholderReconciler;
import com.test.basic.lol.domain.match.SyncMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchSyncOrchestratorService {

    public static final String GLOBAL_MATCH_SYNC_LOCK_KEY = "sync:matches:global";

    private final RedissonClient redissonClient;
    private final LeagueService leagueService;
    private final SyncMatchService syncMatchService;
    private final MatchCacheService matchCacheService;
    private final BatchJobService batchJobService;
    private final MatchService matchService;
    private final PlaceholderReconciler placeholderReconciler;

    @FunctionalInterface
    private interface SyncTask {
        void run() throws Exception;
    }

    public SyncExecutionResult runManualLeagueSync(String year) {
        return runWithGlobalLock("manual-league-sync", () -> {
            List<String> leagueIds = leagueService.getAllLeagues()
                    .stream()
                    .map(leagueDto -> leagueDto.getLeagueId())
                    .toList();

            syncMatchService.syncMatchesByLeagueIdsAndYear(leagueIds, year);
            reconcilePlaceholders();
            matchCacheService.invalidateAllCaches();
        });
    }

    public SyncExecutionResult runBatchYearSync(String year) {
        return runWithGlobalLock("batch-year-sync", () -> {
            batchJobService.executeMatchSyncJob(year);
            // 배치는 비동기로 뜨므로 이 시점의 정리는 직전 실행 결과를 대상으로 한다.
            // 매 동기화마다 다시 계산하는 구조라 한 번 늦어도 다음 실행에서 반영된다.
            if (reconcilePlaceholders() > 0) {
                matchCacheService.invalidateAllCaches();
            }
        });
    }

    /**
     * 대진 확정으로 밀려난 대진표 자리를 정리한다.
     * 실패해도 동기화 자체를 실패로 만들지 않는다 — 표시를 다듬는 후처리일 뿐이다.
     *
     * @return 실제로 상태가 바뀐 건수
     */
    private int reconcilePlaceholders() {
        try {
            return placeholderReconciler.reconcile();
        } catch (Exception e) {
            log.error(">>> 플레이스홀더 정리 실패(동기화는 계속 진행): {}", e.getMessage(), e);
            return 0;
        }
    }

    public SyncExecutionResult runTodaySync(LocalDate today) {
        return runWithGlobalLock("today-sync", () -> {
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
            List<Match> todayMatches = matchService.getMatchesByDate(startOfDay, startOfNextDay);

            if (todayMatches.isEmpty()) {
                log.info(">>> 금일 예정된 경기가 없어 동기화를 건너뜁니다.");
                return;
            }

            syncMatchService.syncTodaysMatchesFromLolEsportsApi(todayMatches);
            // matchCacheService.invalidateAllCaches(); // 과도한 캐시 무효화로 인한 부하 방지를 위해 주석 처리
        });
    }

    private SyncExecutionResult runWithGlobalLock(String taskName, SyncTask task) {
        RLock lock = redissonClient.getLock(GLOBAL_MATCH_SYNC_LOCK_KEY);
        boolean locked = false;
        StopWatch sw = new StopWatch();

        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                long ttl = lock.remainTimeToLive();
                String message = "다른 동기화 작업이 실행 중입니다. lockTTL=" + ttl + "ms";
                log.warn(">>> [{}] {}", taskName, message);
                return new SyncExecutionResult(false, false, message, null);
            }

            sw.start();
            task.run();
            sw.stop();

            String message = "동기화 완료";
            log.info(">>> [{}] {} ({}ms)", taskName, message, sw.getTotalTimeMillis());
            return new SyncExecutionResult(true, true, message, sw.getTotalTimeMillis());
        } catch (Exception e) {
            if (sw.isRunning()) {
                sw.stop();
            }
            String message = "동기화 실패: " + e.getMessage();
            log.error(">>> [{}] {}", taskName, message, e);
            return new SyncExecutionResult(false, true, message, sw.getTotalTimeMillis());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info(">>> [{}] 글로벌 락 해제 완료", taskName);
            }
        }
    }
}
