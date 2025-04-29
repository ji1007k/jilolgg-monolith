package com.test.basic.lol.standings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/standings")
@Tag(name = "LOL 리그 순위 API", description = "LOL 리그 순위 API")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping("/{tournamentId}")
    @Operation(summary = "토너먼트별 순위 조회", description = "토너먼트별 순위 조회 API")
    public ResponseEntity<List<StandingsDto>> getStandings(@PathVariable String tournamentId) {
        List<StandingsDto> standings = standingsService.getStandingsByTournamentId(tournamentId);
        return ResponseEntity.ok(standings);
    }

}
