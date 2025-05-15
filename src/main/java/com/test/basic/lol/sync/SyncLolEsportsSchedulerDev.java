package com.test.basic.lol.sync;

import com.test.basic.lol.matches.Match;
import com.test.basic.lol.matches.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        logger.info("==================== 금일 경기 정보 자동 동기화 작업 시작 ====================");
        // [1] 오늘 경기 목록 조회
        logger.info(">>> 금일 경기 목록 조회 중.");
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();                    // 2025-05-15T00:00
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();    // 2025-05-16T00:00
        List<Match> matches = matchService.getMatchesByDate(startOfDay, startOfNextDay);

        // [2] 금일 경기 없으면 동기화 작업 종료
        if (matches.isEmpty()) {
            logger.info(">>> 금일 경기가 없습니다. 동기화 작업을 종료합니다.");
        }

        // [3] 금일 경기 있는 경우 동기화 작업 계속
        logger.info(">>> 금일 경기 개수: {}", matches.size());
        syncMatchService.syncTodaysMatchesFromLolEsportsApi(matches);
        logger.info("==================== 금일 경기 정보 자동 동기화 작업 완료 ====================");
    }



}

