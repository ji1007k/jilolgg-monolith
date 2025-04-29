package com.test.basic.lol.standings;

import com.test.basic.lol.teams.Team;
import lombok.Data;

import java.util.List;

@Data
public class StandingsDto {
    private String id;        // 113503303283548977
    private String tournamentId;
    private String name;    // 정규 리그
    private String slug;    // regular_season
    private String type;

    private List<Team> rankings;
}
