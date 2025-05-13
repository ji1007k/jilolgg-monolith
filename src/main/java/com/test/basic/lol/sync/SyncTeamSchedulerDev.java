package com.test.basic.lol.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("dev")
public class SyncTeamSchedulerDev {

    private static final Logger logger = LoggerFactory.getLogger(SyncTeamSchedulerDev.class);
    private final SyncTeamService syncTeamService;

    public SyncTeamSchedulerDev(SyncTeamService syncTeamService) {
        this.syncTeamService = syncTeamService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void syncTeamsDev() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        syncTeamService.syncTeamsFromLolEsportsApi();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }
}

