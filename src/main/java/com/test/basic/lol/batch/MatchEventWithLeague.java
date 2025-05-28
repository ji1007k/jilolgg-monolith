package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.league.League;

public class MatchEventWithLeague {
    private final MatchScheduleResponse.EventDto event;
    private final League league;

    public MatchEventWithLeague(MatchScheduleResponse.EventDto event, League league) {
        this.event = event;
        this.league = league;
    }

    public MatchScheduleResponse.EventDto getEvent() {
        return event;
    }

    public League getLeague() {
        return league;
    }
}
