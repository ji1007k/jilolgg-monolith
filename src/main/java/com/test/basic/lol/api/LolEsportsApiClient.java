package com.test.basic.lol.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.matches.MatchDto;
import com.test.basic.lol.matches.TeamMatchResult;
import com.test.basic.lol.teams.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// TODO
//  - response를 DTO로 받는 것 고려
//  - LeagueId 조건 추가
//  - 연도별 경기 일정 배치 처리 + DB 저장(2022~)

@Component
public class LolEsportsApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String API_KEY;

    private static final String HL = "ko-KR";
    private static final String LEAGUE_ID = "98767991310872058"; // LCK

//    private static final String API_URL =
//            "/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019";

    // *** @Value로 주입되는 시점은 생성자 호출 이후임!
    @Autowired
    public LolEsportsApiClient(
            WebClient.Builder webClientBuilder,
            LolEsportsApiConfig apiConfig
    ) {

        // WebClient가 받아들이는 응답 크기 제한
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 최대 2MB로 설정 (필요하면 더)
                .build();

        this.webClient = webClientBuilder
                .baseUrl(apiConfig.getUrl())
                .exchangeStrategies(strategies)
                .build();
        this.objectMapper = new ObjectMapper();
        this.API_KEY = apiConfig.getKey();
    }

    public Mono<String> fetchScheduleMatches() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", LEAGUE_ID)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

    public List<MatchDto> parseMatchesFromResponse(String response) {
        List<MatchDto> result = new ArrayList<>();

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
                        .toList();

                boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
                String winningTeamCode = completed
                        ? matchResult.stream()
                        .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                        .map(TeamMatchResult::getCode)
                        .findFirst().orElse(null)
                        : null;

                result.add(new MatchDto(
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

    public List<MatchDto> fetchScheduleByYear(String year) {
        List<MatchDto> allMatches = new ArrayList<>();
        String nextPageToken = null;

        do {
            String finalToken = nextPageToken;
            Mono<String> response = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/persisted/gw/getSchedule");
                        uriBuilder.queryParam("hl", HL);
                        uriBuilder.queryParam("leagueId", LEAGUE_ID);
                        if (finalToken != null) {
                            uriBuilder.queryParam("pageToken", finalToken);
                        }
                        return uriBuilder.build();
                    })
                    .header("x-api-key", API_KEY)
                    .retrieve()
                    .bodyToMono(String.class);

            if (response == null) break;

            try {
                JsonNode root = objectMapper.readTree(response.block());
                JsonNode schedule = root.path("data").path("schedule");
                JsonNode events = schedule.path("events");

                List<MatchDto> pageMatches = parseMatchesFromEvents(events, year);
                allMatches.addAll(pageMatches);

                // 중단 조건: 더 이상 해당 연도의 이벤트가 없음
                boolean allBeforeTargetYear = StreamSupport.stream(events.spliterator(), false)
                        .allMatch(event -> !event.path("startTime").asText().startsWith(year));
                if (allBeforeTargetYear) break;

                JsonNode pages = schedule.path("pages");
                nextPageToken = pages.path("older").asText(null);

            } catch (Exception e) {
                throw new RuntimeException("Failed to parse response", e);
            }

        } while (nextPageToken != null);

        return allMatches;
    }

    public List<MatchDto> parseMatchesFromEvents(JsonNode events, String year) {
        List<MatchDto> result = new ArrayList<>();

        for (JsonNode event : events) {
            String startTime = event.path("startTime").asText();
            if (!startTime.startsWith(year)) continue;

            List<TeamMatchResult> matchResult = StreamSupport.stream(
                            event.path("match").path("teams").spliterator(), false)
                    .map(team -> new TeamMatchResult(
                            team.path("code").asText(),
                            team.path("name").asText(),
                            team.path("result").path("outcome").asText()
                    ))
                    .toList();

            boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
            String winningTeamCode = completed
                    ? matchResult.stream()
                    .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                    .map(TeamMatchResult::getCode)
                    .findFirst().orElse(null)
                    : null;

            result.add(new MatchDto(
                    startTime,
                    event.path("state").asText(),
                    winningTeamCode,
                    matchResult.stream()
                            .map(team -> new Team(team.getCode(), team.getName(), null, null, LEAGUE_ID))
                            .toList()
            ));
        }

        return result;
    }

    public Mono<String> fetchAllTeams() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getTeams")
                        .queryParam("hl", HL)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fetchTeamBySlug(String slug) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/persisted/gw/getTeams")
                    .queryParam("hl", HL)
                    .queryParam("id", slug)
                    .build())
            .header("x-api-key", API_KEY)
            .retrieve()
            .bodyToMono(String.class);
    }

    // FIXME API의 팀 정보 전체 속성 가진 DTO 클래스로 관리 (선수 정보 포함)
    public List<Team> parseTeamsFromResponse(String response) {
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

    public Mono<String> fetchTournaments() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getTournamentsForLeague")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", LEAGUE_ID)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

    // 현재 날짜 기준으로 진행중인 토너먼트 있으면 해당 토너먼트 id 순위 조회
    public Mono<String> fetchStandings(String tournamentId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getStandings")
                        .queryParam("hl", HL)
                        .queryParam("tournamentId", tournamentId)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

}
