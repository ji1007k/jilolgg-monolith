package com.test.basic.lol.domain.match.manual;

import java.time.LocalDateTime;
import java.util.List;

public record ExternalMatchCandidateResponse(
        String externalMatchId,
        String leagueId,
        String tournamentId,
        LocalDateTime startTime,
        String state,
        String blockName,
        Integer bestOf,
        String strategy,
        List<String> teamCodes,
        List<String> teamNames,
        int score
) {
}
