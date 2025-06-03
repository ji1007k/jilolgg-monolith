package com.test.basic.lol.domain.match;

import com.test.basic.lol.api.esports.dto.MatchDetailResponse;
import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SyncMatchService {
    private static Logger logger = LoggerFactory.getLogger(SyncMatchService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final MatchApiService matchApiService;

    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;

    private final RedissonClient redissonClient;
    private RLock lock;
    private String LOCK_KEY = "sync-matches-lock";


    @Transactional
    public void syncTodaysMatchesFromLolEsportsApi(List<Match> matches) {
        // Redisson Lock 획득
        lock = redissonClient.getLock(LOCK_KEY); // lightweight 프록시 객체
        boolean isLocked = false;

        try {
            long startTime = System.currentTimeMillis();
            // 도중에 컨테이너가 죽으면 락이 leaseTime(10분)간 Redis에 계속 남음
            //  -> 다른 인스턴스가 tryLock 시도 시 "다른 애가 락 잡고 있음" -> 오래된 락
//            isLocked = lock.tryLock(1, 600, TimeUnit.SECONDS);  // 1분 대기 및 최대 600초(10분)간 락 유지
            // leaseTime 생략 → watchdog 활성화
            //  -> Redisson이 백그라운드에서 락을 유지하고, 컨테이너/JVM 죽으면 watchdog도 종료 → 락 자동 해제
            isLocked = lock.tryLock(1, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            logger.info(">>> 락 획득 결과: {}, 대기 시간: {}ms", isLocked, (endTime - startTime));

            if (! isLocked) {
                logger.warn(">>> 락 획득 실패. 이미 다른 동기화 작업이 진행 중.");
                return;
            }

            logger.info(">>> 락 획득 성공.");
            logger.info(">>> LOL Esports API로부터 경기 정보 동기화 시작");

            for (Match match : matches) {
                String matchId = match.getMatchId();

                // matchid로 경기 상세 api 요청&응답 수신
                Mono<MatchDetailResponse> monoResponse = matchApiService.fetchMatchDetailFromApi(matchId);
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
                            .orElseGet(() -> {
                                // 여기서 새로 생성된 MatchTeam은 match 필드 값이 null이기 떄문에 세팅 필요
                                MatchTeam mt = new MatchTeam();
                                mt.setMatch(match);
                                return mt;
                            });

                    boolean teamUpdated = false;

                    if (!Objects.equals(matchTeam.getTeam(), team)) {
                        matchTeam.setTeam(team);
                        teamUpdated = true;
                    }

                    if (teamDto.getResult() != null) {
                        int newGameWins = teamDto.getResult().getGameWins();
                        if (matchTeam.getGameWins() == null || matchTeam.getGameWins() != newGameWins) {
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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(">> 락 획득 중 예외 발생: {}", e.getMessage());
        } finally {
            cleanup();

            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            logger.info("현재 JVM 힙 사용량: {} MB", used);
        }
    }

    @PreDestroy
    public void cleanup() {
        // 현재 스레드가 잡은 락인지 보장하기 위해 isHeldByCurrentThread() 꼭 확인
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            logger.info(">>> 락 해제 완료");
        } else {
            logger.warn(">>> 락 해제 시도 없음 (락 획득 실패 또는 타임아웃에 의한 자동 해제)");
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();
    }


    // 리그id, 연도별 데이터 동기화(블로킹 방식)
    // 파라미터로 전달된 YEAR 데이터까지만 갱신
    // EX) 2022를 전달한 경우, 금년 2025부터~2024,2023,2022 데이터 갱신
    @Transactional
    public void syncMatchesByLeagueIdAndYearExternalApi(String leagueId, String year) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("다른 동기화 작업이 실행 중입니다.");
            }

            Optional<League> leagueOpt = leagueRepository.findByLeagueId(leagueId);
            if (leagueOpt.isEmpty()) {
                throw new RuntimeException("League not found with id: " + leagueId);
            }

            int targetYear = Integer.parseInt(year);
            String nextPageToken = null;

            do {
                String finalToken = nextPageToken;

                MatchScheduleResponse response = matchApiService.fetchScheduleByLeagueIdAndPageToken(leagueId, finalToken);

                if (response == null || response.getData() == null || response.getData().getSchedule() == null) {
                    logger.warn("[{}] 리그의 일정 정보가 비어 있습니다. nextToken: {}", leagueId, finalToken);
                    break;
                }

                List<MatchScheduleResponse.EventDto> events = response.getData()
                        .getSchedule()
                        .getEvents();

                // 1️⃣ 페이지 내 모든 이벤트가 targetYear보다 이전이면 종료
                boolean allBeforeTargetYear = events.stream()
                        .filter(event -> event.getStartTime() != null)
                        .map(event -> OffsetDateTime.parse(event.getStartTime())
                                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                .toLocalDateTime()
                                .getYear())
                        .allMatch(eventYear -> eventYear < targetYear);

                if (allBeforeTargetYear) break;

                for (MatchScheduleResponse.EventDto event : events) {
                    if (event.getMatch() == null || event.getStartTime() == null) continue;

                    // 시간대 포함된 문자열 -> LocalDateTime 변환
                    // OffsetDateTime 자체에 시간대가 있음 → 서울 시간대로 맞춤 변환
                    LocalDateTime eventDateTime = OffsetDateTime.parse(event.getStartTime())
                            .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            .toLocalDateTime();

                    int eventYear = eventDateTime.getYear();

                    // 2️⃣ 연도가 타겟보다 작으면 무시 (스킵)
                    if (eventYear < targetYear) continue;

                    MatchScheduleResponse.MatchDto matchDto = event.getMatch();

                    // [1] Match 저장
                    Match match = matchRepository.findByMatchId(matchDto.getId()).orElseGet(Match::new);
                    match.setMatchId(matchDto.getId());
                    match.setLeague(leagueOpt.get());
                    match.setStartTime(eventDateTime);
                    match.setState(event.getState());
                    match.setBlockName(event.getBlockName());
                    match.setGameCount(matchDto.getStrategy().getCount());
                    match.setStrategy(matchDto.getStrategy().getType() + matchDto.getStrategy().getCount());

                    Match savedMatch = matchRepository.save(match);

                    // [2] MatchTeam 저장.
                    for (MatchScheduleResponse.TeamDto teamDto : matchDto.getTeams()) {
                        Optional<Team> teamOpt;
                        if (teamDto.getName().equalsIgnoreCase("TBD")) {    // TBD (To Be Determined)
                            teamOpt = teamRepository.findByName(teamDto.getName());
                        } else {
                            teamOpt = teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName());
                        }

                        if (teamOpt.isEmpty()) {
                            throw new RuntimeException("Team not found with name: " + teamDto.getName());
                        }

                        Team team = teamOpt.get();
                        MatchTeam matchTeam = matchTeamRepository
                                .findByMatch_MatchIdAndTeam_TeamId(savedMatch.getMatchId(), team.getTeamId())
                                .orElseGet(MatchTeam::new);

                        matchTeam.setMatch(savedMatch);
                        matchTeam.setTeam(team);

                        if (teamDto.getResult() != null) {
                            matchTeam.setOutcome(teamDto.getResult().getOutcome());
                            matchTeam.setGameWins(teamDto.getResult().getGameWins());
                        }

                        matchTeamRepository.save(matchTeam);
                    }
                }

                nextPageToken = response.getData().getSchedule().getPages().getOlder();

            } while (nextPageToken != null);

        } catch (InterruptedException ie) {
            throw new RuntimeException("동기화 락 획득 중 인터럽트 발생", ie);
        } catch (Exception e) {
            logger.error("[{}] 리그 동기화 실패: {}", leagueId, e.getMessage(), e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

            entityManager.flush();
            entityManager.clear();
        }
    }

    // 250526 미사용. 참고용. ==================================================

    // 리그id, 연도별 데이터 동기화
    /*public List<MatchDto> getMatchesByLeagueIdAndYearFromExternalApi(String leagueId, String year) {

        List<MatchDto> allMatches = new ArrayList<>();
        String nextPageToken = null;

        do {
            String finalToken = nextPageToken;

            Mono<String> response = apiClient.fetchScheduleJsonByLeagueIdAndPageToken(leagueId, finalToken);

            try {
                JsonNode root = objectMapper.readTree(response.block());
                JsonNode schedule = root.path("data").path("schedule");
                JsonNode events = schedule.path("events");

                List<MatchDto> pageMatches = parseMatchesFromEvents(events, leagueId, year);
                allMatches.addAll(pageMatches);

                // 중단 조건: 더 이상 해당 연도의 이벤트가 없음
                boolean allBeforeTargetYear = StreamSupport.stream(events.spliterator(), false)
                        .noneMatch(event -> event.path("startTime").asText().startsWith(year));
                if (allBeforeTargetYear) break;

                JsonNode pages = schedule.path("pages");
                nextPageToken = pages.path("older").asText(null);

            } catch (Exception e) {
                throw new RuntimeException("Failed to parse response", e);
            }

        } while (nextPageToken != null);

        return allMatches;
    }*/

    /*public List<MatchDto> parseMatchesFromEvents(JsonNode events, String leagueId, String year) {
        List<MatchDto> result = new ArrayList<>();

        for (JsonNode event : events) {
            String startTime = event.path("startTime").asText();
            if (!startTime.startsWith(year)) continue;

            List<MatchTeamDto> matchTeamDtos = StreamSupport.stream(
                            event.path("match").path("teams").spliterator(), false)
                    .map(team -> new MatchTeamDto(
                            team.path("result").path("outcome").asText(),
                            team.path("result").path("gameWins").asInt(),
                            new TeamDto(team.path("code").asText(),
                                    team.path("name").asText())
                    ))
                    .toList();

            boolean completed = event.path("state").asText().equalsIgnoreCase("completed");
            String winningTeamCode = completed
                    ? matchTeamDtos.stream()
                    .filter(team -> "win".equalsIgnoreCase(team.getOutcome()))
                    .map(matchTeamDto -> matchTeamDto.getTeam().getCode())
                    .findFirst().orElse(null)
                    : null;

            result.add(new MatchDto(
                    event.path("match").path("id").asText(),
                    startTime,
                    event.path("state").asText(),
                    event.path("strategy").path("type").asText(),
                    winningTeamCode,
                    matchTeamDtos.stream()
                            .map(team -> {
                                team.getTeam().setLeagueId(leagueId);
                                return team;
                            })
                            .toList()
            ));
        }

        return result;
    }*/

}
