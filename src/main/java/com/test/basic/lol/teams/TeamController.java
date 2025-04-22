package com.test.basic.lol.teams;

import com.test.basic.lol.batch.TeamBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lol/teams")
@Tag(name = "팀 API", description = "팀 관련 API")
public class TeamController {
    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    private final TeamService teamService;
    private final TeamBatchService teamBatchService;

    public TeamController(TeamService teamService, TeamBatchService teamBatchService) {
        this.teamService = teamService;
        this.teamBatchService = teamBatchService;
    }

//    @GetMapping
//    public ResponseEntity<List> getAllTeams() {
//        return ResponseEntity.ok(teamService.getAllTeamsFromDB());
//    }

    // 리스트 필터 조회
    @GetMapping
    public ResponseEntity<List<Team>> getTeams(@RequestParam(required = false) String homeLeague,
                                               // GET /teams?slugs=slug1,slug2 자동 파싱됨
                                               @RequestParam(required = false) List<String> slugs) {
        List<Team> teams = teamService.getTeamsFromDB(homeLeague, slugs);
        return ResponseEntity.ok(teamService.filterLCKFirstTeams(teams));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Team> getTeamBySlug(@PathVariable String slug) {
        try {
            Team foundTeam = teamService.getTeamBySlugFromDB(slug);
            return ResponseEntity.ok(foundTeam);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sync-teams")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "LOL 팀 정보 수동 동기화", description = "LOL 팀 정보 수동 동기화 API")
    public ResponseEntity<String> syncTeams() {
        logger.info("[DEV][팀 수동 동기화] LoL Esports API로부터 팀 정보 동기화 시작");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[DEV][팀 수동 동기화] 팀 정보 동기화 완료");
        return ResponseEntity.ok("팀 수동 동기화 성공");
    }

}
