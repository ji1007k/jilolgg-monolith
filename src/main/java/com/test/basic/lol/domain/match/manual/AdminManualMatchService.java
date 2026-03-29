package com.test.basic.lol.domain.match.manual;

import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchCacheService;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.matchteam.MatchTeamRepository;
import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import com.test.basic.lol.domain.tournament.Tournament;
import com.test.basic.lol.domain.tournament.TournamentRepository;
import com.test.basic.lol.sync.MatchSyncOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManualMatchService {

    private static final String STRATEGY_TYPE = "BO";
    private static final Set<Integer> ALLOWED_BEST_OF = Set.of(1, 3, 5);

    private final MatchRepository matchRepository;
    private final LeagueRepository leagueRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final ManualMatchOverrideRepository manualMatchOverrideRepository;
    private final ManualMatchOverrideService manualMatchOverrideService;
    private final MatchCacheService matchCacheService;
    private final RedissonClient redissonClient;

    @Transactional
    public AdminManualMatchUpsertResponse upsert(String matchId, AdminManualMatchUpsertRequest request, String actor) {
        validateRequest(matchId, request);

        RLock lock = redissonClient.getLock(MatchSyncOrchestratorService.GLOBAL_MATCH_SYNC_LOCK_KEY);
        boolean locked = false;

        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 동기화 작업이 실행 중입니다. 잠시 후 다시 시도하세요.");
            }

            League league = leagueRepository.findByLeagueId(request.leagueId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "leagueId를 찾을 수 없습니다: " + request.leagueId()));

            Tournament tournament = tournamentRepository.findByTournamentId(request.tournamentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "tournamentId를 찾을 수 없습니다: " + request.tournamentId()));
            if (!league.getLeagueId().equals(tournament.getLeague().getLeagueId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tournamentId가 leagueId와 일치하지 않습니다.");
            }

            int bestOf = request.bestOf();
            if (!ALLOWED_BEST_OF.contains(bestOf)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bestOf는 1, 3, 5 중 하나만 허용됩니다.");
            }

            List<String> distinctTeamIds = request.teamIds().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

            List<Team> teams = teamRepository.findByTeamIdIn(distinctTeamIds);
            Map<String, Team> teamsById = teams.stream().collect(Collectors.toMap(Team::getTeamId, Function.identity()));
            List<String> missingTeamIds = distinctTeamIds.stream()
                    .filter(teamId -> !teamsById.containsKey(teamId))
                    .toList();

            if (!missingTeamIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId를 찾을 수 없습니다: " + String.join(", ", missingTeamIds));
            }

            Match match = matchRepository.findAllByMatchIdOrderByIdAsc(matchId)
                    .stream()
                    .findFirst()
                    .orElseGet(Match::new);

            match.setMatchId(matchId);
            match.setLeague(league);
            match.setTournament(tournament);
            match.setStartTime(request.startTime());
            match.setBlockName(request.blockName());
            match.setState(StringUtils.hasText(request.state()) ? request.state().trim() : "unstarted");
            match.setGameCount(bestOf);
            match.setStrategy(STRATEGY_TYPE + bestOf);

            Match savedMatch = matchRepository.save(match);

            matchTeamRepository.deleteByMatch_MatchId(savedMatch.getMatchId());
            for (String teamId : distinctTeamIds) {
                MatchTeam matchTeam = new MatchTeam();
                matchTeam.setMatch(savedMatch);
                matchTeam.setTeam(teamsById.get(teamId));
                matchTeamRepository.save(matchTeam);
            }

            boolean requestedLockUpdate = request.lockStartTime() != null || request.lockBlockName() != null;
            if (requestedLockUpdate) {
                ManualMatchOverrideRequest overrideRequest = new ManualMatchOverrideRequest(
                        request.startTime(),
                        request.blockName(),
                        request.lockStartTime(),
                        request.lockBlockName(),
                        true
                );
                manualMatchOverrideService.upsert(savedMatch.getMatchId(), overrideRequest, actor);
            }

            matchCacheService.invalidateAllCaches();

            return new AdminManualMatchUpsertResponse(
                    savedMatch.getMatchId(),
                    savedMatch.getLeague().getLeagueId(),
                    savedMatch.getTournament().getTournamentId(),
                    savedMatch.getStartTime(),
                    savedMatch.getBlockName(),
                    savedMatch.getGameCount(),
                    savedMatch.getStrategy(),
                    savedMatch.getState(),
                    distinctTeamIds,
                    Boolean.TRUE.equals(request.lockStartTime()),
                    Boolean.TRUE.equals(request.lockBlockName())
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void deleteOriginalMatch(String matchId) {
        if (!StringUtils.hasText(matchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matchId는 필수입니다.");
        }

        RLock lock = redissonClient.getLock(MatchSyncOrchestratorService.GLOBAL_MATCH_SYNC_LOCK_KEY);
        boolean locked = false;

        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 동기화 작업이 실행 중입니다. 잠시 후 다시 시도하세요.");
            }

            List<Match> matches = matchRepository.findAllByMatchIdOrderByIdAsc(matchId);
            if (matches.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 matchId가 존재하지 않습니다: " + matchId);
            }

            matchTeamRepository.deleteByMatch_MatchId(matchId);
            manualMatchOverrideRepository.deleteByMatchId(matchId);
            matchRepository.deleteByMatchId(matchId);
            matchCacheService.invalidateAllCaches();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "락 획득 중 인터럽트 발생");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void validateRequest(String matchId, AdminManualMatchUpsertRequest request) {
        if (!StringUtils.hasText(matchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matchId는 필수입니다.");
        }
        if (!StringUtils.hasText(request.leagueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leagueId는 필수입니다.");
        }
        if (!StringUtils.hasText(request.tournamentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tournamentId는 필수입니다.");
        }
        if (request.startTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime은 필수입니다.");
        }
        if (request.bestOf() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bestOf는 필수입니다.");
        }
        if (request.teamIds() == null || request.teamIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamIds는 필수입니다.");
        }

        Set<String> distinctTeamIds = request.teamIds().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (distinctTeamIds.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamIds는 서로 다른 값으로 정확히 2개여야 합니다.");
        }
    }
}
