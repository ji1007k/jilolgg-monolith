package com.test.basic.lol.batch.service;

import com.test.basic.lol.domain.match.MatchCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

// TODO
//  비동기 job 상태 조회 API 제공(spring batch 공식 패턴)
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchJobService {

    private final MatchCacheService matchCacheService;

    private final JobLauncher jobLauncher;  // Job을 실행하는 컴포넌트 (Job 실행 트리거)
    private final Job syncMatchJob;

    private final RedissonClient redissonClient;

    @Async("limitedTaskExecutor") // 명시적으로 TaskExecutor 지정
    public void executeMatchSyncJob(String year) {
        String lockKey = "batch:sync-match";
        RLock matchSyncLock = redissonClient.getLock(lockKey);

        try {
            log.info(">>> Thread: {}, 메서드 진입", Thread.currentThread().getName());

            long startTime = System.currentTimeMillis();
            boolean isLocked = matchSyncLock.tryLock(1, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            log.info(">>> Thread: {}, 락 시도 결과: {}, 실제 대기시간: {}ms", 
                    Thread.currentThread().getName(), isLocked, (endTime - startTime));

            if (!isLocked) {
                long ttl = matchSyncLock.remainTimeToLive();
                log.warn(">>> 락 획득 실패. 현재 락 TTL: {}ms", ttl);
                throw new RuntimeException("다른 동기화 작업이 실행 중입니다.");
            }

            Thread.sleep(5000); // 락 획득 시간 늘리기 위해 강제 대기

            StopWatch sw = new StopWatch();
            sw.start();

            JobParameters params = new JobParametersBuilder()
                    .addString("targetYear", year)
                    .addLong("time", System.currentTimeMillis())
                    .addString("uuid", java.util.UUID.randomUUID().toString()) // 고유성 보장
                    .addString("thread", Thread.currentThread().getName()) // 디버깅용
                    .toJobParameters();

            jobLauncher.run(syncMatchJob, params);  // 한 번만 실행

            // 배치 종료 후 경기 일정 캐시 무효화
            matchCacheService.invalidateAllCaches();

            sw.stop();
            log.info(">>> 소요 시간: {}ms", sw.getTotalTimeMillis());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 예외 발생", e);
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("Job 실행 실패", e);
        } catch (Exception e) {
            log.error("경기 일정 갱신 배치 실행 실패: {}", e.getMessage());
        } finally {
            if (matchSyncLock != null && matchSyncLock.isHeldByCurrentThread()) {
                matchSyncLock.unlock();
                log.info("락 해제 완료");
            }
        }
    }


}
