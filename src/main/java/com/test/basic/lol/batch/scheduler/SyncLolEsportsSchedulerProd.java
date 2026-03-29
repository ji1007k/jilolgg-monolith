package com.test.basic.lol.batch.scheduler;

import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.sync.MatchSyncOrchestratorService;
import com.test.basic.lol.sync.SyncExecutionResult;
import com.test.basic.lol.domain.team.SyncTeamService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class SyncLolEsportsSchedulerProd {
    private static final Logger logger = LoggerFactory.getLogger(SyncLolEsportsSchedulerProd.class);

    private final SyncTeamService syncTeamService;
    private final MatchService matchService;
    private final MatchSyncOrchestratorService matchSyncOrchestratorService;

    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Seoul")
    public void syncTeamsProd() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        syncTeamService.syncTeamsFromLolEsportsApi();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    private void syncAllMatchesDev() {
        logger.info("==================== [전체 경기 일정 자동 동기화 배치 작업 시작] ====================");
        SyncExecutionResult result = matchSyncOrchestratorService.runBatchYearSync(Year.now(ZoneId.of("Asia/Seoul")).toString());
        if (!result.lockAcquired()) {
            logger.warn(">>> 전체 경기 일정 배치 실행 건너뜀: {}", result.message());
        }
    }


    // 서버 시작 후 5분 뒤 시작 및 10분 간격으로 반복
    @Scheduled(fixedDelay = 1000*60*10, initialDelay = 1000*60*5)
    private void syncTodaysMatchesFromApiDev() {
        logger.info("==================== [금일 경기 정보 자동 동기화 작업 시작] ====================");
        LocalDate today = LocalDate.now();

        // [1] 동기화 타이밍 검사
        if (!isUpdateTime(today)) {
            logger.info(">>> 동기화 작업을 건너뜁니다.");
            logger.info("==================== [금일 경기 정보 자동 동기화 작업 종료] ====================");
            return;
        }

        // [2] 동기화 실행
        SyncExecutionResult result = matchSyncOrchestratorService.runTodaySync(today);
        if (!result.lockAcquired()) {
            logger.warn(">>> 금일 경기 동기화 건너뜀: {}", result.message());
            return;
        }
        logger.info("==================== [금일 경기 정보 자동 동기화 작업 완료] ====================");
    }

    public boolean isUpdateTime(LocalDate date) {
        // 1. 해당 날짜 경기 중 가장 이른 시간 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        LocalDateTime firstMatchTime = matchService.getFirstMatchTimeOfDay(startOfDay, endOfDay);
        if (firstMatchTime == null) return false;

        // 2. 첫 경기 시작시간 -1시간 전부터 갱신 시작
        LocalDateTime updateStartTime = firstMatchTime.minusHours(1);

        // 3. 현재 시각이 그 시간 이후면 true
        return LocalDateTime.now().isAfter(updateStartTime);
    }
}
