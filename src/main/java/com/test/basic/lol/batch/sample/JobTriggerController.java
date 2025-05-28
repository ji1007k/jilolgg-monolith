package com.test.basic.lol.batch.sample;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TODO
//  CommandLineRunner 또는 Scheduler로 실행하거나
//  REST API 등을 통해 JobLauncher로 외부에서 실행
//  사용 예) 처음 서버 올릴 때 API에서 데이터 싹 받아와서 DB에 넣는 작업을 자동으로 실행

@RestController
@RequestMapping("/lol/batch")
@RequiredArgsConstructor
@Tag(name = "[TEST] Batch Job Trigger API", description = "Batch Job Trigger API")
public class JobTriggerController {

    private final JobLauncher jobLauncher;  // Job을 실행하는 컴포넌트 (Job 실행 트리거)
    private final Job exampleJob;
    private final Job syncMatchJob;

    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            // LCK, LCK CL
            "98767991310872058",
            "98767991335774713",
            // 국제 대회 (FIRST STAND, MSI, WORLDS)
            "113464388705111224",
            "98767991325878492",
            "98767975604431411",
            // LPL, LEC, ...
            "98767991314006698",
            "98767991302996019",
            "98767991349978712",
            "98767991299243165");

/*
    @GetMapping("/run-sample")
    public String runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(exampleJob, params);
        return "Job 실행 완료!";
    }
*/


    @GetMapping("/run-match-job")
    public String runMatchJob(@RequestParam String year) {
        for (String leagueId : MAJOR_LEAGUE_IDS) {
            JobParameters params = new JobParametersBuilder()
                    .addString("leagueId", leagueId)
                    .addLong("targetYear", Long.valueOf(year))
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            try {
                jobLauncher.run(syncMatchJob, params);
            } catch (JobExecutionAlreadyRunningException | JobRestartException
                     | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
                // 로그 출력
//                log.error("Job 실행 실패 - leagueId: {}, year: {}", leagueId, year, e);
                // 에러 메시지를 반환하거나, 필요에 따라 계속 진행할지 결정
                return "Job 실행 실패: " + e.getMessage();
            }
        }

        return "Match Job 실행 완료";
    }
}

