package com.test.basic.lol.sync;

public record SyncExecutionResult(
        boolean success,
        boolean lockAcquired,
        String message,
        Long elapsedMs
) {
}
