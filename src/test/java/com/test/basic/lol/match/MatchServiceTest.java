package com.test.basic.lol.match;

import com.test.basic.lol.domain.match.*;
import com.test.basic.lol.domain.match.mapping.MatchExternalMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("== 경기 조회 캐시 동작 단위테스트 ==")
public class MatchServiceTest {

    private static final String LEAGUE_ID = "98767991310872058";  // LCK

    @Mock
    private MatchApiService matchApiService;
    @Mock
    private MatchCacheService matchCacheService;
    @Mock
    private MatchMapper matchMapper;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchExternalMappingRepository matchExternalMappingRepository;

    @InjectMocks
    private MatchService matchService;

    private static MatchDto dto(String matchId) {
        MatchDto dto = new MatchDto();
        dto.setMatchId(matchId);
        return dto;
    }

    @Test
    @DisplayName("캐시미스_DB조회후_결과를캐시에저장")
    void getMatchesByLeagueIdAndDate_cacheMiss_queriesDbAndCaches() {
        LocalDate today = LocalDate.now();
        String expectedKey = String.join(":", "match", LEAGUE_ID, today.toString(), today.toString());

        // matchId를 다르게 준다: 값이 같으면 두 인스턴스가 equals상 동일해져
        // Mockito가 같은 인자로 취급하고 뒤에 등록한 stub이 앞의 것을 덮어쓴다.
        Match match1 = new Match();
        match1.setMatchId("m1");
        match1.setStartTime(LocalDateTime.now());
        Match match2 = new Match();
        match2.setMatchId("m2");
        match2.setStartTime(LocalDateTime.now());

        // 캐시 미스
        when(matchCacheService.getCachedMatchList(expectedKey)).thenReturn(null);
        when(matchRepository.findMatchByLeagueIdAndDate(eq(LEAGUE_ID), any(), any()))
                .thenReturn(List.of(match1, match2));
        when(matchMapper.entityToDto(match1)).thenReturn(dto("m1"));
        when(matchMapper.entityToDto(match2)).thenReturn(dto("m2"));
        // 외부 매핑 없음 -> dedupe는 원본을 그대로 돌려준다
        when(matchExternalMappingRepository
                .findAllByProviderAndExternalMatchIdIn(anyString(), anyList()))
                .thenReturn(List.of());

        List<MatchDto> matches = matchService.getMatchesByLeagueIdAndDate(LEAGUE_ID, today, today);

        assertThat(matches).extracting(MatchDto::getMatchId).containsExactly("m1", "m2");
        // 조회 결과가 캐시에 적재되어야 다음 호출에서 DB를 타지 않는다
        verify(matchCacheService).cacheMatchList(expectedKey, matches);
    }

    @Test
    @DisplayName("캐시히트_DB를조회하지않고캐시값반환")
    void getMatchesByLeagueIdAndDate_cacheHit_skipsDb() {
        LocalDate today = LocalDate.now();
        String expectedKey = String.join(":", "match", LEAGUE_ID, today.toString(), today.toString());

        when(matchCacheService.getCachedMatchList(expectedKey))
                .thenReturn(List.of(dto("cached-1")));
        when(matchExternalMappingRepository
                .findAllByProviderAndExternalMatchIdIn(anyString(), anyList()))
                .thenReturn(List.of());

        List<MatchDto> matches = matchService.getMatchesByLeagueIdAndDate(LEAGUE_ID, today, today);

        assertThat(matches).extracting(MatchDto::getMatchId).containsExactly("cached-1");
        verify(matchRepository, never()).findMatchByLeagueIdAndDate(any(), any(), any());
        verify(matchCacheService, never()).cacheMatchList(any(), any());
    }
}
