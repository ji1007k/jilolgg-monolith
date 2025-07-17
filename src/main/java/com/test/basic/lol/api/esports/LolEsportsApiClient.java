package com.test.basic.lol.api.esports;

import com.test.basic.lol.api.esports.dto.MatchDetailResponse;
import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.api.esports.dto.StandingsResponse;
import com.test.basic.lol.domain.league.LeagueDto;
import com.test.basic.lol.domain.league.LeagueResponse;
import com.test.basic.lol.domain.tournament.TournamentDto;
import com.test.basic.lol.domain.tournament.TournamentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;


@Component
@Slf4j
public class LolEsportsApiClient {

    private final WebClient webClient;

    private static final String HL = "ko-KR";
//    LCK 리그ID: 98767991310872058
//    https://esports-api.lolesports.com/persisted/gw/getSchedule?hl=ko-KR&leagueId=98767991302996019

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
                .baseUrl(apiConfig.getUrl() + "/persisted/gw")
                .defaultHeader("x-api-key", apiConfig.getKey())
                .exchangeStrategies(strategies)
                .build();
    }

    public Mono<List<LeagueDto>> fetchLeagues() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getLeagues")
                        .queryParam("hl", HL)
                        .build())
//                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToMono(LeagueResponse.class)
                .map(response -> response.getData().getLeagues());
    }

    public Mono<List<TournamentDto>> fetchTournamentsByLeagueId(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getTournamentsForLeague")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .retrieve()
                .bodyToMono(TournamentResponse.class)
                .map(response -> response
                        .getData().getLeagues().get(0)
                        .getTournaments());
    }

    public Mono<String> fetchAllMatchesJson() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getSchedule")
                        .queryParam("hl", HL)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fetchScheduleMatchesJson(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    /*public Mono<MatchScheduleResponse> fetchMatchSchedule(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getSchedule")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .retrieve()
                .bodyToMono(MatchScheduleResponse.class);
    }*/

    public Mono<MatchScheduleResponse> fetchScheduleByLeagueIdAndPageToken(String leagueId, String finalToken) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/getSchedule");
                    uriBuilder.queryParam("hl", HL);
                    uriBuilder.queryParam("leagueId", leagueId);
                    uriBuilder.queryParam("pageToken", finalToken);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(MatchScheduleResponse.class)
                .timeout(Duration.ofSeconds(30));
    }

    /*public Mono<String> fetchScheduleJsonByLeagueIdAndPageToken(String leagueId, String finalToken) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/getSchedule");
                    uriBuilder.queryParam("hl", HL);
                    uriBuilder.queryParam("leagueId", leagueId);
                    uriBuilder.queryParam("pageToken", finalToken);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class);
    }
*/

    public Mono<String> fetchAllTeams() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getTeams")
                        .queryParam("hl", HL)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fetchTeamBySlug(String slug) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/getTeams")
                    .queryParam("hl", HL)
                    .queryParam("id", slug)
                    .build())
            .retrieve()
            .bodyToMono(String.class);
    }

    public Mono<String> fetchTournamentsJson(String leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getTournamentsForLeague")
                        .queryParam("hl", HL)
                        .queryParam("leagueId", leagueId)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    // 현재 날짜 기준으로 진행중인 토너먼트 있으면 해당 토너먼트 id 순위 조회
    public Mono<String> fetchStandingsJson(String tournamentId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getStandings")
                        .queryParam("hl", HL)
                        .queryParam("tournamentId", tournamentId)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<StandingsResponse> fetchStandings(String tournamentId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getStandings")
                        .queryParam("hl", HL)
                        .queryParam("tournamentId", tournamentId)
                        .build())
                .retrieve()
                .bodyToMono(StandingsResponse.class);
    }

    public Mono<MatchDetailResponse> fetchMatchDetailFromApi(String matchId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getEventDetails")
                        .queryParam("hl", HL)
                        .queryParam("id", matchId)
                        .build())
                .retrieve()
                .bodyToMono(MatchDetailResponse.class);
    }
}
