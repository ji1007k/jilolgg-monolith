package com.test.basic.lol.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.comp.CompDto;
import com.test.basic.lol.comp.TeamMatchResult;
import com.test.basic.lol.teams.Team;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class LolEsportsApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String esportsApiKey;

    private static final String HL = "ko-KR";
    private static final String LEAGUE_ID = "98767991310872058"; // LCK

//    private static final String API_URL =
//            "/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019";

    // *** @Value로 주입되는 시점은 생성자 호출 이후임!
    @Autowired
    public LolEsportsApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${lol.esports.api.url}") String apiBaseUrl,
            @Value("${lol.esports.api.key}") String esportsApiKey
    ) {

        // WebClient가 받아들이는 응답 크기 제한
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 최대 2MB로 설정 (필요하면 더)
                .build();

        this.webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .exchangeStrategies(strategies)
                .build();
        this.objectMapper = new ObjectMapper();
        this.esportsApiKey = esportsApiKey;
    }

    public List<CompDto> fetchScheduleComps() {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", LEAGUE_ID)
                        .build())
                .header("x-api-key", esportsApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseCompsFromResponse(response);
    }

    private List<CompDto> parseCompsFromResponse(String response) {
        List<CompDto> result = new ArrayList<>();

        try {
            JsonNode events = objectMapper.readTree(response)
                    .path("data").path("schedule").path("events");

            for (JsonNode event : events) {
                List<TeamMatchResult> matchResult = StreamSupport.stream(
                                event.path("match").path("teams").spliterator(), false)
                        .map(team -> new TeamMatchResult(
                                team.path("code").asText(),
                                team.path("name").asText(),
                                team.path("result").path("outcome").asText()
                        ))
                        .collect(Collectors.toList());

                boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
                String winningTeamCode = completed
                        ? matchResult.stream()
                        .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                        .map(TeamMatchResult::getCode)
                        .findFirst().orElse(null)
                        : null;

                result.add(new CompDto(
                        event.path("startTime").asText(),
                        event.path("state").asText(),
                        winningTeamCode,
                        matchResult.stream()
                                .map(team -> new Team(team.getCode(), team.getName(), null, null, LEAGUE_ID))
                                .toList()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse schedule data", e);
        }

        return result;
    }

    public List<Team> fetchAllTeams() {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getTeams")
                        .queryParam("hl", HL)
                        .build())
                .header("x-api-key", esportsApiKey)
                .retrieve()
                .bodyToMono(String.class)   // 전체 응답을 스트리밍 방식으로 처리
                .block();

        return parseTeamsFromResponse(response);
    }

    public Team fetchTeamBySlug(String slug) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getTeams")
                        .queryParam("hl", HL)
                        .queryParam("id", slug)
                        .build())
                .header("x-api-key", esportsApiKey)
                .retrieve()
                .bodyToMono(String.class)   // 전체 응답을 스트리밍 방식으로 처리
                .block();

        // FIXME API의 팀 정보 전체 속성 가진 DTO 클래스로 관리 (선수 정보 포함)
        List<Team> teams = parseTeamsFromResponse(response);

        if (teams == null || teams.isEmpty()) {
            throw new EntityNotFoundException("Team not found with slug: " + slug);
        }

        return teams.get(0);
    }

    private List<Team> parseTeamsFromResponse(String response) {
        try {
            JsonNode teamsNode = objectMapper.readTree(response)
                    .path("data").path("teams");

            List<Team> teams = new ArrayList<>();

            for (JsonNode team : teamsNode) {
                teams.add(new Team(
                        team.path("code").asText(),
                        team.path("name").asText(),
                        team.path("slug").asText(),
                        team.path("image").asText(),
                        team.path("homeLeague").path("name").asText()
                ));
            }

            return teams;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse team data", e);
        }
    }

}
