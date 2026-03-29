package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;

public record ExternalMatchLinkStatusResponse(
        String provider,
        String externalMatchId,
        String matchId,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
