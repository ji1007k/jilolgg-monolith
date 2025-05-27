package com.test.basic.lol.matches;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
// 이렇게 실행된 쿼리는 Hibernate를 거치지 않고, 스프링의 DataSource를 통해 직접 실행되기 때문에
// Hibernate SQL 로그에 출력 안됨
@Sql(scripts = {"/db/h2/schema.sql", "/db/h2/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testGetMatchesByLeagueIdAndDate() {
        String leagueId = "98767991310872058";  // LCK
        LocalDate today = LocalDate.now();  // 주의: 이거 값 생성 후 밤12시 지나면 테스트 통과 못할 수 있음
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Match> matches = matchRepository.findMatchByLeagueIdAndDate(leagueId, start, end);

        // 금일 진행 경기 유무에 따라 분기조치
        if (matches.isEmpty()) {
            assertThat(matches.size()).isEqualTo(0);
        } else {
            assertThat(matches.size()).isGreaterThan(0);
            assertThat(matches.get(0).getLeague().getLeagueId()).isEqualTo(leagueId);
            assertThat(matches.get(0).getStartTime()).isBetween(start, end);
        }
    }


}
