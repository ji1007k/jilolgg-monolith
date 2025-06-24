package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MatchItemWriter(MatchRepository matchRepository, TeamRepository teamRepository,
                              MatchTeamRepository matchTeamRepository) implements ItemWriter<MatchAggregate> {

    private static final Logger logger = LoggerFactory.getLogger(MatchItemWriter.class);

    // Spring Batch 5 이상. Chunk 객체 사용. (내부에 List를 포함한 래퍼)
    // 데이터 + 배치 처리 관련 메타데이터
    @Override
    public void write(Chunk<? extends MatchAggregate> chunk) {
        List<MatchAggregate> items = (List<MatchAggregate>) chunk.getItems();

        // [1] 모든 팀 이름 수집 (TBD 제외)
        Set<String> teamNames = items.stream()
                .flatMap(m -> m.teams().stream())
                .map(MatchScheduleResponse.TeamDto::getName)
                .filter(name -> !"TBD".equalsIgnoreCase(name))
                .collect(Collectors.toSet());

        // [2] 팀 조회 + resolve map 생성
        List<Team> teams = findUniqueTeamsByNameOrCode(teamNames, items);
        Map<String, Team> codeToTeamMap = teams.stream()
                .collect(Collectors.toMap(Team::getCode, Function.identity()));

        // [3] matchId 수집
        Set<String> matchIds = items.stream()
                .map(m -> m.match().getMatchId())
                .collect(Collectors.toSet());

        // [4] Match bulk 조회 + 병합
        Map<String, Match> existingMatches = matchRepository.findByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(Match::getMatchId, Function.identity()));

        List<Match> matchesToSave = new ArrayList<>();
        for (MatchAggregate mag : items) {
            Match incoming = mag.match();
            Match merged = existingMatches.getOrDefault(incoming.getMatchId(), incoming);
            if (existingMatches.containsKey(incoming.getMatchId())) {
                merged.setStartTime(incoming.getStartTime());
                merged.setState(incoming.getState());
                merged.setBlockName(incoming.getBlockName());
                merged.setGameCount(incoming.getGameCount());
                merged.setStrategy(incoming.getStrategy());
                merged.setLeague(incoming.getLeague());
            }
            matchesToSave.add(merged);
        }

        List<Match> savedMatches = matchRepository.saveAll(matchesToSave);
        Map<String, Match> savedMatchMap = savedMatches.stream()
                .collect(Collectors.toMap(Match::getMatchId, Function.identity()));

        // [5] 기존 MatchTeam bulk 조회
        List<MatchTeam> existingMatchTeams = matchTeamRepository.findByMatch_MatchIdIn(matchIds);
        Map<String, List<MatchTeam>> matchIdToTeamsMap = existingMatchTeams.stream()
                .collect(Collectors.groupingBy(mt -> mt.getMatch().getMatchId()));

        // [6] TBD 삭제 대상 식별
        List<MatchTeam> matchTeamsToDelete = new ArrayList<>();
        for (MatchAggregate mag : items) {
            String matchId = mag.match().getMatchId();

            // MatchTeam 갱신
            // 기존 매치 팀 데이터 조회 (DB에 저장된 상태)
            List<MatchTeam> existingTeams = matchIdToTeamsMap.getOrDefault(matchId, Collections.emptyList());

            long existingTbdCnt = existingTeams.stream()
                    .filter(mt -> "TBD".equalsIgnoreCase(mt.getTeam().getName()))
                    .count();
            long incomingTbdCnt = mag.teams().stream()
                    .filter(t -> "TBD".equalsIgnoreCase(t.getName()))
                    .count();

            // 기존 TBD 개수 != 새로운 팀 목록 TBD 개수 -> 기존 TBD 삭제
            if (existingTbdCnt != incomingTbdCnt) {
                matchTeamsToDelete.addAll(
                        existingTeams.stream()
                                .filter(mt -> "TBD".equalsIgnoreCase(mt.getTeam().getName()))
                                .toList()
                );
            }
        }
        if (!matchTeamsToDelete.isEmpty()) {
            matchTeamRepository.deleteAll(matchTeamsToDelete);
        }

        // [7] MatchTeam 병합 후 saveAll
        Map<String, MatchTeam> matchTeamMap = existingMatchTeams.stream()
                .collect(Collectors.toMap(
                        mt -> mt.getMatch().getMatchId() + "_" + mt.getTeam().getTeamId(),
                        Function.identity()
                ));

        List<MatchTeam> matchTeamsToSave = new ArrayList<>();

        for (MatchAggregate mag : items) {
            Match match = savedMatchMap.get(mag.match().getMatchId());

            for (MatchScheduleResponse.TeamDto teamDto : mag.teams()) {
                Team team = resolveTeam(teamDto, codeToTeamMap);
                if (team == null) {
                    logger.warn("Team not found: {}", teamDto.getCode());
                    continue;
                }

                String key = match.getMatchId() + "_" + team.getTeamId();
                MatchTeam mt = matchTeamMap.get(key);

                if (mt == null) {
                    mt = new MatchTeam();
                    mt.setMatch(match);
                    mt.setTeam(team);
                }

                if (teamDto.getResult() != null) {
                    mt.setOutcome(teamDto.getResult().getOutcome());
                    mt.setGameWins(teamDto.getResult().getGameWins());
                }

                matchTeamsToSave.add(mt);
            }
        }

        matchTeamRepository.saveAll(matchTeamsToSave);
    }


    private List<Team> findUniqueTeamsByNameOrCode(Set<String> teamNames, List<MatchAggregate> items) {
        // 1. name 기준 조회
        List<Team> teamsByName = teamRepository.findByNameIn(teamNames);

        // 2. name 기준으로 그룹핑 → 중복된 name 찾아내기
        Map<String, List<Team>> nameGrouped = teamsByName.stream()
                .collect(Collectors.groupingBy(Team::getName));

        // 3. 중복 name 리스트 뽑기
        Set<String> duplicateNames = nameGrouped.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 4. 중복된 팀들만 제거
        List<Team> filteredTeamsByName = teamsByName.stream()
                .filter(team -> !duplicateNames.contains(team.getName()))
                .toList();

        // 5. 중복된 팀 name → code 로 다시 조회
        Set<String> duplicateCodes = items.stream()
                .flatMap(mag -> mag.teams().stream())
                .filter(teamDto -> duplicateNames.contains(teamDto.getName()))
                .map(MatchScheduleResponse.TeamDto::getCode)
                .collect(Collectors.toSet());

        List<Team> teamsByCode = teamRepository.findByCodeIn(duplicateCodes);

        // 6. 합치기
        List<Team> finalTeams = new ArrayList<>();
        finalTeams.addAll(filteredTeamsByName);
        finalTeams.addAll(teamsByCode);

        return finalTeams;
    }

    private Team resolveTeam(MatchScheduleResponse.TeamDto teamDto, Map<String, Team> codeToTeamMap) {
        if ("TBD".equalsIgnoreCase(teamDto.getName())) {
            return teamRepository.findByName("TBD").orElse(null);
        }
        return codeToTeamMap.get(teamDto.getCode());
    }



}
