package com.test.basic.lol.tournaments;

import lombok.Data;

import java.util.List;

@Data
public class LeagueWithTournamentsDto {

    private List<TournamentDto> tournaments;
}
