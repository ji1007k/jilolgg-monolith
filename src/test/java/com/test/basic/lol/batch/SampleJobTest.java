package com.test.basic.lol.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
// 명시적으로 특정 설정 클래스만 테스트하고 싶을 때 @Import를 써서 컨텍스트에 포함
@Import(BatchSampleConfig.class)
// 임베디드 DB(H2 등) 말고 실제 설정된 데이터베이스 그대로 사용
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// 테스트 클래스 인스턴스를 메서드마다 새로 생성하지 않고, 테스트 클래스 단위로 한 번만 생성
// @BeforeAll 메서드나 공유 자원을 쓰기 위해선 static을 요구하지 않게 하려면 필요
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class SampleJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job simpleJob;


    @BeforeEach
    void setup() {
        jobLauncherTestUtils.setJob(simpleJob);
    }

    @Test
    void testSimpleJob_runsSuccessfully() throws Exception {
        JobParameters jobParams = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);

        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }


    @TestConfiguration
    static class BatchTestConfig {

        @Bean
        public JobLauncherTestUtils jobLauncherTestUtils(JobLauncher jobLauncher, JobRepository jobRepository) {
            JobLauncherTestUtils jobUtils = new JobLauncherTestUtils();
            jobUtils.setJobLauncher(jobLauncher);
            jobUtils.setJobRepository(jobRepository);
            return jobUtils;
        }
    }



}
