package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class TeamSyncSchedulerDev {
    private static final Logger logger = LoggerFactory.getLogger(TeamSyncSchedulerDev.class);

    private final TeamBatchService teamBatchService;

    public TeamSyncSchedulerDev(TeamBatchService teamBatchService) {
        this.teamBatchService = teamBatchService;
    }

    @Scheduled(cron = "0 */3 * * * *", zone = "Asia/Seoul")
    public void syncTeamsDev() {
        logger.info("[DEV][팀 동기화] LoL Esports API로부터 팀 정보 동기화 시작");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[DEV][팀 동기화] 팀 정보 동기화 완료");
    }
}
