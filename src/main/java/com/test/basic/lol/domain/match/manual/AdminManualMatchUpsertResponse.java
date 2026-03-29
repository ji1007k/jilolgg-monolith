package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;
import java.util.List;

public record AdminManualMatchUpsertResponse(
        String matchId,
        String leagueId,
        String tournamentId,
        LocalDateTime startTime,
        String blockName,
        Integer bestOf,
        String strategy,
        String state,
        List<String> teamIds,
        boolean lockStartTime,
        boolean lockBlockName
) {
}
