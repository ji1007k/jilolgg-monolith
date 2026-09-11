package com.test.basic.lol.domain.team;

import com.test.basic.lol.domain.player.PlayerMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = PlayerMapper.class)
public interface TeamMapper {
    @Named("teamToTeamDto")
    @Mapping(source = "league.leagueId", target = "leagueId")  // Team의 leagueId를 TeamDto로 매핑
    @Mapping(target = "players", ignore = true)  // 목록 조회는 로스터 불필요 - N+1 방지
    TeamDto teamToTeamDto(Team team);

    @Mapping(source = "league.leagueId", target = "leagueId")
    TeamDto teamToTeamDetailDto(Team team);  // 팀 상세 조회 전용 - 로스터 포함
}
