package com.test.basic.lol.domain.matchgame;

import com.test.basic.lol.api.esports.dto.MatchDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 경기 상세 응답의 세트 목록을 match_games에 upsert한다. 호출자의 트랜잭션에 참여한다. */
@Service
@RequiredArgsConstructor
public class MatchGameSyncService {

    private final MatchGameRepository matchGameRepository;

    public void sync(String matchId, MatchDetailResponse.MatchDto detail) {
        List<MatchDetailResponse.GameDto> games = detail.getGames();
        if (games == null || games.isEmpty()) return;

        Map<String, MatchGame> existing = matchGameRepository.findByMatchIdOrderByNumberAsc(matchId).stream()
                .collect(Collectors.toMap(MatchGame::getGameId, Function.identity(), (a, b) -> a));
        Map<Integer, String> winners = resolveWinners(detail);

        for (MatchDetailResponse.GameDto dto : games) {
            if (dto.getId() == null) continue;

            MatchGame game = existing.getOrDefault(dto.getId(), new MatchGame());
            game.setGameId(dto.getId());
            game.setMatchId(matchId);
            game.setNumber(dto.getNumber());
            game.setState(dto.getState());
            game.setBlueTeamId(sideTeamId(dto, "blue"));
            game.setRedTeamId(sideTeamId(dto, "red"));
            // 이번 응답으로 확정되지 않으면 기존 승자를 유지한다.
            String winner = winners.get(dto.getNumber());
            if (winner != null) {
                game.setWinnerTeamId(winner);
            }
            matchGameRepository.save(game);
        }
    }

    private String sideTeamId(MatchDetailResponse.GameDto game, String side) {
        if (game.getTeams() == null) return null;
        return game.getTeams().stream()
                .filter(t -> side.equalsIgnoreCase(t.getSide()))
                .map(MatchDetailResponse.GameTeamDto::getId)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /**
     * getEventDetails에는 세트별 승패가 없어 시리즈 누적 승수(gameWins)에서 확정 가능한 세트만 계산한다.
     * - 한 팀의 승수가 0이면 완료된 모든 세트는 상대 팀 승리
     * - 시리즈가 끝났다면(한 팀이 과반) 마지막 완료 세트는 시리즈 승자의 승리
     * 그 외(예: 2-1의 1·2세트)는 순서를 알 수 없으므로 포함하지 않는다.
     */
    static Map<Integer, String> resolveWinners(MatchDetailResponse.MatchDto detail) {
        if (detail.getTeams() == null || detail.getTeams().size() != 2 || detail.getGames() == null) {
            return Map.of();
        }
        MatchDetailResponse.TeamDto a = detail.getTeams().get(0);
        MatchDetailResponse.TeamDto b = detail.getTeams().get(1);
        if (a.getResult() == null || b.getResult() == null || a.getId() == null || b.getId() == null) {
            return Map.of();
        }

        List<MatchDetailResponse.GameDto> completed = detail.getGames().stream()
                .filter(g -> "completed".equalsIgnoreCase(g.getState()))
                .sorted((x, y) -> Integer.compare(x.getNumber(), y.getNumber()))
                .toList();
        int aw = a.getResult().getGameWins();
        int bw = b.getResult().getGameWins();
        if (completed.isEmpty() || aw + bw != completed.size()) {
            return Map.of(); // 집계가 맞지 않으면 확정하지 않는다
        }

        Map<Integer, String> winners = new HashMap<>();
        if (bw == 0) {
            completed.forEach(g -> winners.put(g.getNumber(), a.getId()));
        } else if (aw == 0) {
            completed.forEach(g -> winners.put(g.getNumber(), b.getId()));
        } else if (detail.getStrategy() != null) {
            int majority = detail.getStrategy().getCount() / 2 + 1;
            if (aw >= majority || bw >= majority) {
                winners.put(completed.get(completed.size() - 1).getNumber(), aw >= majority ? a.getId() : b.getId());
            }
        }
        return winners;
    }
}
