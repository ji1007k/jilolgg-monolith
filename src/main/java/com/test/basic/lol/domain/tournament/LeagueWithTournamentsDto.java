package com.test.basic.lol.domain.tournament;

import lombok.Data;

import java.util.List;

@Data
public class LeagueWithTournamentsDto {

    private List<TournamentDto> tournaments;
}
