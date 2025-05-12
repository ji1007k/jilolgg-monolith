package com.test.basic.lol.leagues;

import com.test.basic.lol.sync.LolSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/leagues")
public class LeagueController {

    public final LeagueService leagueService;
    private final LolSyncService lolSyncService;

    public LeagueController(LeagueService leagueService, LolSyncService lolSyncService) {
        this.leagueService = leagueService;
        this.lolSyncService = lolSyncService;
    }

    @GetMapping
    public ResponseEntity<List> getAllLeagues() {
        return ResponseEntity.ok(leagueService.getAllLeagues());
    }

    @GetMapping("/sync")
    public ResponseEntity<List<LeagueDto>> getAllLeaguesFromApi() {
        lolSyncService.syncLeaguesFromLolEsportsApi();
        return ResponseEntity.ok(leagueService.getAllLeagues());
    }

}
