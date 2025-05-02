package com.test.basic.lol.teams.favorites;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lol/favorites")
@RequiredArgsConstructor
@Tag(name = "[LOL] 팀 즐겨찾기 API", description = "팀 즐겨찾기 관련 API")
public class FavoriteTeamController {

    private final FavoriteTeamService favoriteTeamService;

    // 1. 즐겨찾기 등록
    @PostMapping("/{teamId}")
    public ResponseEntity<String> addFavorite(@PathVariable String teamId,
                                              @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getClaim("sub"));

        try {
            favoriteTeamService.addFavoriteTeam(userId, Long.parseLong(teamId));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("즐겨찾는 팀 등록 실패: " + e.getMessage());
        }
    }

    // 2. 즐겨찾기 목록 조회
    @GetMapping
    public ResponseEntity<List<FavoriteTeamResponse>> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getClaim("sub"));
        List<FavoriteTeamResponse> favorites = favoriteTeamService.getFavoriteTeams(userId);
        return ResponseEntity.ok(favorites);
    }

    // 3. 즐겨찾기 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable String teamId,
                                               @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        favoriteTeamService.removeFavoriteTeam(userId, Long.parseLong(teamId));
        return ResponseEntity.noContent().build();
    }

    // 4. 즐겨찾기 순서 변경
    /*@PatchMapping("/order")
    public ResponseEntity<Void> updateOrder(@RequestBody FavoriteOrderRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        favoriteTeamService.updateFavoriteOrder(userId, request.getTeamCodeList());
        return ResponseEntity.ok().build();
    }
*/
}