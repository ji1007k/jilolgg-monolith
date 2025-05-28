package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MatchItemWriter(MatchRepository matchRepository, TeamRepository teamRepository,
                              MatchTeamRepository matchTeamRepository) implements ItemWriter<MatchAggregate> {

    // Spring Batch 5 이상. Chunk 객체 사용. (내부에 List를 포함한 래퍼)
    // 데이터 + 배치 처리 관련 메타데이터
    @Override
    public void write(Chunk<? extends MatchAggregate> chunk) throws Exception {
        List<MatchAggregate> items = (List<MatchAggregate>) chunk.getItems();

        // 1. 모든 팀 코드 수집 (TBD 포함)
        Set<String> teamCodes = new HashSet<>();
        for (MatchAggregate mag : items) {
            for (MatchScheduleResponse.TeamDto teamDto : mag.teams()) {
                if (!teamDto.getName().equalsIgnoreCase("TBD")) {
                    teamCodes.add(teamDto.getCode());
                }
            }
        }

        // 2. 한 번에 팀들 조회 (teamRepository에 findByCodeIn 메서드가 있어야 함)
        List<Team> teams = teamRepository.findByCodeIn(teamCodes);
        Map<String, Team> codeToTeamMap = teams.stream()
                .collect(Collectors.toMap(Team::getCode, Function.identity()));

        for (MatchAggregate mag : items) {
            Match incoming = mag.match();

            Match savedMatch = matchRepository.findByMatchId(incoming.getMatchId())
                    .map(existing -> {
                        existing.setStartTime(incoming.getStartTime());
                        existing.setState(incoming.getState());
                        existing.setBlockName(incoming.getBlockName());
                        existing.setGameCount(incoming.getGameCount());
                        existing.setStrategy(incoming.getStrategy());
                        existing.setLeague(incoming.getLeague());
                        return matchRepository.save(existing);
                    })
                    .orElseGet(() -> matchRepository.save(incoming));

            for (MatchScheduleResponse.TeamDto teamDto : mag.teams()) {
                Team team;

                if (teamDto.getName().equalsIgnoreCase("TBD")) {
                    // TBD 팀은 별도 조회 (기존 코드 재활용)
                    Optional<Team> tbdTeamOpt = teamRepository.findByName(teamDto.getName());
                    if (tbdTeamOpt.isEmpty()) {
                        throw new RuntimeException("Team not found with name: " + teamDto.getName());
                    }
                    team = tbdTeamOpt.get();
                } else {
                    team = codeToTeamMap.get(teamDto.getCode());
                    if (team == null) {
                        throw new RuntimeException("Team not found with code: " + teamDto.getCode());
                    }
                }

                MatchTeam matchTeam = matchTeamRepository
                        .findByMatch_MatchIdAndTeam_TeamId(savedMatch.getMatchId(), team.getTeamId())
                        .orElseGet(() -> {
                            MatchTeam mt = new MatchTeam();
                            mt.setMatch(savedMatch);
                            mt.setTeam(team);
                            return mt;
                        });

                if (teamDto.getResult() != null) {
                    matchTeam.setOutcome(teamDto.getResult().getOutcome());
                    matchTeam.setGameWins(teamDto.getResult().getGameWins());
                }

                matchTeamRepository.save(matchTeam);
            }
        }
    }

}
