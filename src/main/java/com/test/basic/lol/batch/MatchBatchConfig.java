package com.test.basic.lol.batch;


import com.test.basic.lol.domain.league.LeagueService;
import com.test.basic.lol.domain.match.MatchApiService;
import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import com.test.basic.lol.domain.team.TeamService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Year;

/** Job, Step, Reader, Processor, Writer Bean 정의
    - Job: syncMatchJob
    - Step: syncMatchStep
    - Reader: MatchItemReader (API 호출)
    - Processor: MatchItemProcessor (EventDto -> MatchAggregate)
    - Writer: MatchItemWriter (MatchAggregate -> DB 저장)
*/
@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@EnableAsync
public class MatchBatchConfig {

    public final SimpleJobListener simpleJobListener;
    public final SimpleStepListener simpleStepListener;
    public final SimpleRetryListener simpleRetryListener;

    // 기본 제공 스키마 파일을 실행하여 Job 실행 상태, 이력 등을 저장할 메타 테이블 생성 (쿼리 경로 직접 지정)
    @Profile({"dev", "prod"}) // 개발/운영만
    @Bean
    public DataSourceInitializer batchDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"));

        // 에러 무시 설정 추가 (EX. 이미 관련 테이블 생성된 경우)
        populator.setContinueOnError(true); // SQL 실행 중 에러가 나도 계속 실행
        populator.setIgnoreFailedDrops(true); // DROP 구문 실패 무시 (선택적)

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }


    @Bean
    public Job syncMatchJob(JobRepository jobRepository, Step syncMatchStep) {
        return new JobBuilder("syncMatchJob", jobRepository)
                .start(syncMatchStep)
                .listener(simpleJobListener)
                .incrementer(new RunIdIncrementer()) // 고유 실행 ID 자동 생성
                .build();
    }

    // 마스터 Step: 파티셔닝 관리. 데이터 처리x
    // 내부 Step 들이 chunk 기반이면 트랜잭션 매니저 필수
    //  ignored prefix: "사용 안 함"을 표현하는 네이밍 규칙(but. Spring은 의존성 요구함)
    @Bean
    public Step syncMatchStep(JobRepository jobRepository,
                              PlatformTransactionManager ignoredTransactionManager,
                              LeaguePartitioner leaguePartitioner,
                              Step partitionedMatchStep,
                              TaskExecutor limitedTaskExecutor) {
        return new StepBuilder("syncMatchStep", jobRepository)
                .partitioner("partitionedMatchStep", leaguePartitioner)
                .step(partitionedMatchStep)
                .taskExecutor(limitedTaskExecutor)  // Step 내부에서 파티션들을 병렬로 실행
                .gridSize(5)    // 병렬 작업(파티션) 수 조절
                .allowStartIfComplete(true)       // 완료된 파티션 재시작 허용
                .build();
    }

    // 마스터 Step 파티션 내부에서 실행될 Step
    @Bean
    public Step partitionedMatchStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     MatchItemReader matchItemReader,
                                     MatchItemProcessor matchItemProcessor,
                                     MatchItemWriter matchItemWriter) {
        return new StepBuilder("partitionedMatchStep", jobRepository)
                .<MatchEventWithLeague, MatchAggregate>chunk(
                        100, // 하나의 트랜잭션 단위로 처리할 아이템 수.
                        transactionManager
                )
                .reader(matchItemReader)
                .processor(matchItemProcessor)
                .writer(matchItemWriter)    // 내부적으로 flush + clear 수행
                // 실패 재처리
                .faultTolerant()
                .retryLimit(3)              // 최대 3회 재시도
                .retry(Exception.class)     // 특정 예외만 재시도
                .skip(DataIntegrityViolationException.class)   // 데이터 무결성 오류 스킵
                .skip(ConstraintViolationException.class)     // 제약 조건 위반 스킵
                .skipLimit(3)               // 최대 3개까지 건너뛰기
                .listener(simpleStepListener)     // Step 리스너 (Step 시작/종료 시)
                .listener(simpleRetryListener)    // 재시도 리스너 (재시도 시작/오류/종료 시)
                .build();
    }

    @Bean
    // StepScope: 매 파티션마다 독립된 Reader 생성 (Spring Batch 추천 방식) -> Thread-safe를 위한 설정
    @StepScope
    public MatchItemReader matchItemReader(
            @Value("#{stepExecutionContext['leagueId']}") String leagueId,
            @Value("#{stepExecutionContext['targetYear']}") String targetYear,
            MatchApiService matchApiService,
            LeagueService leagueService) {

        log.info("MatchItemReader Bean 생성 - LeagueId: {}, TargetYear: {}", leagueId, targetYear);
        
        return new MatchItemReader(
                leagueId,
                targetYear,
                matchApiService,
                leagueService);
    }

    @Bean
    @StepScope
    public MatchItemProcessor matchItemProcessor() {
        return new MatchItemProcessor();
    }

    @Bean
    @StepScope
    public MatchItemWriter matchItemWriter(
            MatchService matchService,
            TeamService teamService,
            MatchTeamService matchTeamService
    ) {
        return new MatchItemWriter(
                matchService,
                teamService,
                matchTeamService);
    }




// 1) 병렬 수 제한 예 ================================================================
    /*@Bean
    public Step multiThreadedStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        return new StepBuilder("multiThreadedStep", jobRepository)
                .<String, String>chunk(100, transactionManager) // chunk size 너무 크면 GC 문제 유발
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .taskExecutor(limitedTaskExecutor())    // 병렬 실행. 직접 정의한 병렬 executor 주입
                .build();
    }*/

    // Spring Batch 5.x 부터 TaskExecutor 직접 설정해서 병렬 제어
    // 멀티스레드 Step 처리에서 스레드 풀을 설정
    @Bean(name = "limitedTaskExecutor")
    public TaskExecutor limitedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);    // 기본적으로 유지할 병렬 스레드 수 (ex. CPU 수 * 2 이하)
        executor.setMaxPoolSize(20);     // 최대 확장 스레드 수
        executor.setQueueCapacity(30);   // 버퍼 역할 큐 0 -> 대기x 바로 실행
        executor.setThreadNamePrefix("thread-batch-match-partition-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // 현재 코드 동작 (동기 방식) - 결과 받을 수 있지만 분산락 안됨
    //    Request 1: API → 분산락 획득 → jobLauncher.run() → Job 완료까지 5초 대기 → 락 해제 → 응답
    //    Request 2: API → Request 1이 완료될 때까지 대기 → 분산락 획득 시도
    //    락 획득 이전에 이미 Spring Batch 레벨에서 동기화가 일어나고 있어 분산락까지 도달하지 못함
    // 비동기 JobLauncher 적용 후 - 분산락 되지만 결과 못받음
    //    Request 1: API → 분산락 획득 → asyncJobLauncher.run() → 즉시 응답
    //                      ↓
    //              백그라운드에서 Job 실행 (5초) → Job 완료 → 락 해제
    //    Request 2: API → 분산락 획득 시도 → 1초 후 실패 → 예외 응답
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);

        // API 요청이 Job 완료를 기다리지 않고 즉시 응답
        // Job 시작 레벨의 비동기성
        TaskExecutor taskExecutor = limitedTaskExecutor();
        jobLauncher.setTaskExecutor(taskExecutor);  // Job 자체를 비동기로 시작

        try {
            jobLauncher.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("JobLauncher 초기화 실패", e);
        }

        return jobLauncher;
    }

}
