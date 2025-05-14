package com.test.basic.lol.matches;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.leagues.League;
import com.test.basic.lol.leagues.LeagueRepository;
import com.test.basic.lol.matchteams.MatchTeam;
import com.test.basic.lol.matchteams.MatchTeamDto;
import com.test.basic.lol.matchteams.MatchTeamRepository;
import com.test.basic.lol.teams.Team;
import com.test.basic.lol.teams.TeamDto;
import com.test.basic.lol.teams.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service
public class MatchService {
    private final LolEsportsApiClient apiClient;
    private final MatchMapper matchMapper;
    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final LeagueRepository leagueRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private List<MatchDto> cachedMatches = null;
    private Instant lastFetchedTime = null;
    private static final Duration TTL = Duration.ofMinutes(10);

    public MatchService(LolEsportsApiClient apiClient, MatchMapper matchMapper, ObjectMapper objectMapper, MatchRepository matchRepository, TeamRepository teamRepository, MatchTeamRepository matchTeamRepository, LeagueRepository leagueRepository) {
        this.apiClient = apiClient;
        this.matchMapper = matchMapper;
        this.objectMapper = objectMapper;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.leagueRepository = leagueRepository;
    }

    public List<MatchDto> getAllMatches() {
        if (cachedMatches != null && lastFetchedTime != null &&
                Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            return cachedMatches; // 아직 TTL 안 지났으면 캐시 데이터 사용
        }

        // TTL 지났거나 최초 요청이면 새로 로딩
        String response = apiClient.fetchScheduleMatchesJson().block();
        cachedMatches = parseMatchesFromResponse(response, null);
        lastFetchedTime = Instant.now();
        return cachedMatches;
    }

    public List<MatchDto> getMatchesByLeagueId(String leagueId) {
        if (cachedMatches != null && lastFetchedTime != null &&
                Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            return cachedMatches; // 아직 TTL 안 지났으면 캐시 데이터 사용
        }

        // TTL 지났거나 최초 요청이면 새로 로딩
        String response = apiClient.fetchScheduleMatchesJson().block();
        cachedMatches = parseMatchesFromResponse(response, leagueId);
        lastFetchedTime = Instant.now();
        return cachedMatches;
    }

    public List<MatchDto> getMatchesByTeamName(String name) {
        return getAllMatches().stream()
                .filter(dto -> dto.getParticipants().stream()
                        .anyMatch(matchTeamDto -> matchTeamDto
                                .getTeam().getName()
                                .equalsIgnoreCase(name)))
                .collect(Collectors.toList());
    }

    // 리그id, 연도별 데이터 동기화 (블로킹 방식)
    public void syncMatchesByExternalApi(List<String> leagueIds, String year) {
        leagueIds.forEach(
                leagueId -> syncMatchesByLeagueIdAndYearExternalApi(leagueId, year)
        );
    }

    // 파라미터로 전달된 YEAR 데이터까지만 갱신.
    // EX) 2022를 전달한 경우, 금년 2025부터~2024,2023,2022 데이터 갱신
    public void syncMatchesByLeagueIdAndYearExternalApi(String leagueId, String year) {
        Optional<League> leagueOpt = leagueRepository.findByLeagueId(leagueId);
        if (leagueOpt.isEmpty()) {
            throw new RuntimeException("League not found with id: " + leagueId);
        }

        int targetYear = Integer.parseInt(year);
        String nextPageToken = null;

        do {
            String finalToken = nextPageToken;

            MatchScheduleResponse response = apiClient
                    .fetchScheduleByLeagueIdAndPageToken(leagueId, finalToken)
                    .block();

            if (response == null || response.getData() == null || response.getData().getSchedule() == null) {
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

                // [2] MatchTeam 저장
                for (MatchScheduleResponse.TeamDto teamDto : matchDto.getTeams()) {
                    Optional<Team> teamOpt = teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName());
                    if (teamOpt.isEmpty()) continue;

                    Team team = teamOpt.get();
                    MatchTeam matchTeam = matchTeamRepository
                            .findByMatch_MatchIdAndTeam_TeamId(savedMatch.getMatchId(), team.getTeamId())
                            .orElseGet(MatchTeam::new);

                    matchTeam.setMatch(savedMatch);
                    matchTeam.setTeam(team);
                    matchTeam.setOutcome(teamDto.getResult().getOutcome());
                    matchTeam.setGameWins(teamDto.getResult().getGameWins());

                    matchTeamRepository.save(matchTeam);
                }
            }

            nextPageToken = response.getData().getSchedule().getPages().getOlder();

        } while (nextPageToken != null);
    }


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

    public List<MatchDto> parseMatchesFromResponse(String response, String leagueId) {
        List<MatchDto> result = new ArrayList<>();

        try {
            JsonNode events = objectMapper.readTree(response)
                    .path("data").path("schedule").path("events");

            for (JsonNode event : events) {
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
                        event.path("startTime").asText(),
                        event.path("state").asText(),
                        winningTeamCode,
                        matchTeamDtos.stream()
                                .map(team -> {
                                    team.getTeam().setLeagueId(leagueId);
                                    return team;
                                })
                                .toList()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse schedule data", e);
        }

        return result;
    }

    public List<MatchDto> parseMatchesFromEvents(JsonNode events, String leagueId, String year) {
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
    }

    // TODO 양방향 연관관계로 인한 순환참조 이슈 해결 방법 더 알아보기
    public List<MatchDto> getMatchesFromDB(String year, String leagueId) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM Match m WHERE 1 = 1");

        if (year != null) {
            jpql.append(" AND FUNCTION('date_part', 'year', m.startTime) = :year");
        }
        if (leagueId != null) {
            jpql.append(" AND m.league.leagueId = :leagueId");
        }

        // JPQL 쿼리 생성
        TypedQuery<Match> query = entityManager.createQuery(jpql.toString(), Match.class);

        // 파라미터 설정
        if (year != null) {
            query.setParameter("year", Integer.parseInt(year)); // YEAR 함수는 정수로 비교
        }
        if (leagueId != null) {
            query.setParameter("leagueId", leagueId);
        }

        // 결과 반환
        List<Match> matches = query.getResultList();

        return matches.stream()
                .map(matchMapper::entityToDto)
                .toList();

    }


}
