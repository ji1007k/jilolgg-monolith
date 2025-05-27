package com.test.basic.lol.domain.standings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.esports.LolEsportsApiClient;
import com.test.basic.lol.api.esports.dto.StandingsResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StandingsService {

    private final LolEsportsApiClient lolEsportsApiClient;
    private final ObjectMapper objectMapper;

    public StandingsService(LolEsportsApiClient lolEsportsApiClient,
                            ObjectMapper objectMapper) {
        this.lolEsportsApiClient = lolEsportsApiClient;;
        this.objectMapper = objectMapper;
    }

    public Mono<StandingsResponse.StandingsData> getStandingsByTournamentIdFromApi(String tournamentId) {
        return lolEsportsApiClient
                .fetchStandings(tournamentId)
                .map(response -> {
                    response.getData().setTournamentId(tournamentId);

                    List<StandingsResponse.StandingsDto> standings = response.getData().getStandings();
                    for (StandingsResponse.StandingsDto standing : standings) {
                        List<StandingsResponse.StageDto> stages = standing.getStages();

                        for (StandingsResponse.StageDto stage : stages) {
                            List<StandingsResponse.SectionDto> sections = stage.getSections();

                            for (StandingsResponse.SectionDto section : sections) {
                                // [1] matches : 전적 조회
                                List<StandingsResponse.MatchDto> matches = section.getMatches();

                                // [1-1] 경기별 세트 스코어로 득실차 계산
                                Map<String, Integer> gameWinsMap = new HashMap<>();

                                for (StandingsResponse.MatchDto match : matches) {
                                    List<StandingsResponse.TeamDto> teams = match.getTeams();

                                    if (teams.size() == 2) {
                                        StandingsResponse.TeamDto teamA = teams.get(0);
                                        StandingsResponse.TeamDto teamB = teams.get(1);

                                        String teamAId = teamA.getTeamId();
                                        String teamBId = teamB.getTeamId();

                                        if (teamA.getResult() == null || teamB.getResult() == null)
                                            continue;

                                        int teamAWins = teamA.getResult().getGameWins();
                                        int teamBWins = teamB.getResult().getGameWins();

                                        // teamA 득실차 = 내 wins - 상대 wins
                                        int diffA = teamAWins - teamBWins;
                                        int diffB = teamBWins - teamAWins;

                                        gameWinsMap.put(teamAId, gameWinsMap.getOrDefault(teamAId, 0) + diffA);
                                        gameWinsMap.put(teamBId, gameWinsMap.getOrDefault(teamBId, 0) + diffB);
                                    }
                                }

                                // [2] rankings : 순위, 승, 패, 득실
                                // [2-1] 정렬된 팀순위 저장
                                List<StandingsResponse.RankingDto> rankings = section.getRankings();
                                section.setRefinedRankings(flattenAndSortRankings(rankings, gameWinsMap));
                            }
                        }
                    }

                    return response.getData();
                });
    }

    public List<StandingsResponse.TeamDto> flattenAndSortRankings(List<StandingsResponse.RankingDto> rankings,
                                                                  Map<String, Integer> gameWinsMap) {
        List<StandingsResponse.TeamDto> allTeams = new ArrayList<>();

        // 1. 각 랭킹의 팀들 모으고 gameDiff 세팅
        for (StandingsResponse.RankingDto ranking : rankings) {
            for (StandingsResponse.TeamDto team : ranking.getTeams()) {
                int gameDiff = gameWinsMap.getOrDefault(team.getTeamId(), 0);
                team.getRecord().setGameDiff(gameDiff);
                allTeams.add(team);
            }
        }

        // 2. 정렬 (승수 내림차순 > 패수 오름차순 > 득실차 내림차순)
        allTeams.sort((a, b) -> {
            int winCompare = Integer.compare(b.getRecord().getWins(), a.getRecord().getWins());
            if (winCompare != 0) return winCompare;

            int lossCompare = Integer.compare(a.getRecord().getLosses(), b.getRecord().getLosses());
            if (lossCompare != 0) return lossCompare;

            return Integer.compare(b.getRecord().getGameDiff(), a.getRecord().getGameDiff());
        });

        // 3. 순위 다시 매기기
        int rank = 1;
        for (int i = 0; i < allTeams.size(); i++) {
            if (i > 0) {
                StandingsResponse.RecordDto prev = allTeams.get(i - 1).getRecord();
                StandingsResponse.RecordDto curr = allTeams.get(i).getRecord();

                boolean sameRecord = prev.getWins() == curr.getWins() &&
                        prev.getLosses() == curr.getLosses() &&
                        prev.getGameDiff() == curr.getGameDiff();

                if (!sameRecord) {
                    rank = i + 1;
                }
            }
            allTeams.get(i).setRank(rank);
        }

        return allTeams;
    }


    /*public List<StandingsDto> getStandingsByTournamentIdFromApi(String tournamentId) {
        Mono<String> result = lolEsportsApiClient.fetchStandingsJson(tournamentId);

        try {
            JsonNode standingsNodes = objectMapper.readTree(result.block())
                    .path("data").path("standings");

            List<StandingsDto> standingsList = new ArrayList<>();

            for (JsonNode standings : standingsNodes) {
                JsonNode stagesArray = standings.path("stages");
                StandingsDto dto = new StandingsDto();
                dto.setId(tournamentId);

                List<StageDto> stageList = new ArrayList<>();
                for (JsonNode stage : stagesArray) {
                    StageDto stageDto = new StageDto();

                    stageDto.setId(stage.path("id").asText());
                    stageDto.setName(stage.path("name").asText());
                    stageDto.setSlug(stage.path("slug").asText());
                    stageDto.setType(stage.path("type").asText(null)); // null safe

                    // rankings: stage.sections[0].rankings
                    List<SectionDto> sectionList = new ArrayList<>();
                    JsonNode sections = stage.path("sections");
                    for (JsonNode section : sections) {
                        SectionDto sectionDto = new SectionDto();
                        sectionDto.setName(section.path("name").asText());

                        if (section.path("rankings").isEmpty()) continue;

                        List<TournamentTeamRankingDto> teamList = new ArrayList<>();

                        // 섹션별 경기 상세 정보 파싱
                        JsonNode matches = section.path("matches");
                        Map<String, Integer> gameWinsMap = new HashMap<>();

                        for (JsonNode match : matches) {
                            JsonNode teams = match.get("teams");
                            if (teams.size() == 2) {
                                JsonNode teamA = teams.get(0);
                                JsonNode teamB = teams.get(1);

                                String teamAId = teamA.get("id").asText();
                                String teamBId = teamB.get("id").asText();

                                int teamAWins = teamA.get("result").get("gameWins").asInt();
                                int teamBWins = teamB.get("result").get("gameWins").asInt();

                                // teamA 득실차 = 내 wins - 상대 wins
                                int diffA = teamAWins - teamBWins;
                                int diffB = teamBWins - teamAWins;

                                gameWinsMap.put(teamAId, gameWinsMap.getOrDefault(teamAId, 0) + diffA);
                                gameWinsMap.put(teamBId, gameWinsMap.getOrDefault(teamBId, 0) + diffB);
                            }
                        }

                        // 섹션별 순위 파싱
                        JsonNode rankings = section.path("rankings");
                        for (JsonNode rankingNode : rankings) {

                            // 기본 필드들 파싱
                            String rank = rankingNode.path("ordinal").asText();

                            JsonNode teamNodes = rankingNode.path("teams");
                            for (JsonNode teamNode : teamNodes) {   // 공동 순위
                                TournamentTeamRankingDto team = new TournamentTeamRankingDto();

                                team.setRank(rank);
                                team.setTeamId(teamNode.path("id").asText());
                                team.setSlug(teamNode.path("slug").asText());
                                team.setName(teamNode.path("name").asText());
                                team.setCode(teamNode.path("code").asText());
                                team.setImage(teamNode.path("image").asText());

                                // record 파싱
                                JsonNode recordNode = teamNode.path("record");
                                int wins = recordNode.path("wins").asInt();
                                int losses = recordNode.path("losses").asInt();
                                team.setRecord(wins + "," + losses + "," + gameWinsMap.getOrDefault(team.getTeamId(), 0));

                                teamList.add(team);
                            }
                        }

                        teamList.sort((a, b) -> {
                            String[] recordA = a.getRecord().split(",");
                            String[] recordB = b.getRecord().split(",");

                            int winsA = Integer.parseInt(recordA[0]);
                            int winsB = Integer.parseInt(recordB[0]);

                            int diffA = Integer.parseInt(recordA[2]);
                            int diffB = Integer.parseInt(recordB[2]);

                            // 승수 우선
                            int winCompare = Integer.compare(winsB, winsA);
                            if (winCompare != 0) {
                                return winCompare;
                            }

                            // 득실차 보조
                            return Integer.compare(diffB, diffA);
                        });

// rank 부여
                        int currentRank = 1;
                        for (int i = 0; i < teamList.size(); i++) {
                            if (i > 0) {
                                String[] prevRecord = teamList.get(i - 1).getRecord().split(",");
                                String[] currRecord = teamList.get(i).getRecord().split(",");

                                int prevWins = Integer.parseInt(prevRecord[0]);
                                int prevDiff = Integer.parseInt(prevRecord[2]);

                                int currWins = Integer.parseInt(currRecord[0]);
                                int currDiff = Integer.parseInt(currRecord[2]);

                                // 이전 팀과 다르면 순위 증가
                                if (prevWins != currWins || prevDiff != currDiff) {
                                    currentRank = i + 1;
                                }
                            }
                            teamList.get(i).setRank(String.valueOf(currentRank));
                        }

                        sectionDto.setRankings(teamList);

                        sectionList.add(sectionDto);
                    }

                    stageDto.setSections(sectionList);

                    stageList.add(stageDto);
                }

                dto.setStages(stageList);

                standingsList.add(dto);
            }

            return standingsList;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse data", e);
        }
    }*/



}
