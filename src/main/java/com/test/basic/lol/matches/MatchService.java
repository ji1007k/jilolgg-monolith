package com.test.basic.lol.matches;

import com.test.basic.lol.api.LolEsportsApiClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


// TODO
//  - 리그 ID 별 조회 기능 추가

@Service
public class MatchService {
    private final LolEsportsApiClient apiClient;

    private List<MatchDto> cachedMatches = null;
    private Instant lastFetchedTime = null;
    private static final Duration TTL = Duration.ofMinutes(10);

    public MatchService(LolEsportsApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<MatchDto> getAllMatches() {
        if (cachedMatches != null && lastFetchedTime != null &&
                Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            return cachedMatches; // 아직 TTL 안 지났으면 캐시 데이터 사용
        }

        // TTL 지났거나 최초 요청이면 새로 로딩
        String response = apiClient.fetchScheduleMatches().block();
        cachedMatches = apiClient.parseMatchesFromResponse(response);
        lastFetchedTime = Instant.now();
        return cachedMatches;
    }

    public List<MatchDto> getMatchesByName(String teamName) {
        return getAllMatches().stream()
                .filter(dto -> dto.getParticipants().stream()
                        .anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamName)))
                .collect(Collectors.toList());
    }

}
