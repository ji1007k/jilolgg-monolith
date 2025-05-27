package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.team.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TeamMapper.class)
public interface MatchTeamMapper {

    // target을 "."으로 지정하면 전체 객체를 그대로 target으로 전달
    @Mapping(source = "team", target = "team")  // team 엔티티를 teamDto로 매핑 (TeamMapper가 처리함)
    MatchTeamDto toDto(MatchTeam matchTeam);
}
