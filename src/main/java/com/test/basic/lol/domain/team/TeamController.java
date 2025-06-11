package com.test.basic.lol.domain.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lol/teams")
@Tag(name = "[LOL(Esports)] 3. Team API", description = "팀 관련 API")
public class TeamController {
    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    private final TeamService teamService;
    private final SyncTeamService syncTeamService;

    public TeamController(TeamService teamService, SyncTeamService syncTeamService) {
        this.teamService = teamService;
        this.syncTeamService = syncTeamService;
    }

//    @GetMapping
//    public ResponseEntity<List> getAllTeams() {
//        return ResponseEntity.ok(teamService.getAllTeamsFromDB());
//    }

    // 리스트 필터 조회
    @GetMapping
    public ResponseEntity<List<TeamDto>> getTeams(@RequestParam(required = false) String leagueId,
                                               // GET /teams?slugs=slug1,slug2 자동 파싱됨
                                               @RequestParam(required = false) List<String> slugs) {
        List<TeamDto> teams = teamService.getTeamsFromDB(leagueId, slugs);
        return ResponseEntity.ok(teams);
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


    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    @SecurityRequirement(name = "IgnoreCSRF")
    @SecurityRequirement(name = "CSRF")
    @Operation(summary = "LOL 팀 정보 수동 동기화", description = "LOL 팀 정보 수동 동기화 API")
    public ResponseEntity<String> syncTeams() {
        logger.info("==================== 팀 정보 수동 동기화 작업 시작 ====================");
        String result = syncTeamService.syncTeamsFromLolEsportsApi();

        if (result.contains("실패")) {
            logger.error(">>> 동기화 실패: {}", result);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

        logger.info(">>> 동기화 결과: {}", result);
        logger.info("==================== 팀 정보 수동 동기화 작업 완료 ====================");
        return ResponseEntity.ok(result);
    }

}
