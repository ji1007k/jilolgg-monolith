package com.test.basic.lol.matchgame;

import com.test.basic.lol.api.esports.dto.MatchDetailResponse;
import com.test.basic.lol.domain.matchgame.MatchGameSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MatchGameSyncServiceTest {

    @SuppressWarnings("unchecked")
    private Map<Integer, String> resolve(MatchDetailResponse.MatchDto dto) throws Exception {
        Method m = MatchGameSyncService.class.getDeclaredMethod("resolveWinners", MatchDetailResponse.MatchDto.class);
        m.setAccessible(true);
        return (Map<Integer, String>) m.invoke(null, dto);
    }

    private MatchDetailResponse.MatchDto detail(int count, int aWins, int bWins, String... gameStates) {
        MatchDetailResponse.MatchDto dto = new MatchDetailResponse.MatchDto();
        MatchDetailResponse.StrategyDto strategy = new MatchDetailResponse.StrategyDto();
        strategy.setCount(count);
        dto.setStrategy(strategy);
        dto.setTeams(List.of(team("A", aWins), team("B", bWins)));
        MatchDetailResponse.GameDto[] games = new MatchDetailResponse.GameDto[gameStates.length];
        for (int i = 0; i < gameStates.length; i++) {
            games[i] = new MatchDetailResponse.GameDto();
            games[i].setNumber(i + 1);
            games[i].setState(gameStates[i]);
        }
        dto.setGames(List.of(games));
        return dto;
    }

    private MatchDetailResponse.TeamDto team(String id, int wins) {
        MatchDetailResponse.TeamDto t = new MatchDetailResponse.TeamDto();
        t.setId(id);
        MatchDetailResponse.ResultDto r = new MatchDetailResponse.ResultDto();
        r.setGameWins(wins);
        t.setResult(r);
        return t;
    }

    @Test
    @DisplayName("2대0이면_완료된_모든_세트의_승자가_확정된다")
    void sweep() throws Exception {
        assertThat(resolve(detail(3, 2, 0, "completed", "completed", "unneeded")))
                .containsEntry(1, "A").containsEntry(2, "A").hasSize(2);
    }

    @Test
    @DisplayName("2대1이면_마지막_세트만_시리즈_승자로_확정된다")
    void twoToOne() throws Exception {
        assertThat(resolve(detail(3, 1, 2, "completed", "completed", "completed")))
                .containsOnly(Map.entry(3, "B"));
    }

    @Test
    @DisplayName("진행중이고_1대1이면_확정하지_않는다")
    void inProgress() throws Exception {
        assertThat(resolve(detail(3, 1, 1, "completed", "completed", "inProgress"))).isEmpty();
    }

    @Test
    @DisplayName("승수_합과_완료_세트_수가_다르면_확정하지_않는다")
    void mismatch() throws Exception {
        assertThat(resolve(detail(3, 2, 0, "completed", "unstarted", "unstarted"))).isEmpty();
    }
}
