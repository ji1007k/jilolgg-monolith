package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("dev")
public class TeamSyncSchedulerDev {

    private static final Logger logger = LoggerFactory.getLogger(TeamSyncSchedulerDev.class);
    private final TeamBatchService teamBatchService;

    public TeamSyncSchedulerDev(TeamBatchService teamBatchService) {
        this.teamBatchService = teamBatchService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void syncTeamsDev() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        teamBatchService.syncTeamsFromLolEsportsApi();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }
}

