package com.test.basic.lol.batch;

import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({MatchBatchConfig.class/*, BatchTestConfig.class*/})
// JobLauncherTestUtils, JobRepositoryTestUtils 등의 테스트 클래스 의존성 자동 주입
//  -> BatchTestConfig.class import 안해도 됨
@SpringBatchTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // 각 객체가 클래스 당 한번만 생성되도록
@ActiveProfiles("test")
public class SyncMatchJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job syncMatchJob;

    @Autowired
//    @Qualifier("asyncJobLauncher")
    private JobLauncher jobLauncher;    // job 완료 후 응답받기 위해 기본 JobLauncher 주입

    @BeforeAll
    void setUp() {
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJob(syncMatchJob);
    }

    @AfterEach
    void tearDown() {
        // 1. 테스트 후 Job 실행 기록 삭제
        // BATCH_JOB_EXECUTION 테이블에서 모든 기록 삭제
        // BATCH_JOB_EXECUTION_CONTEXT 테이블에서 모든 기록 삭제
        // BATCH_JOB_EXECUTION_PARAMS 테이블에서 모든 기록 삭제
        // BATCH_JOB_INSTANCE 테이블에서 모든 기록 삭제
        // BATCH_STEP_EXECUTION 테이블에서 모든 기록 삭제
        // BATCH_STEP_EXECUTION_CONTEXT 테이블에서 모든 기록 삭제
        jobRepositoryTestUtils.removeJobExecutions();
    }


    // @SpringBatchTest + JobLauncherTestUtils
    @Test
    @DisplayName("전체 Batch Job 테스트(통합) - 동기식")
    void syncMatchJob_runsSuccessfully() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("leagueId", "98767991302996019")
                .addLong("targetYear", (long) Year.now(ZoneId.of("Asia/Seoul")).getValue())
                .addLong("time", System.currentTimeMillis())        // 유일한 job instance 위해 시간 파라미터 추가
                .addString("uuid", UUID.randomUUID().toString())    // 시간은 중복될 수 있어서 식별을 위한 UUID 추가
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }


    // TODO STEP, READER, PROCESSOR, WRITER별 테스트
    //  ...
}
