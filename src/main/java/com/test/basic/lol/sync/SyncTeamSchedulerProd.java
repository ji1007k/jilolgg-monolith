package com.test.basic.lol.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("prod")
public class SyncTeamSchedulerProd {
    private static final Logger logger = LoggerFactory.getLogger(SyncTeamSchedulerProd.class);

    private final SyncTeamService syncTeamService;

    public SyncTeamSchedulerProd(SyncTeamService syncTeamService) {
        this.syncTeamService = syncTeamService;
    }

    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Seoul")
    public void syncTeamsProd() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        syncTeamService.syncTeamsFromLolEsportsApi();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }
}
