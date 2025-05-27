package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.esports.LolEsportsApiClient;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// TODO 외부 API 데이터 바로 가져오는 로직 분리
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final LolEsportsApiClient lolEsportsApiClient;
    private final TeamMapper teamMapper;
    private final ObjectMapper objectMapper;
    private final LeagueRepository leagueRepository;
    private final MatchTeamService matchTeamService;


    public TeamService(TeamRepository teamRepository, LolEsportsApiClient lolEsportsApiClient, TeamMapper teamMapper, ObjectMapper objectMapper, LeagueRepository leagueRepository, MatchTeamService matchTeamService) {
        this.teamRepository = teamRepository;
        this.lolEsportsApiClient = lolEsportsApiClient;
        this.teamMapper = teamMapper;
        this.objectMapper = objectMapper;
        this.leagueRepository = leagueRepository;
        this.matchTeamService = matchTeamService;
    }

    public List<Team> getAllTeamsFromDB() {
        return teamRepository.findAll();
    }

    public List<TeamDto> getTeamsFromDB(String leagueId, List<String> slugs) {
        List<Team> teams;

        // 둘 다 null 또는 비어있으면 전체 조회
        if ((leagueId == null || leagueId.isBlank()) && (slugs == null || slugs.isEmpty())) {
            teams = teamRepository.findTeamsWithMatches();
        } else {
            Optional<League> league = leagueRepository.findByLeagueId(leagueId);

            if (league.get().getRegion().equals("국제 대회")) {
                // 국제 대회 경기 일정이 있는 팀 목록 조회
                List<String> teamIds = matchTeamService.findTeamIdsByLeagueId(leagueId);
                teams = teamRepository.findByTeamIdIn(teamIds);
            } else {
                teams = teamRepository.findTeamsWithMatchesFiltered(leagueId, slugs);
            }
        }

        return teams.stream().map(teamMapper::teamToTeamDto).toList();
    }

    public Team getTeamBySlugFromDB(String slug) {
        return teamRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + slug));
    }

    // FIXME API 응답 데이터 DTO 따로 생성
    /*public TeamSyncDto getTeamBySlugFromExternalApi(String slug) {
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
    }*/

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
