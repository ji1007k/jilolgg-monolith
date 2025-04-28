package com.test.basic.lol.api;

import com.test.basic.lol.matches.MatchDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LolEsportsApiClientIntergrationTest {

    @Autowired
    private LolEsportsApiClient lolEsportsApiClient;

    @Test
    void testFetchScheduleMatchesIntegration() {
        // 실제로 HTTP 요청 보내는 테스트 (실서버가 아니면 WireMock 등으로 가짜 서버 띄워야 안전)
        Mono<String> result = lolEsportsApiClient.fetchScheduleMatches();

        String jsonResponse = result.block();

        assertThat(jsonResponse).isNotNull();

        List<MatchDto> matches = lolEsportsApiClient.parseMatchesFromResponse(jsonResponse);

        assertThat(matches).isNotNull();
        assertThat(matches.size()).isGreaterThan(0);
    }
}
