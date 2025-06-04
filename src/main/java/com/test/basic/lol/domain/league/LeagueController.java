package com.test.basic.lol.domain.league;

import com.test.basic.lol.api.esports.SyncLolEsportsApiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/leagues")
@Tag(name = "[LOL(Esports)] 1. League API", description = "리그 관련 API")
public class LeagueController {

    public final LeagueService leagueService;
    private final SyncLolEsportsApiService syncLolEsportsApiService;

    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            // LCK, LCK CL
            "98767991310872058",
            "98767991335774713",
            // 국제 대회 (FIRST STAND, MSI, WORLDS)
            "113464388705111224",
            "98767991325878492",
            "98767975604431411",
            // LPL, LEC, ...
            "98767991314006698",
            "98767991302996019",
            "98767991349978712",
            "98767991299243165");


    public LeagueController(LeagueService leagueService, SyncLolEsportsApiService syncLolEsportsApiService) {
        this.leagueService = leagueService;
        this.syncLolEsportsApiService = syncLolEsportsApiService;
    }

    @GetMapping
    public ResponseEntity<List<LeagueDto>> getAllLeagues() {
        return ResponseEntity.ok(leagueService.getAllLeagues().stream()
                .filter(league -> MAJOR_LEAGUE_IDS.contains(league.getLeagueId()))
                .toList()
        );
    }

    @GetMapping("/sync")
    public ResponseEntity<List<LeagueDto>> getAllLeaguesFromApi() {
        syncLolEsportsApiService.syncLeaguesFromLolEsportsApi();
        return ResponseEntity.ok(leagueService.getAllLeagues());
    }

}
