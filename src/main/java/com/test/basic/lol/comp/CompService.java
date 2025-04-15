package com.test.basic.lol.comp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


// TODO
//  - 리그 ID 별 조회 기능 추가

@Service
public class CompService {

    // *** @Value로 주입되는 시점은 생성자 호출 이후임!
    @Value("${lol.esports.api.key}")
    private String esportsApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HL = "ko-KR";
    private static final String LEAGUE_ID = "98767991310872058";    // LCK ID

    private List<CompDto> allComps = null;
    private Instant lastFetchedTime = null;
    private static final Duration TTL = Duration.ofMinutes(10);

//    private static final String API_URL =
//            "/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019";

    public CompService(WebClient.Builder webClientBuilder,
                       @Value("${lol.esports.api.url}") String esportsApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(esportsApiBaseUrl).build();
    }

    public List<CompDto> getAllComps() {
        if (allComps != null && lastFetchedTime != null
            && Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            return allComps;    // 아직 TTL 안 지났으면 캐시 데이터 사용
        }

        // TTL 지났거나 최초 요청이면 새로 로딩
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

        List<CompDto> result = new ArrayList<>();

        try {
            JsonNode events = objectMapper.readTree(response)
                    .path("data")
                    .path("schedule")
                    .path("events");

            for (JsonNode event : events) {
                List<TeamInfo> teamInfos = StreamSupport.stream(
                                event.path("match").path("teams").spliterator(), false)
                        .map(team -> new TeamInfo(
                                team.path("code").asText(),
                                team.path("result").path("outcome").asText()
                        ))
                        .collect(Collectors.toList());

                boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
                String winningTeamCode = completed
                        ? teamInfos.stream()
                        .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                        .map(TeamInfo::getCode)
                        .findFirst()
                        .orElse("Unknown")
                        : null;

                result.add(new CompDto(
                        event.path("startTime").asText(),
                        event.path("state").asText(),
                        winningTeamCode,
                        teamInfos.stream().map(TeamInfo::getCode).collect(Collectors.toList())
                ));
            }

            allComps = result;
            lastFetchedTime = Instant.now();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return allComps;
    }

    public List<CompDto> getComps(String teamCode) {
        return getAllComps().stream()
                .filter(dto -> dto.getTeams().stream()
                        .anyMatch(code -> code.equalsIgnoreCase(teamCode)))
                .collect(Collectors.toList());
    }

}
