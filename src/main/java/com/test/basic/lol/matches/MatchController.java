package com.test.basic.lol.matches;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/lol/matches")
@RequiredArgsConstructor
@Tag(name = "[LOL] 경기 일정 API", description = "경기 일정 API")
public class MatchController {
    private final MatchService matchService;

    // TODO 연도 -> 날짜 검색
    @GetMapping
    public ResponseEntity<List<MatchDto>> getMatches(@RequestParam(required = false) String year,
                                                     @RequestParam(required = false) String leagueId) {
        List<MatchDto> matches = matchService.getMatches(year, leagueId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/team/{name}")
    @Operation(summary = "팀별 경기 일정 조회", description = "팀명으로 경기 일정 조회 API")
    public ResponseEntity<List<MatchDto>> getMatchesByTeamName(@PathVariable("name") String name) {
        List<MatchDto> matches = matchService.getMatchesByTeamName(name);
        return ResponseEntity.ok(matches);
    }

}
