package com.test.basic.lol.batch.scheduler;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.domain.match.SyncMatchService;
import com.test.basic.lol.domain.team.SyncTeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Configuration
@Profile("dev")
public class SyncLolEsportsSchedulerDev {

    private static final Logger logger = LoggerFactory.getLogger(SyncLolEsportsSchedulerDev.class);
    private final SyncTeamService syncTeamService;
    private final SyncMatchService syncMatchService;
    private final MatchService matchService;

    public SyncLolEsportsSchedulerDev(SyncTeamService syncTeamService,
                                      SyncMatchService syncMatchService, MatchService matchService) {
        this.syncTeamService = syncTeamService;
        this.syncMatchService = syncMatchService;
        this.matchService = matchService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    private void syncTeamsDev() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        syncTeamService.syncTeamsFromLolEsportsApi();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }


    @Scheduled(cron = "0 0/10 * * * *", zone = "Asia/Seoul")
    private void syncTodaysMatchesFromApiDev() {
        logger.info("==================== [금일 경기 정보 자동 동기화 작업 시작] ====================");
        LocalDate today = LocalDate.now();

        // [1] 동기화 타이밍 검사
        if (!isUpdateTime(today)) {
            logger.info(">>> 동기화 작업을 건너뜁니다.");
            logger.info("==================== [금일 경기 정보 자동 동기화 작업 종료] ====================");
            return;
        }

        // [2] 경기 조회 및 처리
        logger.info(">>> 금일 전체 경기 목록 조회 중...");
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        List<Match> todayMatches = matchService.getMatchesByDate(startOfDay, startOfNextDay);

        if (todayMatches.isEmpty()) {
            logger.info(">>> 금일 예정된 경기가 없습니다. 동기화 작업을 종료합니다.");
            logger.info("==================== [금일 경기 정보 자동 동기화 작업 종료] ====================");
            return;
        }

        logger.info(">>> 동기화 대상 경기 수: {}", todayMatches.size());
        syncMatchService.syncTodaysMatchesFromLolEsportsApi(todayMatches);
        logger.info("==================== [금일 경기 정보 자동 동기화 작업 완료] ====================");
    }

    public boolean isUpdateTime(LocalDate date) {
        // 1. 해당 날짜 경기 중 가장 이른 시간 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        Optional<LocalDateTime> firstMatchTime = matchService.getFirstMatchTimeOfDay(startOfDay, endOfDay);

        if (firstMatchTime.isEmpty()) return false;

        // 2. 첫 경기 시작시간 -1시간 전부터 갱신 시작
        LocalDateTime updateStartTime = firstMatchTime.get().minusHours(1);

        // 3. 현재 시각이 그 시간 이후면 true
        return LocalDateTime.now().isAfter(updateStartTime);
    }



}

