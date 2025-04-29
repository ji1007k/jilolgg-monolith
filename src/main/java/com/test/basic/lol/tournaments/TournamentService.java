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
        int currentYear = LocalDate.now().getYear();

        Mono<String> result = lolEsportsApiClient.fetchTournaments();

        try {
            // 파싱
            JsonNode tournamentNodes = objectMapper.readTree(result.block())
                .path("data").path("leagues").get(0).path("tournaments");

            List<TournamentDto> tournaments = new ArrayList<>();

            for (JsonNode tournament : tournamentNodes) {
                tournaments.add(objectMapper.treeToValue(tournament, TournamentDto.class));
            }

            return tournaments.stream().filter(tournament ->
                tournament.getStartDate().getYear() == currentYear
            ).toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse data", e);
        }

    }
}
