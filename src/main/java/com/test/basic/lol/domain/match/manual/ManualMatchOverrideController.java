package com.test.basic.lol.domain.match.manual;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/match-overrides")
@RequiredArgsConstructor
@Tag(name = "[ADMIN] Match Override API", description = "관리자 수동 일정 오버라이드 API")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class ManualMatchOverrideController {

    private final ManualMatchOverrideService manualMatchOverrideService;

    @PutMapping("/{matchId}")
    @Operation(summary = "수동 일정 오버라이드 등록/수정", description = "startTime/blockName 필드 잠금 기반 수동 오버라이드")
    public ResponseEntity<ManualMatchOverrideResponse> upsert(
            @PathVariable String matchId,
            @RequestBody ManualMatchOverrideRequest request,
            Authentication authentication
    ) {
        String actor = resolveActor(authentication);
        return ResponseEntity.ok(manualMatchOverrideService.upsert(matchId, request, actor));
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "수동 오버라이드 조회", description = "matchId 기준 수동 오버라이드 조회")
    public ResponseEntity<ManualMatchOverrideResponse> get(@PathVariable String matchId) {
        return ResponseEntity.ok(manualMatchOverrideService.get(matchId));
    }

    @DeleteMapping("/{matchId}")
    @Operation(summary = "수동 오버라이드 삭제", description = "matchId 기준 수동 오버라이드 삭제")
    public ResponseEntity<Void> delete(@PathVariable String matchId) {
        manualMatchOverrideService.delete(matchId);
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
