package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.team.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TeamMapper.class)
public interface MatchTeamMapper {

    // target을 "."으로 지정하면 전체 객체를 그대로 target으로 전달
    // 로스터 없는 목록용 변환(TeamMapper.teamToTeamDto)을 명시적으로 지정 - 상세용 변환과 모호성 방지
    @Mapping(source = "team", target = "team", qualifiedByName = "teamToTeamDto")
    MatchTeamDto toDto(MatchTeam matchTeam);
}
