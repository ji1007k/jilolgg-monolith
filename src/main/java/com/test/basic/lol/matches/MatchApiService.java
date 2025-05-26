package com.test.basic.lol.matches;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.api.dto.matches.MatchDetailResponse;
import com.test.basic.lol.api.dto.matches.MatchScheduleResponse;
import com.test.basic.lol.matchteams.MatchTeamDto;
import com.test.basic.lol.teams.TeamDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MatchApiService {
    private final LolEsportsApiClient apiClient;
    private final ObjectMapper objectMapper;


    public List<MatchDto> fetchAllMatches() {
        String json = apiClient.fetchScheduleMatchesJson().block();
        return parseMatchesFromJson(json, null);
    }

    public List<MatchDto> fetchMatchesByLeague(String leagueId) {
        String json = apiClient.fetchScheduleMatchesJson().block();
        return parseMatchesFromJson(json, leagueId);
    }

    public List<MatchDto> parseMatchesFromJson(String response, String leagueId) {
        List<MatchDto> result = new ArrayList<>();

        try {
            JsonNode events = objectMapper.readTree(response)
                    .path("data").path("schedule").path("events");

            for (JsonNode event : events) {
                List<MatchTeamDto> matchTeamDtos = StreamSupport.stream(
                                event.path("match").path("teams").spliterator(), false)
                        .map(team -> new MatchTeamDto(
                                team.path("result").path("outcome").asText(),
                                team.path("result").path("gameWins").asInt(),
                                new TeamDto(team.path("code").asText(),
                                        team.path("name").asText())
                        ))
                        .toList();

                boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
                String winningTeamCode = completed
                        ? matchTeamDtos.stream()
                        .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                        .map(matchTeamDto -> matchTeamDto.getTeam().getCode())
                        .findFirst().orElse(null)
                        : null;

                result.add(new MatchDto(
                        event.path("match").path("id").asText(),
                        event.path("startTime").asText(),
                        event.path("state").asText(),
                        event.path("strategy").path("type").asText(),
                        winningTeamCode,
                        matchTeamDtos.stream()
                                .map(team -> {
                                    team.getTeam().setLeagueId(leagueId);
                                    return team;
                                })
                                .toList()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse schedule data", e);
        }

        return result;
    }

    public MatchScheduleResponse fetchScheduleByLeagueIdAndPageToken(String leagueId, String pageToken) {
        return apiClient
                .fetchScheduleByLeagueIdAndPageToken(leagueId, pageToken)
                .block();
    }

    public Mono<MatchDetailResponse> fetchMatchDetailFromApi(String matchId) {
        return apiClient.fetchMatchDetailFromApi(matchId);
    }

}
