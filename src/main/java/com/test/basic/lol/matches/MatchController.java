package com.test.basic.lol.matches;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TODO
//  - 비동기 처리 (컨트롤러에서도 @Async나 WebFlux로 비동기 처리 가능)
//  - .block() 대신 .subscribe()나 Mono<List<MatchDto>>로 리턴


@RestController
@RequestMapping("/lol/matches")
@RequiredArgsConstructor
@Tag(name = "[LOL] 경기 일정 API", description = "경기 일정 API")
public class MatchController {
    private final MatchService matchService;

    @GetMapping("/{teamName}")
    @Operation(summary = "팀 경기 일정 조회", description = "팀명으로 경기 일정 조회 API")
    public ResponseEntity<List<MatchDto>> getMatches(@PathVariable("teamName") String teamName) {
        List<MatchDto> matches = matchService.getMatchesByName(teamName);
        return ResponseEntity.ok(matches);
    }

    @GetMapping
    @Operation(summary = "전체 경기 일정 조회", description = "전체 경기 일정 조회 API")
    public ResponseEntity<List<MatchDto>> getAllMatches() {
        List<MatchDto> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }
}
