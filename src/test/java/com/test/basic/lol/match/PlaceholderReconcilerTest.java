package com.test.basic.lol.match;

import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.domain.match.PlaceholderReconciler;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.team.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("== 밀려난 대진표 자리 정리 단위테스트 ==")
class PlaceholderReconcilerTest {

    private static final LocalDateTime SLOT = LocalDateTime.of(2026, 8, 26, 17, 0);
    private static final String LCK = "98767991310872058";

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private PlaceholderReconciler reconciler;

    private void mode(String mode) {
        ReflectionTestUtils.setField(reconciler, "mode", mode);
    }

    private static Team team(String slug) {
        Team t = new Team();
        t.setSlug(slug);
        return t;
    }

    private static Match match(String matchId, String leagueId, LocalDateTime startTime, String... teamSlugs) {
        League league = new League();
        league.setLeagueId(leagueId);

        Match m = new Match();
        m.setMatchId(matchId);
        m.setLeague(league);
        m.setStartTime(startTime);
        m.setMatchTeams(java.util.Arrays.stream(teamSlugs).map(slug -> {
            MatchTeam mt = new MatchTeam();
            mt.setMatch(m);
            mt.setTeam(team(slug));
            return mt;
        }).toList());
        return m;
    }

    @Test
    @DisplayName("log모드에서는_대상을_찾아도_데이터를_바꾸지_않는다")
    void logMode_doesNotMutate() {
        mode("log");
        Match placeholder = match("p1", LCK, SLOT, "tbd");
        when(matchRepository.findAllForReconcile())
                .thenReturn(List.of(placeholder, match("r1", LCK, SLOT, "bro", "kt")));

        int changed = reconciler.reconcile();

        assertThat(changed).isZero();
        assertThat(placeholder.getHiddenAt()).isNull();
    }

    @Test
    @DisplayName("같은리그_같은시각에_확정경기가있으면_자리를_숨긴다")
    void applyMode_hidesSupersededPlaceholder() {
        mode("apply");
        Match placeholder = match("p1", LCK, SLOT, "tbd");
        Match real = match("r1", LCK, SLOT, "bro", "kt");
        when(matchRepository.findAllForReconcile()).thenReturn(List.of(placeholder, real));

        int changed = reconciler.reconcile();

        assertThat(changed).isEqualTo(1);
        assertThat(placeholder.getHiddenAt()).isNotNull();
        assertThat(placeholder.getHiddenReason()).isEqualTo("SUPERSEDED_PLACEHOLDER");
        assertThat(real.getHiddenAt()).isNull();
    }

    @Test
    @DisplayName("대진미확정_자리만_있으면_숨기지_않는다")
    void keepsPendingPlaceholder() {
        // "이 시간에 경기가 예정돼 있다"는 정보이므로 지우면 안 된다.
        mode("apply");
        Match placeholder = match("p1", LCK, SLOT, "tbd");
        when(matchRepository.findAllForReconcile()).thenReturn(List.of(placeholder));

        assertThat(reconciler.reconcile()).isZero();
        assertThat(placeholder.getHiddenAt()).isNull();
    }

    @Test
    @DisplayName("다른리그의_확정경기는_영향을_주지_않는다")
    void differentLeagueDoesNotSupersede() {
        mode("apply");
        Match placeholder = match("p1", LCK, SLOT, "tbd");
        Match otherLeague = match("r1", "other-league", SLOT, "bro", "kt");
        when(matchRepository.findAllForReconcile()).thenReturn(List.of(placeholder, otherLeague));

        assertThat(reconciler.reconcile()).isZero();
        assertThat(placeholder.getHiddenAt()).isNull();
    }

    @Test
    @DisplayName("팀정보가_없는_경기는_자리로_보지_않는다")
    void matchWithoutTeamsIsNotPlaceholder() {
        // 대진표 자리가 아니라 팀 데이터가 유실된 실제 경기다. 숨기면 완료 경기가 사라진다.
        mode("apply");
        Match noTeams = match("n1", LCK, SLOT);
        when(matchRepository.findAllForReconcile())
                .thenReturn(List.of(noTeams, match("r1", LCK, SLOT, "bro", "kt")));

        assertThat(reconciler.reconcile()).isZero();
        assertThat(noTeams.getHiddenAt()).isNull();
    }

    @Test
    @DisplayName("더이상_밀려나지_않으면_숨김을_되돌린다")
    void restoresWhenNoLongerSuperseded() {
        mode("apply");
        Match placeholder = match("p1", LCK, SLOT, "tbd");
        placeholder.setHiddenAt(LocalDateTime.now());
        placeholder.setHiddenReason("SUPERSEDED_PLACEHOLDER");
        // 같은 슬롯에 확정 경기가 없다
        when(matchRepository.findAllForReconcile()).thenReturn(List.of(placeholder));

        assertThat(reconciler.reconcile()).isEqualTo(1);
        assertThat(placeholder.getHiddenAt()).isNull();
        assertThat(placeholder.getHiddenReason()).isNull();
    }

    @Test
    @DisplayName("다른_이유로_숨긴_경기는_건드리지_않는다")
    void doesNotRestoreOtherReasons() {
        mode("apply");
        Match hiddenByOther = match("x1", LCK, SLOT, "tbd");
        hiddenByOther.setHiddenAt(LocalDateTime.now());
        hiddenByOther.setHiddenReason("MANUAL_HIDE");
        when(matchRepository.findAllForReconcile()).thenReturn(List.of(hiddenByOther));

        assertThat(reconciler.reconcile()).isZero();
        assertThat(hiddenByOther.getHiddenAt()).isNotNull();
    }
}
