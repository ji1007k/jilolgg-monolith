package com.test.basic.lol.domain.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/lol/matches")
@RequiredArgsConstructor
@Tag(name = "[LOL] Match API", description = "경기 일정 API")
@Slf4j
public class MatchController {
    private final MatchService matchService;
    private final SyncMatchService syncMatchService;
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


    @GetMapping
    @Operation(summary = "경기 일정 목록 조회", description = "리그/날짜별 경기 일정 목록 조회 API")
    @Parameters({
            @Parameter(name = "leagueId", description = "리그ID"),
            @Parameter(name = "startDate", description = "시작일"),
            @Parameter(name = "endDate", description = "종료일")
    })
    public ResponseEntity<List<MatchDto>> getMatches(@RequestParam String leagueId,
                                                     @RequestParam LocalDate startDate,
                                                     @RequestParam LocalDate endDate) {
        List<MatchDto> matches = matchService.getMatchesByLeagueIdAndDate(
                leagueId,
                startDate,
                endDate
        );
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/team/{name}")
    @Operation(summary = "팀별 경기 일정 조회", description = "팀명으로 경기 일정 조회 API")
    public ResponseEntity<List<MatchDto>> getMatchesByTeamName(@PathVariable("name") String name) {
        List<MatchDto> matches = matchService.getMatchesByTeamName(name);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/sync")
    @Operation(summary = "리그별 경기일정 수동 동기화", description = "리그별 경기일정 수동 동기화 API")
    public ResponseEntity syncAllMatchesByLeagueIdFromApi(@RequestParam(required = false) String year) {

        // 소요 시간 측정
        StopWatch sw = new StopWatch();
        sw.start();

        for (String leagueId : MAJOR_LEAGUE_IDS) {
            syncMatchService.syncMatchesByLeagueIdAndYearExternalApi(leagueId, year);
        }

        sw.stop();
        log.info(">>> 소요 시간: {}ms", sw.getTotalTimeMillis());

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
