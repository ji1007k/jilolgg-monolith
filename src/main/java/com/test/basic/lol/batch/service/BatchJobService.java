package com.test.basic.lol.batch.service;

import com.test.basic.lol.domain.match.MatchCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchJobService {

    private final MatchCacheService matchCacheService;

    private final JobLauncher jobLauncher;  // Job을 실행하는 컴포넌트 (Job 실행 트리거)
    private final Job syncMatchJob;

    public long executeMatchSyncJob(String year) {
        try {
            StopWatch sw = new StopWatch();
            sw.start();

            JobParameters params = new JobParametersBuilder()
                    .addString("targetYear", year)
                    .addLong("time", System.currentTimeMillis())
                    .addString("uuid", java.util.UUID.randomUUID().toString()) // 고유성 보장
                    .toJobParameters();

            jobLauncher.run(syncMatchJob, params);

            // 배치 종료 후 경기 일정 캐시 무효화
            matchCacheService.invalidateAllCaches();

            sw.stop();
            log.info(">>> 소요 시간: {}ms", sw.getTotalTimeMillis());
            return sw.getTotalTimeMillis();

        } catch (JobExecutionAlreadyRunningException | JobRestartException
                 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("Job 실행 실패", e);
            throw new IllegalStateException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
