package com.test.basic.lol.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SimpleStepListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.debug("Step 시작: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.debug("Step 종료: {} - {}", stepExecution.getStepName(), stepExecution.getExitStatus());
        log.debug("  - Read: {}, Write: {}", stepExecution.getReadCount(), stepExecution.getWriteCount());
        log.debug("  - Skip (Read/Process/Write): {}/{}/{}",
                stepExecution.getReadSkipCount(),
                stepExecution.getProcessSkipCount(),
                stepExecution.getWriteSkipCount()
        );

        if (!stepExecution.getFailureExceptions().isEmpty()) {
            stepExecution.getFailureExceptions().forEach(e ->
                    log.error("Step 예외 발생: {}", e.toString())
            );
        }

        return stepExecution.getExitStatus();
    }


}
