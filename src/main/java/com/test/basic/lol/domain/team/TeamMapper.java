package com.test.basic.lol.domain.team;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeamMapper {
    @Mapping(source = "league.leagueId", target = "leagueId")  // Team의 leagueId를 TeamDto로 매핑
    TeamDto teamToTeamDto(Team team);
}
