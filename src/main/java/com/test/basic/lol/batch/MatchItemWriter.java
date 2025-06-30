package com.test.basic.lol.batch;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchService;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamService;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public record MatchItemWriter(MatchService matchService,
                              TeamService teamService,
                              MatchTeamService matchTeamService) implements ItemWriter<MatchAggregate> {

    // Spring Batch 5 이상. Chunk 객체 사용. (내부에 List를 포함한 래퍼)
    @Override
    public void write(Chunk<? extends MatchAggregate> chunk) {
        // [1] MatchItemProcessor 에서 전처리 후 반환된 데이터 가져오기
        List<MatchAggregate> mags = (List<MatchAggregate>) chunk.getItems();

        // [1] 경기 정보 갱신

        // [1-1] 갱신 대상 경기 정보 bulk 조회
        Set<String> matchIds = mags.stream()
                .map(mag -> mag.match().getMatchId())
                .collect(Collectors.toSet());

        log.debug("Processing {} matches for matchIds: {}", mags.size(), matchIds);

        Map<String, Match> existingMatchMap = matchService.getMatchEntitiesByMatchIds(matchIds)
                .stream()
                .collect(Collectors.toMap(
                        Match::getMatchId,
                        Function.identity()));

        log.debug("Found {} existing matches", existingMatchMap.size());

        // [1-2] 경기 정보 upsert 객체 준비
        List<Match> matchesToSave = new ArrayList<>();

        for (var mag : mags) {
            Match incoming = mag.match();
            Match merged;

            // 모든 매치 ID를 로깅해서 실제로 중복되는지 확인
            log.debug("파티션 [{}]에서 매치 [{}] 처리 중",
                Thread.currentThread().getName(), incoming.getMatchId());

            // 저장된 경기정보 업데이트
            if (existingMatchMap.containsKey(incoming.getMatchId())) {
                Match existing = existingMatchMap.get(incoming.getMatchId());

                // 변경된 값이 있을 때만 업데이트
                if (!Objects.equals(existing, incoming)) {
                    merged = existing;

                    // 기존 엔터티에 값 업데이트 (ID가 살아 있음)
                    existing.setStartTime(incoming.getStartTime());
                    existing.setState(incoming.getState());
                    existing.setBlockName(incoming.getBlockName());
                    existing.setGameCount(incoming.getGameCount());
                    existing.setStrategy(incoming.getStrategy());
                    existing.setLeague(incoming.getLeague());

                    matchesToSave.add(merged);
                }
            } else {
                // 신규 추가
                merged = incoming;
                matchesToSave.add(merged);
                existingMatchMap.put(incoming.getMatchId(), incoming); // 맵에도 추가
            }
        }

        log.debug("Saving {} matches", matchesToSave.size());

        // [1-3] 변경된 경기 정보 bulk 저장
        if (!matchesToSave.isEmpty()) {
            matchService.saveMatches(matchesToSave);
        }

        // [2] 팀 정보 준비 및 MatchTeam 삭제 + 재생성
        Set<String> teamNames = mags.stream()
                .flatMap(mag -> mag.teams()
                        .stream()
                        .map(team -> team.getName()))
                .collect(Collectors.toSet());

        Map<String, Team> teamsMap = teamService.getTeamsByName(teamNames)
                .stream()
                .collect(Collectors.toMap(
                        Team::getName,
                        Function.identity()
                ));

        log.debug("Deleting MatchTeams for {} matches", matchIds.size());

        // [2-1] 기존 MatchTeam 삭제
        if (!matchIds.isEmpty()) {
            matchTeamService.deleteByMatchIds(matchIds);
        }

        // [2-2] 새로운 MatchTeam 생성
        List<MatchTeam> matchTeamsToSave = new ArrayList<>();

        for (var mag : mags) {
            Match match = existingMatchMap.get(mag.match().getMatchId());

            for (var teamDto : mag.teams()) {
                Team team = teamsMap.get(teamDto.getName());
                if (team == null) continue;

                MatchTeam matchTeam = new MatchTeam();
                matchTeam.setMatch(match);
                matchTeam.setTeam(team);

                if (teamDto.getResult() != null) {
                    matchTeam.setOutcome(teamDto.getResult().getOutcome());
                    matchTeam.setGameWins(teamDto.getResult().getGameWins());
                }

                matchTeamsToSave.add(matchTeam);
            }
        }

        log.debug("Creating {} new MatchTeams", matchTeamsToSave.size());

        // [2-3] MatchTeam 정보 bulk 저장
        if (!matchTeamsToSave.isEmpty()) {
            matchTeamService.saveMatchTeams(matchTeamsToSave);
        }
    }
}
