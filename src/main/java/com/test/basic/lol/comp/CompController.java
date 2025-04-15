package com.test.basic.lol.comp;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/lol/comps")
@RequiredArgsConstructor
public class CompController {
    private final CompService compService;

    @GetMapping("/{teamCode}")
    @Operation(summary = "팀 경기 일정 조회", description = "팀 경기 일정 조회 API")
    public ResponseEntity<List<CompDto>> getComps(@PathVariable("teamCode") String teamCode) {
        List<CompDto> comps = compService.getComps(teamCode);
        return ResponseEntity.ok(comps);
    }

    @GetMapping
    @Operation(summary = "경기 일정 전체 조회", description = "경기 일정 전체 조회 API")
    public ResponseEntity<List<CompDto>> getAllComps() {
        List<CompDto> comps = compService.getAllComps();
        return ResponseEntity.ok(comps);
    }
}
