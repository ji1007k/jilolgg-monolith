package com.test.basic.lol.match;

import com.test.basic.lol.domain.match.*;
import com.test.basic.lol.domain.match.mapping.MatchExternalMappingRepository;
import com.test.basic.lol.domain.matchteam.MatchTeamDto;
import com.test.basic.lol.domain.team.TeamDto;
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
    // ── 대진 확정으로 밀려난 플레이스홀더 숨김 ──────────────────────────

    private static final String SLOT = "2026-08-26 17:00:00";

    /** 참가 팀이 TBD 하나뿐인 대진표 자리. */
    private static MatchDto placeholderDto(String matchId, String startTime) {
        MatchDto dto = dto(matchId);
        dto.setStartTime(startTime);
        dto.setParticipants(List.of(participant("tbd", "TBDC")));
        return dto;
    }

    private static MatchDto realDto(String matchId, String startTime) {
        MatchDto dto = dto(matchId);
        dto.setStartTime(startTime);
        dto.setParticipants(List.of(participant("bro", "BRO"), participant("kt", "KT")));
        return dto;
    }

    private static MatchTeamDto participant(String slug, String code) {
        TeamDto team = new TeamDto();
        team.setSlug(slug);
        team.setCode(code);
        MatchTeamDto participant = new MatchTeamDto();
        participant.setTeam(team);
        return participant;
    }

    private List<MatchDto> queryWithCachedList(List<MatchDto> cached) {
        LocalDate today = LocalDate.now();
        String key = String.join(":", "match", LEAGUE_ID, today.toString(), today.toString());

        when(matchCacheService.getCachedMatchList(key)).thenReturn(cached);
        when(matchExternalMappingRepository
                .findAllByProviderAndExternalMatchIdIn(anyString(), anyList()))
                .thenReturn(List.of());

        return matchService.getMatchesByLeagueIdAndDate(LEAGUE_ID, today, today);
    }

    @Test
    @DisplayName("같은시각에_확정경기가있으면_플레이스홀더는_숨긴다")
    void hidesPlaceholder_whenRealMatchOccupiesSameSlot() {
        List<MatchDto> result = queryWithCachedList(List.of(
                placeholderDto("115548147900750289", SLOT),
                realDto("117030752644841571", SLOT)
        ));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatchId()).isEqualTo("117030752644841571");
    }

    @Test
    @DisplayName("대진미확정_플레이스홀더만_있으면_그대로_보여준다")
    void keepsPlaceholder_whenNoRealMatchInSlot() {
        // "이 시간에 경기가 예정돼 있다"는 정보이므로 지우면 안 된다.
        List<MatchDto> result = queryWithCachedList(List.of(
                placeholderDto("115548147900750289", SLOT),
                placeholderDto("115548147900750295", "2026-08-27 17:00:00")
        ));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("다른시각의_플레이스홀더는_영향받지_않는다")
    void keepsPlaceholder_inDifferentSlot() {
        List<MatchDto> result = queryWithCachedList(List.of(
                realDto("117030752644841571", SLOT),
                placeholderDto("115548147900750295", "2026-08-27 17:00:00")
        ));

        assertThat(result)
                .extracting(MatchDto::getMatchId)
                .containsExactlyInAnyOrder("117030752644841571", "115548147900750295");
    }

    @Test
    @DisplayName("TB_TBE같은_실제팀은_플레이스홀더로_오인하지_않는다")
    void doesNotTreatRealTeamsAsPlaceholder() {
        // 코드 접두사(TB, TBE)로 판별하면 Team Bliss가 지워진다. slug로 판별해야 한다.
        MatchDto teamBliss = dto("m-tb");
        teamBliss.setStartTime(SLOT);
        teamBliss.setParticipants(List.of(participant("team-bliss", "TB"), participant("team-blackeye", "TBE")));

        List<MatchDto> result = queryWithCachedList(List.of(
                teamBliss,
                realDto("117030752644841571", SLOT)
        ));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("팀정보가_없는_경기는_숨기지_않는다")
    void keepsMatchWithoutParticipants() {
        // 대진표 자리가 아니라 팀 데이터가 유실된 실제 경기다(현재 1,248건).
        // 같은 시각에 경기가 여러 개인 리그에서 이걸 자리로 오인하면 완료 경기가 사라진다.
        MatchDto noTeams = dto("m-noteam");
        noTeams.setStartTime(SLOT);
        noTeams.setParticipants(List.of());

        List<MatchDto> result = queryWithCachedList(List.of(
                noTeams,
                realDto("117030752644841571", SLOT)
        ));

        assertThat(result).hasSize(2);
    }

}
