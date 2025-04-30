package com.test.basic.lol.standings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.teams.Team;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class StandingsService {

    private final LolEsportsApiClient lolEsportsApiClient;
    private final ObjectMapper objectMapper;

    public StandingsService(LolEsportsApiClient lolEsportsApiClient,
                            ObjectMapper objectMapper) {
        this.lolEsportsApiClient = lolEsportsApiClient;;
        this.objectMapper = objectMapper;
    }

    public List<StandingsDto> getStandingsByTournamentId(String tournamentId) {
        Mono<String> result = lolEsportsApiClient.fetchStandings(tournamentId);

        try {
            JsonNode standingsNodes = objectMapper.readTree(result.block())
                    .path("data").path("standings");

            List<StandingsDto> standingsList = new ArrayList<>();

            for (JsonNode standings : standingsNodes) {
                JsonNode stagesArray = standings.path("stages");

                for (JsonNode stage : stagesArray) {
                    StandingsDto dto = new StandingsDto();
                    dto.setTournamentId(tournamentId);
                    dto.setId(stage.path("id").asText());
                    dto.setName(stage.path("name").asText());
                    dto.setSlug(stage.path("slug").asText());
                    dto.setType(stage.path("type").asText(null)); // null safe

                    // rankings: stage.sections[0].rankings
                    JsonNode sections = stage.path("sections");
                    if (sections.isArray() && sections.size() > 0) {
                        List<Team> teamList = new ArrayList<>();

                        JsonNode rankings = sections.get(0).path("rankings");
                        for (JsonNode rankingNode : rankings) {

                            // 기본 필드들 파싱
                            String rank = rankingNode.path("ordinal").asText();

                            JsonNode teamNodes = rankingNode.path("teams");
                            for (JsonNode teamNode : teamNodes) {   // 공동 순위
                                Team team = new Team();

                                team.setRank(rank);
                                team.setTeamId(teamNode.path("id").asText());
                                team.setSlug(teamNode.path("slug").asText());
                                team.setTeamName(teamNode.path("name").asText());
                                team.setTeamCode(teamNode.path("code").asText());
                                team.setImage(teamNode.path("image").asText());

                                // record 파싱
                                JsonNode recordNode = teamNode.path("record");
                                int wins = recordNode.path("wins").asInt();
                                int losses = recordNode.path("losses").asInt();
                                team.setRecord(wins + "," + losses);

                                teamList.add(team);
                            }
                        }

                        dto.setRankings(teamList);
                    }

                    standingsList.add(dto);
                }
            }

            return standingsList;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse data", e);
        }
    }

}
