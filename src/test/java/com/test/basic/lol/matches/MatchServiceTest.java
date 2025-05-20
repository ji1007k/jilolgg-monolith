package com.test.basic.lol.matches;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    @BeforeEach
    void setup() {

    }

    // 금일 경기일정 정보 조회 (leagueId, 날짜)
    @Test
    void testGetTodaysMatchesByLeagueId() {
        String leagueId = "98767991310872058";  // LCK
        LocalDate today = LocalDate.now();  // 주의: 이거 값 생성 후 밤12시 지나면 테스트 통과 못할 수 있음
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        Match match1 = new Match();
        match1.setStartTime(LocalDateTime.now());
        Match match2 = new Match();
        match2.setStartTime(LocalDateTime.now());

        when(matchRepository.findMatchByLeagueIdAndDate(leagueId, startOfDay, endOfDay)).thenReturn(List.of(match1, match2));
        when(matchMapper.entityToDto(match1)).thenReturn(new MatchDto());
        when(matchMapper.entityToDto(match2)).thenReturn(new MatchDto());

        List<MatchDto> matches = matchService.getMatchesByLeagueIdAndDate(leagueId, today, today);

        assertThat(matches.size()).isEqualTo(2);
    }


}
