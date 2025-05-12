package com.test.basic.lol.matches;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.matchteams.MatchTeamDto;
import com.test.basic.lol.teams.TeamDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


// TODO
//  - 리그 ID 별 조회 기능 추가

@Service
public class MatchService {
    private final LolEsportsApiClient apiClient;
    private final MatchMapper matchMapper;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private List<MatchDto> cachedMatches = null;
    private Instant lastFetchedTime = null;
    private static final Duration TTL = Duration.ofMinutes(10);

    public MatchService(LolEsportsApiClient apiClient, MatchMapper matchMapper, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.matchMapper = matchMapper;
        this.objectMapper = objectMapper;
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

    public List<MatchDto> getMatchesByLeagueIdAndYear(String leagueId, String year) {

        List<MatchDto> allMatches = new ArrayList<>();
        String nextPageToken = null;

        do {
            String finalToken = nextPageToken;

            Mono<String> response = apiClient.fetchScheduleByLeagueIdAndPageToken(leagueId, finalToken);

            try {
                JsonNode root = objectMapper.readTree(response.block());
                JsonNode schedule = root.path("data").path("schedule");
                JsonNode events = schedule.path("events");

                List<MatchDto> pageMatches = parseMatchesFromEvents(events, leagueId, year);
                allMatches.addAll(pageMatches);

                // 중단 조건: 더 이상 해당 연도의 이벤트가 없음
                String finalYear = year;
                boolean allBeforeTargetYear = StreamSupport.stream(events.spliterator(), false)
                        .allMatch(event -> !event.path("startTime").asText().startsWith(finalYear));
                if (allBeforeTargetYear) break;

                JsonNode pages = schedule.path("pages");
                nextPageToken = pages.path("older").asText(null);

            } catch (Exception e) {
                throw new RuntimeException("Failed to parse response", e);
            }

            if (response == null) break;

        } while (nextPageToken != null);

        return allMatches;
    }

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
    public List<MatchDto> getMatches(String year, String leagueId) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM Match m WHERE 1 = 1");

        if (year != null) {
            jpql.append(" AND FUNCTION('date_part', 'year', m.startTime) = :year");
        }
        if (leagueId != null) {
            jpql.append(" AND m.leagueId = :leagueId");
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
