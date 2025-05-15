package com.test.basic.lol.sync;

import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.matches.Match;
import com.test.basic.lol.matches.MatchDetailResponse;
import com.test.basic.lol.matches.MatchRepository;
import com.test.basic.lol.matchteams.MatchTeam;
import com.test.basic.lol.matchteams.MatchTeamRepository;
import com.test.basic.lol.teams.Team;
import com.test.basic.lol.teams.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SyncMatchService {
    private static Logger logger = LoggerFactory.getLogger(SyncMatchService.class);

    private final LolEsportsApiClient apiClient;
    private final RedissonClient redissonClient;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;

    // TODO 
    //  - 경기 시간과 현재시각 비교해서 시작 1시간쯤 전부터만 갱신하기
    @Transactional
    public String syncTodaysMatchesFromLolEsportsApi(List<Match> matches) {
        // Redisson Lock 획득
        RLock lock = redissonClient.getLock("sync-matches-lock");
        boolean isLocked = false;

        try {
            long startTime = System.currentTimeMillis();
            isLocked = lock.tryLock(1, 600, TimeUnit.SECONDS);  // 1분 대기 및 최대 600초(10분)간 락 유지
            long endTime = System.currentTimeMillis();
            logger.info(">>> 락 획득 결과: {}, 대기 시간: {}ms", isLocked, (endTime - startTime));

            if (! isLocked) {
                logger.warn(">>> 락 획득 실패. 이미 다른 동기화 작업이 진행 중.");
                return "이미 동기화 작업이 진행 중입니다.";
            }

            logger.info(">>> 락 획득 성공.");
            logger.info(">>> LOL Esports API로부터 경기 정보 동기화 시작");

            for (Match match : matches) {
                String matchId = match.getMatchId();

                // matchid로 경기 상세 api 요청&응답 수신
                Mono<MatchDetailResponse> monoResponse = apiClient.fetchMatchDetailFromApi(matchId);
                MatchDetailResponse response = monoResponse.block();    // 데이터 비교 위해 동기식으로 요청

                if (response == null || response.getData() == null || response.getData().getEvent() == null) {
                    throw new RuntimeException("No match detail found for ID: " + matchId);
                }

                // 응답 결과와 db 데이터 비교
                MatchDetailResponse.MatchDto matchDetail = response.getData().getEvent().getMatch();

                boolean isUpdated = false;

                // [1] Match 기본 정보 갱신. matchDetail의 각 game별 state가 모두 completed 또는 unneeded 인지 확인해야함
                List<MatchDetailResponse.GameDto> games = matchDetail.getGames();

                String computedState;

                if (games.stream().anyMatch(g -> "inProgress".equalsIgnoreCase(g.getState()))) {
                    computedState = "inProgress";
                } else if (games.stream().anyMatch(g -> "unstarted".equalsIgnoreCase(g.getState()))) {
                    computedState = "unstarted";
                } else {
                    // 모두 completed 또는 unneeded인 경우
                    computedState = "completed";
                }

                if (!Objects.equals(match.getState(), computedState)) {
                    logger.info("Match [{}] 상태 변경: {} → {}", match.getMatchId(), match.getState(), computedState);
                    match.setState(computedState);
                    isUpdated = true;
                }

                // 변경된 경우 업데이트
                if (isUpdated) {
                    matchRepository.save(match);
                }


                // [2] MatchTeam 갱신
                for (MatchDetailResponse.TeamDto teamDto : matchDetail.getTeams()) {
                    Optional<Team> teamOpt;
                    if (teamDto.getName().equalsIgnoreCase("TBD")) {
                        teamOpt = teamRepository.findByName(teamDto.getName());
                    } else {
                        teamOpt = teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName());
                    }

                    if (teamOpt.isEmpty()) {
                        throw new RuntimeException("Team not found: " + teamDto.getName());
                    }

                    Team team = teamOpt.get();

                    MatchTeam matchTeam = matchTeamRepository
                            .findByMatch_MatchIdAndTeam_TeamId(match.getMatchId(), team.getTeamId())
                            .orElseGet(MatchTeam::new);

                    boolean teamUpdated = false;

                    if (!Objects.equals(matchTeam.getTeam(), team)) {
                        matchTeam.setTeam(team);
                        teamUpdated = true;
                    }

                    if (teamDto.getResult() != null) {
                        int newGameWins = teamDto.getResult().getGameWins();
                        if (matchTeam.getGameWins() != newGameWins) {
                            matchTeam.setGameWins(newGameWins);
                            teamUpdated = true;
                        }

                        // 경기 종료 상태일 때만 outcome 계산
                        if ("completed".equalsIgnoreCase(computedState)) {
                            int strategyCount = match.getGameCount(); // BO1, BO3, BO5 등
                            int majority = (strategyCount / 2) + 1;

                            // 현재 팀의 승리 여부 확인
                            boolean isWinner = newGameWins >= majority;

                            // outcome 설정
                            String outcome = isWinner ? "win" : "loss";

                            if (!Objects.equals(matchTeam.getOutcome(), outcome)) {
                                matchTeam.setOutcome(outcome);
                                teamUpdated = true;
                            }
                        }
                    }

                    if (teamUpdated) {
                        matchTeamRepository.save(matchTeam);
                    }
                }
            }

            return "금일 경기 일정 동기화 성공";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(">> 락 획득 중 예외 발생: {}", e.getMessage());
            return "락 획득 실패";
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.warn(">>> 락 해제 완료");
            } else {
                logger.warn(">>> 락 해제 시도 없음 (락 획득 실패 또는 타임아웃에 의한 자동 해제)");
            }
        }
    }
}
