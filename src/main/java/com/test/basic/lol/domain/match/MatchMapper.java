package com.test.basic.lol.domain.match;

import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamMapper;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// componentModel 옵션 설정 -> 컴포넌트 어노테이션이 붙은 구현체 클래스처럼 처리됨 -> Bean으로 등록 -> 의존성 주입 가능
//  @Component
//  public class MatchMapperImpl implements MatchMapper {}
@Mapper(componentModel = "spring", uses = {TeamMapper.class, MatchTeamMapper.class})  // MapStruct가 생성하는 매퍼 클래스를 Spring Bean으로 등록해 주는 설정
public interface MatchMapper {

    @Mapping(target = "winningTeamCode", expression = "java(getWinningTeamCode(match))")
    @Mapping(source = "startTime", target = "startTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "matchTeams", target = "participants")    // 필드 이름이 다를 경우 매핑 명시
    MatchDto entityToDto(Match match);

    default String getWinningTeamCode(Match match) {
        return match.getMatchTeams().stream()
                .filter(matchTeam -> "win".equalsIgnoreCase(matchTeam.getOutcome()))
                .map(MatchTeam::getTeam)
                .map(Team::getCode)
                .findFirst()
                .orElse(null);
    }
}
