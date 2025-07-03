package com.test.basic.lol.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SimpleJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job 시작: {}({})", jobExecution.getJobId(), jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Job 종료: {}({})", jobExecution.getJobId(), jobExecution.getJobInstance().getJobName());
        
        // 상세한 디버깅 정보 출력
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            log.info("Step: {}, ReadCount: {}, WriteCount: {}, CommitCount: {}, RollbackCount: {}", 
                    stepExecution.getStepName(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getCommitCount(),
                    stepExecution.getRollbackCount());
        }

        // 마스터 스텝(syncMatchStep)을 제외하고 집계
        long totalReadCount = jobExecution.getStepExecutions()
                .stream()
                .filter(stepExecution -> !stepExecution.getStepName().equalsIgnoreCase("syncMatchStep"))    // 워커 스텝만
                .mapToLong(StepExecution::getReadCount)
                .sum();
                
        long totalWriteCount = jobExecution.getStepExecutions()
                .stream()
                .filter(stepExecution -> !stepExecution.getStepName().equalsIgnoreCase("syncMatchStep"))    // 워커 스텝만
                .mapToLong(StepExecution::getWriteCount)
                .sum();

        log.info("배치 완료 - 총 읽기: {}건, 총 쓰기: {}건", totalReadCount, totalWriteCount);
        
        // Job 실행 상태도 확인
        log.info("Job Status: {}, Exit Status: {}", 
                jobExecution.getStatus(), 
                jobExecution.getExitStatus());
    }
}
