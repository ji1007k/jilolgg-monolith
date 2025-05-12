package com.test.basic.lol.tournaments;

import lombok.Data;

import java.util.List;

@Data
public class TournamentResponse {

    private TournamentData data;

    @Data
    public static class TournamentData {
        private List<LeagueWithTournamentsDto> leagues;
    }


}
