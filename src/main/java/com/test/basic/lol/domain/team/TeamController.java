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
@Tag(name = "03. Team", description = "LoL Esports 팀 및 선수 정보 조회")
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
    @Operation(summary = "팀 목록 조회", description = "리그 ID 또는 팀 슬러그(slug) 목록으로 팀 정보를 조회합니다.")
    public ResponseEntity<List<TeamDto>> getTeams(@RequestParam(required = false) String leagueId,
                                               // GET /teams?slugs=slug1,slug2 자동 파싱됨
                                               @RequestParam(required = false) List<String> slugs) {
        List<TeamDto> teams = teamService.getTeamsFromDB(leagueId, slugs);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "팀 상세 조회", description = "팀 슬러그를 이용해 특정 팀의 상세 정보 및 선수 로스터를 조회합니다.")
    public ResponseEntity<Team> getTeamBySlug(@PathVariable String slug) {
        try {
            Team foundTeam = teamService.getTeamBySlugFromDB(slug);
            return ResponseEntity.ok(foundTeam);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @SecurityRequirement(name = "02_BearerAuth")
    @Operation(summary = "LOL 팀 정보 수동 동기화", description = "관리자 권한으로 외부 API로부터 모든 팀 및 선수 정보를 동기화합니다.")
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
