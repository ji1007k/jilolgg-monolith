package com.test.basic.lol.tournaments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.lol.api.LolEsportsApiClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TournamentService {

    private final LolEsportsApiClient lolEsportsApiClient;
    private final ObjectMapper objectMapper;

    public TournamentService(LolEsportsApiClient lolEsportsApiClient,
                             ObjectMapper objectMapper) {
        this.lolEsportsApiClient = lolEsportsApiClient;
        this.objectMapper = objectMapper;
    }

    public List<TournamentDto> getTournamentsForCurrentYear() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        Mono<String> result = lolEsportsApiClient.fetchTournamentsJson();

        try {
            // 파싱
            JsonNode tournamentNodes = objectMapper.readTree(result.block())
                .path("data").path("leagues").get(0).path("tournaments");

            List<TournamentDto> tournaments = new ArrayList<>();

            for (JsonNode tournament : tournamentNodes) {
                TournamentDto dto = objectMapper.treeToValue(tournament, TournamentDto.class);

                if (dto.getStartDate().getYear() != currentYear) continue;

                // 현재 날짜가 startDate ~ endDate 사이에 있으면 isActive 설정
                if (dto.getStartDate() != null && dto.getEndDate() != null) {
                    boolean isActive = !today.isBefore(dto.getStartDate()) && !today.isAfter(dto.getEndDate());
                    dto.setActive(isActive); // ← 여기가 핵심
                }

                tournaments.add(dto);
            }

            return tournaments;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse data", e);
        }

    }
}
