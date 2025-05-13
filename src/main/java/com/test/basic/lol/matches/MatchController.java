package com.test.basic.lol.matches;

import com.test.basic.lol.sync.SyncLolEsportsApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequestMapping("/lol/matches")
@RequiredArgsConstructor
@Tag(name = "[LOL] Match API", description = "경기 일정 API")
public class MatchController {
    private final MatchService matchService;
    private final SyncLolEsportsApiService syncLolEsportsApiService;

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

    // TODO 전체 리그 데이터 동기화
    @GetMapping("/sync")
    public Mono<ResponseEntity<List<MatchDto>>> getAllMatchesByLeagueIdFromApi() {
        // LCK, LCK CL, FIRST STAND, MSI, WORLDS
        List<String> leagueIds = List.of(
                "98767991310872058",
                "98767991335774713",
                "113464388705111224",
                "98767991325878492",
                "98767975604431411");

        return syncLolEsportsApiService.syncMatchesByLeagueIds(leagueIds)
                .then(Mono.fromCallable(() -> {
                        List<MatchDto> matches = matchService.getMatchesByLeagueId(leagueIds.get(0));
                        return ResponseEntity.ok(matches);
                    })
                );
    }

}
