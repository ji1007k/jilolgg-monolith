package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;

public record ManualMatchOverrideRequest(
        LocalDateTime startTime,
        String blockName,
        Boolean lockStartTime,
        Boolean lockBlockName,
        Boolean applyImmediately
) {
}
