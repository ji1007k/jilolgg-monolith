package com.test.basic.lol.matchhistory;

import com.test.basic.lol.matches.MatchDto;
import com.test.basic.lol.matches.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lol/matchhistory")
@Tag(name = "[LOL] Match History API", description = "경기 전적 API")
public class MatchHistoryController {

    private final MatchService matchService;

    public MatchHistoryController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @Operation(summary = "경기 전적 조회", description = "경기 전적 조회 API")
    public ResponseEntity<List<MatchDto>> getHistoryMathces(@RequestBody List<String> matchIds) {
        List<MatchDto> matches = matchService.getMatchesByMatchIds(matchIds);
        return ResponseEntity.ok(matches);
    }

}
