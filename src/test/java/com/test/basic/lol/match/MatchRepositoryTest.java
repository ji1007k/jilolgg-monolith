package com.test.basic.lol.match;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// @DataJpaTest는 기본적으로 DataSource를 자체 임베디드 DB로 교체한다.
// 이 프로젝트의 테스트 H2는 MODE=PostgreSQL / DATABASE_TO_UPPER=false로 운영 DB 의미를 맞춰 둔 설정이므로
// 교체를 끄고 application.yml의 데이터소스를 그대로 사용해야 한다.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// 이렇게 실행된 쿼리는 Hibernate를 거치지 않고, 스프링의 DataSource를 통해 직접 실행되기 때문에
// Hibernate SQL 로그에 출력 안됨
@Sql(scripts = {"/db/h2/lol.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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
