package com.test.basic.lol.batch;

import com.test.basic.lol.domain.match.MatchCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO
//  CommandLineRunner 또는 Scheduler로 실행하거나
//  REST API 등을 통해 JobLauncher로 외부에서 실행
//  사용 예) 처음 서버 올릴 때 API에서 데이터 싹 받아와서 DB에 넣는 작업을 자동으로 실행

@RestController
@RequestMapping("/lol/batch")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "[TEST] Batch Job Trigger API", description = "Batch Job Trigger API")
public class JobTriggerController {

    private final JobLauncher jobLauncher;  // Job을 실행하는 컴포넌트 (Job 실행 트리거)
    private final Job syncMatchJob;
//    private final Job exampleJob;

    private final MatchCacheService matchCacheService;
    /*private final LeagueService leagueService;
    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            // LCK, LCK CL
            "98767991310872058",
            "98767991335774713",
            // 국제 대회 (FIRST STAND, MSI, WORLDS)
            "113464388705111224",
            "98767991325878492",
            "98767975604431411",
            // LPL, LEC, LJL
            "98767991314006698",
            "98767991302996019",
            "98767991349978712");*/


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
    @Operation(summary = "경기 일정 갱신 배치", description = "경기 일정 갱신 배치 API (리그 파티셔닝 적용)")
    public String runMatchJob(@RequestParam String year) {
        StopWatch sw = new StopWatch();
        sw.start();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("targetYear", Long.valueOf(year))
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(syncMatchJob, params);  // 한 번만 실행

            // 배치 종료 후 경기 일정 캐시 무효화
            matchCacheService.invalidateAllCaches();
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            // 로그 출력
            log.error("Job 실행 실패", e);
            return "Job 실행 실패";
        }

        sw.stop();
        log.info(">>> 소요 시간: {}ms", sw.getTotalTimeMillis());

        return "Match Job 실행 완료. 소요 시간: " + sw.getTotalTimeMillis() + "ms";
    }
}
