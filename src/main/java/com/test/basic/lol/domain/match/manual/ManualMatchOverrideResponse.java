package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;

public record ManualMatchOverrideResponse(
        String matchId,
        LocalDateTime startTime,
        String blockName,
        boolean lockStartTime,
        boolean lockBlockName,
        String updatedBy,
        LocalDateTime updatedAt,
        boolean appliedToCurrentMatch
) {
}
