package com.test.basic.lol.domain.league;

import com.test.basic.lol.api.esports.SyncLolEsportsApiService;
import com.test.basic.auth.security.user.CustomUserDetails;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/leagues")
@Tag(name = "04. League", description = "LoL Esports 리그 정보 조회 및 설정")
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
            // LPL, LEC, LJL
            "98767991314006698",
            "98767991302996019",
            "98767991349978712");


    public LeagueController(LeagueService leagueService, SyncLolEsportsApiService syncLolEsportsApiService) {
        this.leagueService = leagueService;
        this.syncLolEsportsApiService = syncLolEsportsApiService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "리그 목록 조회", description = "DB에 저장된 모든 리그 정보를 조회합니다. 로그인 시 사용자 맞춤 순서가 적용됩니다.")
    public ResponseEntity<List<LeagueDto>> getAllLeagues() {
        Long userId = getUserIdFromAuthentication();
        return ResponseEntity.ok(leagueService.getAllLeagues(userId).stream()
//                .filter(league -> MAJOR_LEAGUE_IDS.contains(league.getLeagueId()))
                .toList());
    }

    @PutMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "02_BearerAuth")
    @io.swagger.v3.oas.annotations.Operation(summary = "리그 노출 순서 변경", description = "사용자별로 리그 목록의 노출 순서를 저장합니다.")
    public ResponseEntity<Void> updateLeagueOrders(@RequestBody List<String> leagueIds) {
        Long userId = getUserIdFromAuthentication();
        if (userId != null) {
            leagueService.updateLeagueOrders(userId, leagueIds);
        }
        return ResponseEntity.ok().build();
    }


    @Timed(value = "lol.batch.league", description = "리그 동기화 실행 시간")
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "02_BearerAuth")
    @io.swagger.v3.oas.annotations.Operation(summary = "리그 정보 수동 동기화", description = "관리자 권한으로 외부 API로부터 최신 리그 정보를 동기화합니다.")
    public ResponseEntity<List<LeagueDto>> getAllLeaguesFromApi() {
        syncLolEsportsApiService.syncLeaguesFromLolEsportsApi();
        return ResponseEntity.ok(leagueService.getAllLeagues());
    }

    private Long getUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof Jwt) {
                // JWT 인증인 경우 (Access Token)
                String subject = ((Jwt) principal).getSubject();
                try {
                    return Long.valueOf(subject);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (principal instanceof CustomUserDetails) {
                // CustomUserDetails 인증인 경우 (Refresh Token 등)
                return ((CustomUserDetails) principal).getId();
            }
        }
        return null;
    }

}
