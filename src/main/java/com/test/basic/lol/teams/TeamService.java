package com.test.basic.lol.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.leagues.LeagueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final LolEsportsApiClient lolEsportsApiClient;
    private final TeamMapper teamMapper;
    private final ObjectMapper objectMapper;

    // LCK 1군 팀 코드 (활동중)
    private final List<String> lckFirstTeamCodes = List.of(
            "T1", "GEN", "HLE", "DK", "DRX", "KT", "BRO",
            "NS", "BFX", "DNF"
    );

    public TeamService(TeamRepository teamRepository, LolEsportsApiClient lolEsportsApiClient, TeamMapper teamMapper, ObjectMapper objectMapper) {
        this.teamRepository = teamRepository;
        this.lolEsportsApiClient = lolEsportsApiClient;
        this.teamMapper = teamMapper;
        this.objectMapper = objectMapper;
    }

    public List<Team> getAllTeamsFromDB() {
        return teamRepository.findAll();
    }

    public List<TeamDto> getTeamsFromDB(String leagueId, List<String> slugs) {
        List<Team> teams;

        // 둘 다 null 또는 비어있으면 전체 조회
        if ((leagueId == null || leagueId.isBlank()) && (slugs == null || slugs.isEmpty())) {
            teams = getAllTeamsFromDB();
        } else if (slugs == null || slugs.isEmpty()) {
            // leagueId만 있을 경우
            teams = teamRepository.findByLeague_LeagueId(leagueId);
        } else if (leagueId == null || leagueId.isBlank()) {
            // slugs만 있을 경우
            teams = teamRepository.findBySlugIn(slugs);
        } else {
            // 둘 다 있을 경우
            teams = teamRepository.findByLeague_LeagueIdAndSlugIn(leagueId, slugs);
        }

        return teams.stream().map(teamMapper::teamToTeamDto).toList();
    }


    public Team getTeamBySlugFromDB(String slug) {
        return teamRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + slug));
    }

    public List<TeamDto> filterLCKFirstTeams(List<TeamDto> teams) {
        return teams.stream()
                .filter(team -> lckFirstTeamCodes.contains(team.getCode()))
                .collect(Collectors.toList());
    }

    // FIXME API 응답 데이터 DTO 따로 생성
    public TeamSyncDto getTeamBySlugFromExternalApi(String slug) {
        Mono<String> result = lolEsportsApiClient.fetchTeamBySlug(slug);
        List<TeamSyncDto> teams = parseTeamsFromResponse(result.block());

        if (teams == null || teams.isEmpty()) {
            throw new EntityNotFoundException("Team not found with slug: " + slug);
        }

        return teams.get(0);
    }

    public List<TeamSyncDto> getAllTeamsFromExternalApi() {
        Mono<String> result = lolEsportsApiClient.fetchAllTeams();
        return parseTeamsFromResponse(result.block());
    }

    // FIXME API의 팀 정보 전체 속성 가진 DTO 클래스로 관리 (선수 정보 포함)
    public List<TeamSyncDto> parseTeamsFromResponse(String response) {
        try {
            JsonNode teamsNode = objectMapper.readTree(response)
                    .path("data").path("teams");

            List<TeamSyncDto> teams = new ArrayList<>();

            for (JsonNode team : teamsNode) {
                teams.add(new TeamSyncDto(
                        team.path("id").asText(),
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
