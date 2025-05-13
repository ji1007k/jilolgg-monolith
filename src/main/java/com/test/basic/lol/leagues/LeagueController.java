package com.test.basic.lol.leagues;

import com.test.basic.lol.sync.LolSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/leagues")
@Tag(name = "[LOL] League API", description = "리그 관련 API")
public class LeagueController {

    public final LeagueService leagueService;
    private final LolSyncService lolSyncService;

    // LCK, LCK CL, LPL, LEC, ...
    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            "98767991310872058",
            "98767991335774713",
            "98767991314006698",
            "98767991302996019",
            "98767991349978712",
            "98767991299243165");


    public LeagueController(LeagueService leagueService, LolSyncService lolSyncService) {
        this.leagueService = leagueService;
        this.lolSyncService = lolSyncService;
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
        lolSyncService.syncLeaguesFromLolEsportsApi();
        return ResponseEntity.ok(leagueService.getAllLeagues());
    }

}
