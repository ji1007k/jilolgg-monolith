package com.test.basic.lol.matches;

import com.test.basic.lol.sync.SyncLolEsportsApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/lol/matches")
@RequiredArgsConstructor
@Tag(name = "[LOL] Match API", description = "경기 일정 API")
public class MatchController {
    private final MatchService matchService;
//    private final SyncLolEsportsApiService syncLolEsportsApiService;

    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            // LCK, LCK CL
            "98767991310872058",
            "98767991335774713",
            // 국제 대회 (FIRST STAND, MSI, WORLDS)
            "113464388705111224",
            "98767991325878492",
            "98767975604431411",
            // LPL, LEC, ...
            "98767991314006698",
            "98767991302996019",
            "98767991349978712",
            "98767991299243165");


    // TODO 연도 -> 날짜 검색
    @GetMapping
    public ResponseEntity<List<MatchDto>> getMatches(@RequestParam(required = false) String year,
                                                     @RequestParam(required = false) String leagueId) {
        List<MatchDto> matches = matchService.getMatchesFromDB(year, leagueId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/team/{name}")
    @Operation(summary = "팀별 경기 일정 조회", description = "팀명으로 경기 일정 조회 API")
    public ResponseEntity<List<MatchDto>> getMatchesByTeamName(@PathVariable("name") String name) {
        List<MatchDto> matches = matchService.getMatchesByTeamName(name);
        return ResponseEntity.ok(matches);
    }

    //  TODO 금일 경기가 있는 리그 데이터 30분 ~ 1시간 간격으로 동기화
    @GetMapping("/sync")
    @Operation(summary = "리그별 경기일정 동기화", description = "리그별 경기일정 동기화 API")
    public ResponseEntity syncAllMatchesByLeagueIdFromApi(@RequestParam(required = false) String year) {
        matchService.syncMatchesByExternalApi(MAJOR_LEAGUE_IDS, year);
        return ResponseEntity.ok("리그별 경기 일정 동기화 완료");

    }

    // 해당 API는 단일 페이지 데이터만 동기화함
    /*@GetMapping("/sync")
    public Mono<ResponseEntity<List<MatchDto>>> syncAllMatchesByLeagueIdFromApi() {
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
    }*/

}
