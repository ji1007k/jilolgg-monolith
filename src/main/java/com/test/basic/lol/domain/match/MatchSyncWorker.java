package com.test.basic.lol.domain.match;

import com.test.basic.lol.api.esports.dto.MatchDetailResponse;
import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchSyncWorker {

    private final MatchApiService matchApiService;
    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncTodaysMatchFromLolEsportsApi(Match match) {
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
            log.info("Match [{}] 상태 변경: {} → {}", match.getMatchId(), match.getState(), computedState);
            match.setState(computedState);
            isUpdated = true;
        }

        // 변경된 경우 업데이트
        if (isUpdated) {
            matchRepository.save(match);
        }

        // [2] MatchTeam 갱신
        // 기존 매치 팀 데이터 조회 (DB에 저장된 상태)
        List<MatchTeam> existingTeams = matchTeamRepository.findByMatch_MatchId(match.getMatchId());

        // 기존 데이터에 "TBD" 팀이 포함되어 있는지 확인
        long existingTbdCnt = existingTeams.stream()
                .filter(mt -> "TBD".equalsIgnoreCase(mt.getTeam().getName()))
                .count();

        // 새로 가져온 팀 목록에 "TBD"가 없는지 확인
        long incomingTbdCnt = matchDetail.getTeams().stream()
                .filter(t -> "TBD".equalsIgnoreCase(t.getName()))
                .count();

        // 기존 TBD 개수 != 새로운 팀 목록 TBD 개수면 기존 TBD 삭제
        if (existingTbdCnt != incomingTbdCnt) {
            matchTeamRepository.deleteByMatch_MatchIdAndTeam_Name(match.getMatchId(), "TBD");
        }

        for (MatchDetailResponse.TeamDto teamDto : matchDetail.getTeams()) {
            Optional<Team> teamOpt;
            if (teamDto.getName().equalsIgnoreCase("TBD")) {
                teamOpt = teamRepository.findByName(teamDto.getName());
            } else {
                teamOpt = teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName());
            }

            if (teamOpt.isEmpty()) {
                log.warn("Team not found with code and name: {}({})", teamDto.getCode(), teamDto.getName());
                continue;
            }

            Team team = teamOpt.get();

            // 매칭 팀 정보가 TBD이거나 TFT인 경우 중복으로 조회될 수 있어 분기 태움
            MatchTeam matchTeam;
            if (team.getName().equalsIgnoreCase("TBD") || team.getName().equalsIgnoreCase("TFT")) {
                matchTeam = matchTeamRepository
                        .findAllByMatch_MatchIdAndTeam_TeamId(match.getMatchId(), team.getTeamId())
                        .stream().findFirst().orElseGet(() -> {
                            MatchTeam mt = new MatchTeam();
                            mt.setMatch(match);
                            return mt;
                        });
            } else {
                matchTeam = matchTeamRepository
                        .findByMatch_MatchIdAndTeam_TeamId(match.getMatchId(), team.getTeamId())
                        .orElseGet(() -> {
                            MatchTeam mt = new MatchTeam();
                            mt.setMatch(match);
                            return mt;
                        });
            }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncMatchesByLeagueIdAndYearExternalApi(String leagueId, String year) {
        Optional<League> leagueOpt = leagueRepository.findByLeagueId(leagueId);
        if (leagueOpt.isEmpty()) {
            throw new RuntimeException("League not found with id: " + leagueId);
        }

        int targetYear = Integer.parseInt(year);
        String nextPageToken = null;

        int count = 0;
        do {
            String finalToken = nextPageToken;

            MatchScheduleResponse response = matchApiService.fetchScheduleByLeagueIdAndPageToken(leagueId, finalToken);

            if (response == null || response.getData() == null || response.getData().getSchedule() == null) {
                log.warn("[{}] 리그의 일정 정보가 비어 있습니다. nextToken: {}", leagueId, finalToken);
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
                count++;


                // [2] MatchTeam 갱신
                // 기존 매치 팀 데이터 조회 (DB에 저장된 상태)
                List<MatchTeam> existingTeams = matchTeamRepository.findByMatch_MatchId(match.getMatchId());

                // 기존 데이터에 "TBD" 팀이 포함되어 있는지 확인
                long existingTbdCnt = existingTeams.stream()
                        .filter(mt -> "TBD".equalsIgnoreCase(mt.getTeam().getName()))
                        .count();

                // 새로 가져온 팀 목록에 "TBD"가 없는지 확인
                long incomingTbdCnt = matchDto.getTeams().stream()
                        .filter(t -> "TBD".equalsIgnoreCase(t.getName()))
                        .count();

                // 기존 TBD 개수 != 새로운 팀 목록 TBD 개수면 기존 TBD 삭제
                if (existingTbdCnt != incomingTbdCnt) {
                    matchTeamRepository.deleteByMatch_MatchIdAndTeam_Name(match.getMatchId(), "TBD");
                }

                // [2] MatchTeam 저장.
                for (MatchScheduleResponse.TeamDto teamDto : matchDto.getTeams()) {
                    Optional<Team> teamOpt;
                    if (teamDto.getName().equalsIgnoreCase("TBD")) {    // TBD (To Be Determined)
                        teamOpt = teamRepository.findByName(teamDto.getName());
                    } else {
                        teamOpt = teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName());
                    }

                    if (teamOpt.isEmpty()) {
//                        throw new RuntimeException("Team not found with name: " + teamDto.getName());
                        log.warn("Team not found with code and name: {}({})", teamDto.getCode(), teamDto.getName());
                        continue;
                    }

                    Team team = teamOpt.get();

                    // 매칭 팀 정보가 TBD이거나 TFT인 경우 중복으로 조회될 수 있어 분기 태움
                    MatchTeam matchTeam;
                    if (team.getName().equalsIgnoreCase("TBD") || team.getName().equalsIgnoreCase("TFT")) {
                        matchTeam = matchTeamRepository
                                .findAllByMatch_MatchIdAndTeam_TeamId(match.getMatchId(), team.getTeamId())
                                .stream().findFirst().orElseGet(() -> {
                                    MatchTeam mt = new MatchTeam();
                                    mt.setMatch(savedMatch);
                                    mt.setTeam(team);
                                    return mt;
                                });
                    } else {
                        matchTeam = matchTeamRepository
                                .findByMatch_MatchIdAndTeam_TeamId(savedMatch.getMatchId(), team.getTeamId())
                                .orElseGet(() -> {
                                    MatchTeam mt = new MatchTeam();
                                    mt.setMatch(savedMatch);
                                    mt.setTeam(team);
                                    return mt;
                                });
                    }

                    if (teamDto.getResult() != null) {
                        matchTeam.setOutcome(teamDto.getResult().getOutcome());
                        matchTeam.setGameWins(teamDto.getResult().getGameWins());
                    }

                    matchTeamRepository.save(matchTeam);
                }
            }

            nextPageToken = response.getData().getSchedule().getPages().getOlder();

        } while (nextPageToken != null);

        log.info("리그ID {} 데이터 갱신 완료 - {}건", leagueId, count);
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
                    event.path("blockName").asText(),
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
