package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class TeamSyncSchedulerProd {
    private static final Logger logger = LoggerFactory.getLogger(TeamSyncSchedulerProd.class);

    private final TeamBatchService teamBatchService;

    public TeamSyncSchedulerProd(TeamBatchService teamBatchService) {
        this.teamBatchService = teamBatchService;
    }

    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Seoul")
    public void syncTeamsProd() {
        logger.info("[PROD][팀 동기화] 새벽 정기 작업 시작 - LoL Esports API 팀 정보 동기화");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[PROD][팀 동기화] 정기 작업 완료 - 팀 정보 동기화 성공");
    }
}
