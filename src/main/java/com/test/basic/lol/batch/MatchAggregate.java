package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.match.Match;

import java.util.List;

public record MatchAggregate(Match match, List<MatchScheduleResponse.TeamDto> teams) {
    // constructor, getters, setters
}
