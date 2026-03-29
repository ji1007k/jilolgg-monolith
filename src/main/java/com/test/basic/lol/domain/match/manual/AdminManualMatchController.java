package com.test.basic.lol.domain.match.manual;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/manual-matches")
@RequiredArgsConstructor
@Tag(name = "[ADMIN] Manual Match API", description = "관리자 수동 일정 등록/수정 API")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class AdminManualMatchController {

    private final AdminManualMatchService adminManualMatchService;

    @PutMapping("/{matchId}")
    @Operation(summary = "수동 일정 저장/수정", description = "리그/토너먼트/팀 정보를 포함해 매치를 저장하고 필요 시 override 락도 갱신")
    public ResponseEntity<AdminManualMatchUpsertResponse> upsertManualMatch(
            @PathVariable String matchId,
            @RequestBody AdminManualMatchUpsertRequest request,
            Authentication authentication
    ) {
        String actor = resolveActor(authentication);
        return ResponseEntity.ok(adminManualMatchService.upsert(matchId, request, actor));
    }

    @DeleteMapping("/{matchId}")
    @Operation(summary = "원본 경기 삭제", description = "matches/match_teams/manual_match_overrides 데이터를 함께 삭제")
    public ResponseEntity<Void> deleteManualMatch(@PathVariable String matchId) {
        adminManualMatchService.deleteOriginalMatch(matchId);
        return ResponseEntity.noContent().build();
    }

    private String resolveActor(Authentication authentication) {
        if (authentication == null) {
            return "system";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            return (email == null || email.isBlank()) ? jwt.getSubject() : email;
        }

        return authentication.getName();
    }
}
