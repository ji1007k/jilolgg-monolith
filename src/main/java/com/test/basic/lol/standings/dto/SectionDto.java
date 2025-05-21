package com.test.basic.lol.standings.dto;

import com.test.basic.lol.api.dto.standings.StandingsResponse;
import lombok.Data;

import java.util.List;

@Data
public class SectionDto {
    private String name;    // 플레이인, 플레이오프, 장로, 바론, ...
    public List<StandingsResponse.MatchDto> matches;
    public List<StandingsResponse.RankingDto> rankings;
//    private List<TournamentTeamRankingDto> rankings;
}
