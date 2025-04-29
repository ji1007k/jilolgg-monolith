package com.test.basic.lol.teams;

import com.test.basic.lol.api.LolEsportsApiClient;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final LolEsportsApiClient lolEsportsApiClient;
    
    // LCK 1군 팀 코드
    private final List<String> lckFirstTeamCodes = List.of(
            "T1", "GEN", "HLE", "DK", "DRX", "KT", "BRO",
            "NS", "BFX", "DNF"
    );

    public TeamService(TeamRepository teamRepository, LolEsportsApiClient lolEsportsApiClient) {
        this.teamRepository = teamRepository;
        this.lolEsportsApiClient = lolEsportsApiClient;
    }

    public List<Team> getAllTeamsFromDB() {
        return teamRepository.findAll();
    }

    public List<Team> getTeamsFromDB(String homeLeague, List<String> slugs) {
        // 둘 다 null 또는 비어있으면 전체 조회
        if ((homeLeague == null || homeLeague.isBlank()) && (slugs == null || slugs.isEmpty())) {
            return getAllTeamsFromDB();
        }

        // homeLeague만 있을 경우
        if (slugs == null || slugs.isEmpty()) {
            return teamRepository.findByHomeLeague(homeLeague);
        }

        // slugs만 있을 경우
        if (homeLeague == null || homeLeague.isBlank()) {
            return teamRepository.findBySlugIn(slugs);
        }

        // 둘 다 있을 경우
        return teamRepository.findByHomeLeagueAndSlugIn(homeLeague, slugs);
    }


    public Team getTeamBySlugFromDB(String slug) {
        return teamRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + slug));
    }

    // FIXME 1군 구분할 수 있는 컬럼 있는지 확인 후 해당 컬럼 사용해서 필터링
    public List<Team> filterLCKFirstTeams(List<Team> teams) {
        return teams.stream()
                .filter(team -> lckFirstTeamCodes.contains(team.getTeamCode()))
                .collect(Collectors.toList());
    }

    // FIXME API 응답 데이터 DTO 따로 생성
    public Team getTeamBySlugFromExternalApi(String slug) {
        Mono<String> result = lolEsportsApiClient.fetchTeamBySlug(slug);
        List<Team> teams = lolEsportsApiClient.parseTeamsFromResponse(result.block());

        if (teams == null || teams.isEmpty()) {
            throw new EntityNotFoundException("Team not found with slug: " + slug);
        }

        return teams.get(0);
    }

    public List<Team> getAllTeamsFromExternalApi() {
        Mono<String> result = lolEsportsApiClient.fetchAllTeams();
        return lolEsportsApiClient.parseTeamsFromResponse(result.block());
    }

}
