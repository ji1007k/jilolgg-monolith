package com.test.basic.lol.teams;

import com.test.basic.lol.batch.TeamBatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/sync-teams")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> syncTeams() {
        logger.info("[DEV][팀 동기화] LoL Esports API로부터 팀 정보 동기화 시작");
        teamBatchService.syncTeamsFromLolEsports();
        logger.info("[DEV][팀 동기화] 팀 정보 동기화 완료");
        return ResponseEntity.ok("팀 동기화 성공");
    }
}
