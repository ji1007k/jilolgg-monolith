package com.test.basic.lol.api;

import com.test.basic.lol.leagues.LeagueDto;
import com.test.basic.lol.leagues.LeagueResponse;
import com.test.basic.lol.matches.MatchScheduleResponse;
import com.test.basic.lol.tournaments.TournamentDto;
import com.test.basic.lol.tournaments.TournamentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

// TODO
//  - response를 DTO로 받는 것 고려하기
//  - LeagueId로 경기 일정 조회 (LCK CUP, LCK, MSI, EWC, WORLDS)
//  - 연도별 경기 일정 배치 처리 + DB 저장(2022~)

@Component
public class LolEsportsApiClient {

    private final WebClient webClient;
    private final String API_KEY;

    private static final String HL = "ko-KR";
    private static final String LEAGUE_ID = "98767991310872058"; // LCK

//    private static final String API_URL =
//            "/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019";

    @Autowired
    public LolEsportsApiClient(
            WebClient.Builder webClientBuilder,
            LolEsportsApiConfig apiConfig) {

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
        this.API_KEY = apiConfig.getKey();
    }

    public Mono<List<LeagueDto>> fetchLeagues() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getLeagues")
                        .queryParam("hl", HL)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(LeagueResponse.class)
                .map(response -> response.getData().getLeagues());
    }

    public Mono<List<TournamentDto>> fetchTournamentsByLeagueId(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getTournamentsForLeague")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(TournamentResponse.class)
                .map(response -> response
                        .getData().getLeagues().get(0)
                        .getTournaments());
    }


    public Mono<MatchScheduleResponse> fetchMatchSchedule(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/persisted/gw/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(MatchScheduleResponse.class);
    }

    public Mono<String> fetchScheduleMatchesJson() {
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

    public Mono<String> fetchScheduleByLeagueIdAndPageToken(String leagueId, String finalToken) {

        Mono<String> response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/persisted/gw/getSchedule");
                    uriBuilder.queryParam("hl", HL);
                    uriBuilder.queryParam("leagueId", leagueId);
                    if (finalToken != null) {
                        uriBuilder.queryParam("pageToken", finalToken);
                    }
                    return uriBuilder.build();
                })
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(String.class);

        return response;
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

    public Mono<String> fetchTournamentsJson() {
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
