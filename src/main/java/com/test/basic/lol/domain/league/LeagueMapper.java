package com.test.basic.lol.domain.league;

import com.test.basic.lol.domain.team.TeamMapper;
import org.mapstruct.Mapper;

// componentModel 옵션 설정 -> 컴포넌트 어노테이션이 붙은 구현체 클래스처럼 처리됨 -> Bean으로 등록 -> 의존성 주입 가능
//  @Component
//  public class MatchMapperImpl implements MatchMapper {}
@Mapper(componentModel = "spring", uses = {TeamMapper.class})  // MapStruct가 생성하는 매퍼 클래스를 Spring Bean으로 등록해 주는 설정
public interface LeagueMapper {
    LeagueDto entityToLeagueDto(League league);

}
