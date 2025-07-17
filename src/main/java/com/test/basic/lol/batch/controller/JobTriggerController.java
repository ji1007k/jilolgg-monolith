package com.test.basic.lol.batch.controller;

import com.test.basic.lol.batch.service.BatchJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//  CommandLineRunner 또는 Scheduler로 실행하거나
//  REST API 등을 통해 JobLauncher로 외부에서 실행
//  사용 예) 처음 서버 올릴 때 API에서 데이터 싹 받아와서 DB에 넣는 작업을 자동으로 실행

@RestController
@RequestMapping("/lol/batch")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "[TEST] Batch Job Trigger API", description = "Batch Job Trigger API")
public class JobTriggerController {

    private final BatchJobService batchJobService;
//    private final Job exampleJob;

    @GetMapping("/run-match-job")
    @Operation(summary = "경기 일정 갱신 배치", description = "경기 일정 갱신 배치 비동기 API (리그 파티셔닝 적용)")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public String runMatchJob(@RequestParam String year) {
        batchJobService.executeMatchSyncJob(year);
        return "경기 일정 갱신 배치 Job 시작. " + year + "년";
    }


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

}
