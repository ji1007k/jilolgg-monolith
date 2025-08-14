package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TODO 외부 API 데이터 바로 가져오는 로직 분리
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
//    private final LolEsportsApiClient lolEsportsApiClient;
    private final TeamMapper teamMapper;
    private final ObjectMapper objectMapper;
    private final LeagueRepository leagueRepository;
    private final MatchTeamService matchTeamService;


    @Cacheable("teams")
    public List<Team> getAllTeamsFromDB() {
        return teamRepository.findAll();
    }

    @Cacheable(value = "teams", key = "#leagueId + '_' + #slugs")
    public List<TeamDto> getTeamsFromDB(String leagueId, List<String> slugs) {
        List<Team> teams = getTeamsByCondition(leagueId, slugs);
        return teams.stream().map(teamMapper::teamToTeamDto).toList();
    }

    public List<Team> getTeamsByCondition(String leagueId, List<String> slugs) {
        // 전체 조회 (필터x)
        if (isEmptyCondition(leagueId, slugs)) {
            return teamRepository.findTeamsWithMatches();
        }

        // 국제 대회 처리
        if (isInternationalLeague(leagueId)) {
            // 국제 대회 경기 일정이 있는 팀 목록 조회
            List<String> teamIds = matchTeamService.findTeamIdsByLeagueId(leagueId);
            return teamRepository.findByTeamIdIn(teamIds);
        }

        return teamRepository.findTeamsWithMatchesFiltered(leagueId, slugs);
    }

    private boolean isEmptyCondition(String leagueId, List<String> slugs) {
        return (leagueId == null || leagueId.isBlank()) &&
                (slugs == null || slugs.isEmpty());
    }

    private boolean isInternationalLeague(String leagueId) {
        if (leagueId == null || leagueId.isBlank()) return false;

        return leagueRepository.findByLeagueId(leagueId)
                .map(league -> "국제 대회".equals(league.getRegion()))
                .orElse(false);
    }

    public Team getTeamBySlugFromDB(String slug) {
        return teamRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + slug));
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

    public List<Team> getTeamsByName(Set<String> teamNames) {
        return teamRepository.findByNameIn(teamNames);
    }

    @Cacheable(value = "teams", key = "#leagueId")
    public List<TeamDto> getTeamsByLeagueId(String leagueId) {
        return teamRepository.findByLeague_LeagueId(leagueId)
                .stream()
                .map(teamMapper::teamToTeamDto)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOrUpdate(TeamSyncDto dto) {
        if (dto.getHomeLeague().isEmpty())
            throw new RuntimeException("League is Empty");

        League league = leagueRepository.findByName(dto.getHomeLeague())
                .orElseThrow(() -> new RuntimeException("League not found: " + dto.getHomeLeague()));

        Optional<Team> existing = teamRepository.findBySlug(dto.getSlug());
        Team team = existing.orElseGet(Team::new);

        team.setTeamId(dto.getTeamId());
        team.setCode(dto.getCode());
        team.setName(dto.getName());
        team.setSlug(dto.getSlug());
        team.setImage(dto.getImage());
        team.setLeague(league);

        teamRepository.save(team);
    }


    // TODO 삭제 또는 리팩토링 =======================================================

   /* public List<Team> getTeamsByCode(Set<String> duplicateCodes) {
        return teamRepository.findByCodeIn(duplicateCodes);
    }

    public Team getTeamByName(String teamName) {
        return teamRepository.findByName("TBD").orElse(null);
    }*/

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
}
