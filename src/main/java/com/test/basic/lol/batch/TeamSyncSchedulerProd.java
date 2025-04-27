package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("prod")
public class TeamSyncSchedulerProd {
    private static final Logger logger = LoggerFactory.getLogger(TeamSyncSchedulerProd.class);

    private final TeamBatchService teamBatchService;

    public TeamSyncSchedulerProd(TeamBatchService teamBatchService) {
        this.teamBatchService = teamBatchService;
    }

    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Seoul")
    public void syncTeamsProd() {
        logger.info("==================== 팀 정보 자동 동기화 작업 시작 ====================");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("==================== 팀 정보 자동 동기화 작업 완료 ====================");
    }
}
