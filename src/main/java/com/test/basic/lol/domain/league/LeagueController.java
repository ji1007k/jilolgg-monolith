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
            // LPL, LEC, LJL
            "98767991314006698",
            "98767991302996019",
            "98767991349978712");


    public LeagueController(LeagueService leagueService, SyncLolEsportsApiService syncLolEsportsApiService) {
        this.leagueService = leagueService;
        this.syncLolEsportsApiService = syncLolEsportsApiService;
    }

    @GetMapping
    public ResponseEntity<List<LeagueDto>> getAllLeagues() {
        Long userId = getUserIdFromAuthentication();
        return ResponseEntity.ok(leagueService.getAllLeagues(userId).stream()
//                .filter(league -> MAJOR_LEAGUE_IDS.contains(league.getLeagueId()))
                .toList());
    }

    @PutMapping("/orders")
    @PreAuthorize("isAuthenticated()")
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
