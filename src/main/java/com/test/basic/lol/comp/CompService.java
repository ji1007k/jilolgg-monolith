package com.test.basic.lol.comp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CompService {

    // *** @Value로 주입되는 시점은 생성자 호출 이후임!
    @Value("${lol.esports.api.key}")
    private String esportsApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HL = "ko-KR";
    private static final String LEAGUE_ID = "98767991310872058";    // LCK ID


//    private static final String API_URL =
//            "/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019";

    public CompService(WebClient.Builder webClientBuilder,
                       @Value("${lol.esports.api.url}") String esportsApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(esportsApiBaseUrl).build();
    }

    public List<CompDto> getComps(String teamCode) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", LEAGUE_ID)
                        .build())
                .header("x-api-key", esportsApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 여기서 block()으로 동기화 → 필요시 나중에 비동기로 변경 가능

        List<CompDto> result = new ArrayList<>();

        try {
            JsonNode events = objectMapper.readTree(response)
                    .path("data")
                    .path("schedule")
                    .path("events");

            for (JsonNode event : events) {
                List<String> teamNames = StreamSupport.stream(
                                event.path("match").path("teams").spliterator(), false)
                        .map(team -> team.path("code").asText())
                        .collect(Collectors.toList());

                if (teamNames.stream().anyMatch(name -> name.equalsIgnoreCase(teamCode))) {
                    result.add(new CompDto(
                            event.path("startTime").asText(),
                            event.path("state").asText(),
                            teamNames
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
