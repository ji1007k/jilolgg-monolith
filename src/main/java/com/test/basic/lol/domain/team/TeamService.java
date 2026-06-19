package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import com.test.basic.lol.domain.player.Player;
import com.test.basic.lol.domain.player.PlayerRepository;
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

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final ObjectMapper objectMapper;
    private final LeagueRepository leagueRepository;
    private final MatchTeamService matchTeamService;
    private final PlayerRepository playerRepository;


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
        if (isEmptyCondition(leagueId, slugs)) {
            return teamRepository.findTeamsWithMatches();
        }

        if (isInternationalLeague(leagueId)) {
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

    public List<TeamSyncDto> parseTeamsFromResponse(String response) {
        try {
            JsonNode teamsNode = objectMapper.readTree(response)
                    .path("data").path("teams");

            List<TeamSyncDto> teams = new ArrayList<>();

            for (JsonNode teamNode : teamsNode) {
                TeamSyncDto dto = new TeamSyncDto(
                        teamNode.path("id").asText(),
                        teamNode.path("code").asText(),
                        teamNode.path("name").asText(),
                        teamNode.path("slug").asText(),
                        teamNode.path("image").asText(),
                        teamNode.path("homeLeague").path("name").asText()
                );

                List<TeamSyncDto.PlayerSyncDto> playerDtos = new ArrayList<>();
                for (JsonNode player : teamNode.path("roster")) {
                    playerDtos.add(new TeamSyncDto.PlayerSyncDto(
                            player.path("id").asText(),
                            player.path("fullName").asText(),
                            player.path("role").asText(),
                            player.path("image").asText()
                    ));
                }
                dto.setPlayers(playerDtos);
                teams.add(dto);
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
        if (dto.getHomeLeague() == null || dto.getHomeLeague().isEmpty())
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

        Team savedTeam = teamRepository.save(team);

        // Sync Players
        if (dto.getPlayers() != null) {
            for (TeamSyncDto.PlayerSyncDto pDto : dto.getPlayers()) {
                Player player = playerRepository.findByPlayerId(pDto.getId())
                        .orElseGet(Player::new);
                player.setPlayerId(pDto.getId());
                player.setName(pDto.getName());
                player.setRole(pDto.getRole());
                player.setImage(pDto.getImage());
                player.setTeam(savedTeam);
                playerRepository.save(player);
            }
        }
    }
}
