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

        // 1. 모든 팀명 수집 (TBD 포함)
        Set<String> teamNames = new HashSet<>();
        for (MatchAggregate mag : items) {
            for (MatchScheduleResponse.TeamDto teamDto : mag.teams()) {
                if (!teamDto.getName().equalsIgnoreCase("TBD")) {
                    teamNames.add(teamDto.getName());
                }
            }
        }

        // 2. 한 번에 팀들 조회
        List<Team> teams = findUniqueTeamsByNameOrCode(teamNames, items);

        // 2-1. map 으로 변환
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

            // [2] MatchTeam 갱신
            // 기존 매치 팀 데이터 조회 (DB에 저장된 상태)
            List<MatchTeam> existingTeams = matchTeamRepository.findByMatch_MatchId(savedMatch.getMatchId());

            // 기존 데이터에 "TBD" 팀이 포함되어 있는지 확인
            boolean existingHasTbd = existingTeams.stream()
                    .anyMatch(mt -> "TBD".equalsIgnoreCase(mt.getTeam().getName()));

            // 새로 가져온 팀 목록에 "TBD"가 없는지 확인
            boolean incomingHasNoTbd = mag.teams().stream()
                    .noneMatch(t -> "TBD".equalsIgnoreCase(t.getName()));

            // 기존에는 "TBD"가 있었고, 새 데이터에는 없다면 → 삭제
            if (existingHasTbd && incomingHasNoTbd) {
                matchTeamRepository.deleteByMatch_MatchIdAndTeam_Name(savedMatch.getMatchId(), "TBD");
            }

            for (MatchScheduleResponse.TeamDto teamDto : mag.teams()) {
                Team team;

                if (teamDto.getName().equalsIgnoreCase("TBD")) {
                    // TBD 팀은 별도 조회 (기존 코드 재활용)
                    Optional<Team> tbdTeamOpt = teamRepository.findByName(teamDto.getName());
                    if (tbdTeamOpt.isEmpty()) {
                        // 예외 던지면 해당 STEP이 FATIED 처리돼버리므로 로그만 찍고 건너뛰기
//                        throw new RuntimeException("Team not found with code: " + teamDto.getCode());
                        logger.warn("Team not found with name: {}", teamDto.getName());
                        continue;
                    }
                    team = tbdTeamOpt.get();
                } else {
                    team = codeToTeamMap.get(teamDto.getCode());
                    if (team == null) {
                        logger.warn("Team not found with code: {}", teamDto.getCode());
                        continue;
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

}
