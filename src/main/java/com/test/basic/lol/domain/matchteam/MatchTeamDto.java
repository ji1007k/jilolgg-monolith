package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.team.TeamDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MatchTeamDto {
    private String outcome;
    private Integer gameWins;
    private TeamDto team;

    public MatchTeamDto(String outcome, int gameWins, TeamDto team) {
        this.outcome = outcome;
        this.gameWins = gameWins;
        this.team = team;
    }
}
