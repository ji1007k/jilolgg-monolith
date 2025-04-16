package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TeamSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TeamSyncScheduler.class);

    private final TeamBatchService teamBatchService;

    public TeamSyncScheduler(TeamBatchService teamBatchService) {
        this.teamBatchService = teamBatchService;
    }

    // 개발) 3분마다 실행
    @Scheduled(cron = "0 */3 * * * *", zone = "Asia/Seoul")
    @Profile("dev")
    public void syncTeamsDev() {
        logger.info("[DEV][팀 동기화] LoL Esports API로부터 팀 정보 동기화 시작");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[DEV][팀 동기화] 팀 정보 동기화 완료");
    }

    // 배포) 매주 일요일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Seoul")
    @Profile("prod")
    public void syncTeamsProd() {
        logger.info("[PROD][팀 동기화] 새벽 정기 작업 시작 - LoL Esports API 팀 정보 동기화");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[PROD][팀 동기화] 정기 작업 완료 - 팀 정보 동기화 성공");
    }
}
