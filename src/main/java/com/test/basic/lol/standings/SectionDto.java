package com.test.basic.lol.standings;

import com.test.basic.lol.teams.TournamentTeamRankingDto;
import lombok.Data;

import java.util.List;

@Data
public class SectionDto {
    private String name;    // 플레이인, 플레이오프, 장로, 바론, ...
    private List<TournamentTeamRankingDto> rankings;
}
