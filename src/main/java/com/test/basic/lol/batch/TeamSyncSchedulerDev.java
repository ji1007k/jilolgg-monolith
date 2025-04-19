package com.test.basic.lol.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

@Configuration
public class TeamSyncSchedulerDev {

    private static final Logger logger = LoggerFactory.getLogger(TeamSyncSchedulerDev.class);
    private final TeamBatchService teamBatchService;
    private final Environment env;

    public TeamSyncSchedulerDev(TeamBatchService teamBatchService, Environment env) {
        this.teamBatchService = teamBatchService;
        this.env = env;
    }

    @Scheduled(cron = "0 */3 * * * *", zone = "Asia/Seoul")
    public void syncTeamsDev() {
        if (!Arrays.asList(env.getActiveProfiles()).contains("dev")) {
            return; // ❌ dev가 아니면 실행 안 함
        }

        logger.info("[DEV][팀 동기화] LoL Esports API로부터 팀 정보 동기화 시작");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[DEV][팀 동기화] 팀 정보 동기화 완료");
    }
}

