package com.test.basic.lol.batch;


import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.match.MatchApiService;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.TeamRepository;
import org.springframework.batch.core.Job;
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
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class MatchBatchConfig {

    // TODO: Job, Step, Reader, Processor, Writer Bean 정의
    //  - Job: syncMatchJob
    //  - Step: syncMatchStep
    //  - Reader: MatchItemReader (API 호출)
    //  - Processor: MatchItemProcessor (EventDto -> MatchAggregate)
    //  - Writer: MatchItemWriter (MatchAggregate -> DB 저장)

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

    @Bean
    public Step syncMatchStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              MatchItemReader matchItemReader,
                              MatchItemProcessor matchItemProcessor,
                              MatchItemWriter matchItemWriter) {
        return new StepBuilder("syncMatchStep", jobRepository)
                .<MatchEventWithLeague, MatchAggregate>chunk(
                        50,
                        transactionManager
                )
                .reader(matchItemReader)
                .processor(matchItemProcessor)
                .writer(matchItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public MatchItemReader matchItemReader(
            @Value("#{jobParameters['leagueId']}") String leagueId,
            @Value("#{jobParameters['targetYear']}") int targetYear,
            MatchApiService matchApiService,
            LeagueRepository leagueRepository) {

        return new MatchItemReader(
                leagueId,
                targetYear,
                matchApiService,
                leagueRepository);
    }

    @Bean
    public MatchItemProcessor matchItemProcessor() {
        return new MatchItemProcessor();
    }

    @Bean
    public MatchItemWriter matchItemWriter(
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            MatchTeamRepository matchTeamRepository
    ) {
        return new MatchItemWriter(
                matchRepository,
                teamRepository,
                matchTeamRepository);
    }
}
