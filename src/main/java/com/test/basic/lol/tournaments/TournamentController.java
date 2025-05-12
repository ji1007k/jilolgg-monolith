package com.test.basic.lol.tournaments;

import com.test.basic.lol.sync.LolSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/tournaments")
@Tag(name = "[LOL] 토너먼트 API", description = "토너먼트 관련 API")
public class TournamentController {

    private final TournamentService tournamentService;
    private final LolSyncService lolSyncService;

    public TournamentController(TournamentService tournamentService, LolSyncService lolSyncService) {
        this.tournamentService = tournamentService;
        this.lolSyncService = lolSyncService;
    }

    @GetMapping
    @Operation(summary = "금년도 토너먼트 목록 조회", description = "금년도 토너먼트 목록 조회 API")
    public ResponseEntity<List<TournamentDto>> getTournamentsForCurrentYear() {
        List<TournamentDto> tournaments = tournamentService.getTournamentsForCurrentYear();
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/sync")
    public ResponseEntity<List<TournamentDto>> getAllTournamentsByLeagueIdFromApi() {
        lolSyncService.syncTournaments();
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }
}
