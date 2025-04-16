package com.test.basic.lol.comp;

import com.test.basic.lol.api.LolEsportsApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


// TODO
//  - 리그 ID 별 조회 기능 추가

@Service
public class CompService {
    private final LolEsportsApiClient apiClient;

    private List<CompDto> cachedComps = null;
    private Instant lastFetchedTime = null;
    private static final Duration TTL = Duration.ofMinutes(10);

    public CompService(LolEsportsApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<CompDto> getAllComps() {
        if (cachedComps != null && lastFetchedTime != null &&
                Duration.between(lastFetchedTime, Instant.now()).compareTo(TTL) < 0) {
            return cachedComps; // 아직 TTL 안 지났으면 캐시 데이터 사용
        }

        // TTL 지났거나 최초 요청이면 새로 로딩
        cachedComps = apiClient.fetchScheduleComps();
        lastFetchedTime = Instant.now();
        return cachedComps;
    }

    public List<CompDto> getComps(String teamCode) {
        return getAllComps().stream()
                .filter(dto -> dto.getTeams().stream()
                        .anyMatch(code -> code.equalsIgnoreCase(teamCode)))
                .collect(Collectors.toList());
    }

}
