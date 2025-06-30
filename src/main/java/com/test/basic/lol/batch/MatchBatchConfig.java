package com.test.basic.lol.batch;


import com.test.basic.lol.domain.league.LeagueService;
import com.test.basic.lol.domain.match.MatchApiService;
import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import com.test.basic.lol.domain.team.TeamService;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

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
public class MatchBatchConfig {

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
                .build();
    }

    // 마스터 Step
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
                .taskExecutor(limitedTaskExecutor)
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
                .skipLimit(3)               // 최대 10개까지 건너뛰기
                .build();
    }

    @Bean
    // StepScope: 매 파티션마다 독립된 Reader 생성 (Spring Batch 추천 방식) -> Thread-safe를 위한 설정
    @StepScope
    public MatchItemReader matchItemReader(
            @Value("#{stepExecutionContext['leagueId']}") String leagueId,
            @Value("#{stepExecutionContext['targetYear']}") Integer targetYear,
            MatchApiService matchApiService,
            LeagueService leagueService) {

        log.info("MatchItemReader Bean 생성 - LeagueId: {}, TargetYear: {}", leagueId, targetYear);
        
        return new MatchItemReader(
                leagueId,
                targetYear != null ? targetYear : Year.now().getValue(),
                matchApiService,
                leagueService);
    }

    @Bean
    public MatchItemProcessor matchItemProcessor() {
        return new MatchItemProcessor();
    }

    @Bean
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
    @Bean
    public TaskExecutor limitedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);    // 기본적으로 유지할 병렬 스레드 수 (ex. CPU 수 * 2 이하)
        executor.setMaxPoolSize(20);     // 최대 확장 스레드 수
        executor.setQueueCapacity(30);   // 버퍼 역할 큐 0 -> 대기x 바로 실행
        executor.setThreadNamePrefix("thread-batch-match-partition-");
        executor.initialize();
        return executor;
    }


}
