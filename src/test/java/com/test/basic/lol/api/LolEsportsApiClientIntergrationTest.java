package com.test.basic.lol.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.esports.LolEsportsApiClient;
import com.test.basic.lol.domain.tournament.TournamentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LolEsportsApiClientIntergrationTest {

    @Autowired
    private LolEsportsApiClient lolEsportsApiClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String tournamentId;

    /*@Test
    void testFetchScheduleMatchesIntegration() {
        // 실제로 HTTP 요청 보내는 테스트 (실서버가 아니면 WireMock 등으로 가짜 서버 띄워야 안전)
        Mono<String> result = lolEsportsApiClient.fetchScheduleMatchesJson("98767991310872058");

        String jsonResponse = result.block();

        assertThat(jsonResponse).isNotNull();

        List<MatchDto> matches = lolEsportsApiClient.parseMatchesFromResponse(jsonResponse);

        assertThat(matches).isNotNull();
        assertThat(matches.size()).isGreaterThan(0);
    }*/

    @Test
    void testFetchTournaments() throws JsonProcessingException {
        Mono<String> result = lolEsportsApiClient.fetchTournamentsJson("98767991310872058");
        String jsonResponse = result.block();
        assertThat(jsonResponse).isNotNull();

        JsonNode tournamentNodes = objectMapper.readTree(jsonResponse);

        assertThat(tournamentNodes).isNotNull();
        JsonNode tournaments = tournamentNodes.path("data").path("leagues").get(0).path("tournaments");
        TournamentDto tournament = objectMapper.treeToValue(tournaments.get(0), TournamentDto.class);

        assertThat(tournament).isNotNull();
        tournamentId = tournament.getTournamentId();
    }

    @Test
    void testFetchStandings() throws JsonProcessingException {
        Mono<String> result = lolEsportsApiClient.fetchStandingsJson(tournamentId);
        String jsonResponse = result.block();
        assertThat(jsonResponse).isNotNull();

        JsonNode standingsNodes = objectMapper.readTree(jsonResponse);
        assertThat(standingsNodes).isNotNull();

        if (tournamentId == null) {
            assertThat(standingsNodes.get(0)).isNull();
        } else {
            assertThat(standingsNodes.get(0)).isNotNull();
        }
    }
}
