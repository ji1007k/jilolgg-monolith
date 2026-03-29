package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;
import java.util.List;

public record AdminManualMatchUpsertRequest(
        String leagueId,
        String tournamentId,
        LocalDateTime startTime,
        String blockName,
        Integer bestOf,
        String state,
        List<String> teamIds,
        Boolean lockStartTime,
        Boolean lockBlockName
) {
}
