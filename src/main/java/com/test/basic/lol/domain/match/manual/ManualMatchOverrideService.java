package com.test.basic.lol.domain.match.manual;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchApiService;
import com.test.basic.lol.domain.match.MatchCacheService;
import com.test.basic.lol.domain.match.MatchRepository;
import com.test.basic.lol.sync.MatchSyncOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualMatchOverrideService {

    private final ManualMatchOverrideRepository manualMatchOverrideRepository;
    private final MatchRepository matchRepository;
    private final MatchApiService matchApiService;
    private final MatchCacheService matchCacheService;
    private final RedissonClient redissonClient;

    @Transactional
    public ManualMatchOverrideResponse upsert(String matchId, ManualMatchOverrideRequest request, String actor) {
        RLock lock = redissonClient.getLock(MatchSyncOrchestratorService.GLOBAL_MATCH_SYNC_LOCK_KEY);
        boolean locked = false;

        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "동기화 작업이 실행 중입니다. 잠시 후 다시 시도하세요.");
            }

            ManualMatchOverride override = manualMatchOverrideRepository.findByMatchId(matchId)
                    .orElseGet(ManualMatchOverride::new);

            override.setMatchId(matchId);
            override.setUpdatedBy(actor);

            boolean lockStartTime = request.lockStartTime() != null && request.lockStartTime();
            boolean lockBlockName = request.lockBlockName() != null && request.lockBlockName();
            boolean applyImmediately = request.applyImmediately() == null || request.applyImmediately();

            if (request.startTime() != null) {
                override.setOverrideStartTime(request.startTime());
            }
            if (request.blockName() != null) {
                String normalizedBlockName = request.blockName().isBlank() ? null : request.blockName();
                override.setOverrideBlockName(normalizedBlockName);
            }

            override.setLockStartTime(lockStartTime);
            override.setLockBlockName(lockBlockName);

            if (override.isLockStartTime() && override.getOverrideStartTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lockStartTime=true이면 startTime 값이 필요합니다.");
            }
            if (override.isLockBlockName() && (override.getOverrideBlockName() == null || override.getOverrideBlockName().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lockBlockName=true이면 blockName 값이 필요합니다.");
            }

            ManualMatchOverride saved = manualMatchOverrideRepository.save(override);
            boolean applied = false;

            if (applyImmediately) {
                applied = applyToCurrentMatch(saved, request);
            }

            return new ManualMatchOverrideResponse(
                    saved.getMatchId(),
                    saved.getOverrideStartTime(),
                    saved.getOverrideBlockName(),
                    saved.isLockStartTime(),
                    saved.isLockBlockName(),
                    saved.getUpdatedBy(),
                    saved.getUpdatedAt(),
                    applied
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "락 획득 중 인터럽트 발생");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public ManualMatchOverrideResponse get(String matchId) {
        ManualMatchOverride override = manualMatchOverrideRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수동 오버라이드가 존재하지 않습니다."));

        return new ManualMatchOverrideResponse(
                override.getMatchId(),
                override.getOverrideStartTime(),
                override.getOverrideBlockName(),
                override.isLockStartTime(),
                override.isLockBlockName(),
                override.getUpdatedBy(),
                override.getUpdatedAt(),
                false
        );
    }

    @Transactional
    public void delete(String matchId) {
        RLock lock = redissonClient.getLock(MatchSyncOrchestratorService.GLOBAL_MATCH_SYNC_LOCK_KEY);
        boolean locked = false;

        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "동기화 작업이 실행 중입니다. 잠시 후 다시 시도하세요.");
            }

            manualMatchOverrideRepository.deleteByMatchId(matchId);
            restoreMatchFieldsFromExternalSchedule(matchId);
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

    @Transactional(readOnly = true)
    public void applyLockedFields(Match match) {
        Optional<ManualMatchOverride> optional = manualMatchOverrideRepository.findByMatchId(match.getMatchId());
        if (optional.isEmpty()) {
            return;
        }

        ManualMatchOverride override = optional.get();
        if (override.isLockStartTime() && override.getOverrideStartTime() != null) {
            match.setStartTime(override.getOverrideStartTime());
        }
        if (override.isLockBlockName() && override.getOverrideBlockName() != null) {
            match.setBlockName(override.getOverrideBlockName());
        }
    }

    private boolean applyToCurrentMatch(ManualMatchOverride override, ManualMatchOverrideRequest request) {
        Optional<Match> optionalMatch = matchRepository.findByMatchId(override.getMatchId());
        if (optionalMatch.isEmpty()) {
            return false;
        }

        Match match = optionalMatch.get();

        // Apply explicit user input immediately.
        if (request.startTime() != null) {
            match.setStartTime(request.startTime());
        }
        if (request.blockName() != null) {
            match.setBlockName(request.blockName().isBlank() ? null : request.blockName());
        }

        // If lock is enabled, override values win.
        applyLockedFields(match);
        matchRepository.save(match);
        matchCacheService.invalidateAllCaches();
        log.info("수동 오버라이드 즉시 반영 완료: matchId={}", override.getMatchId());
        return true;
    }

    private void restoreMatchFieldsFromExternalSchedule(String matchId) {
        Optional<Match> optional = matchRepository.findByMatchId(matchId);
        if (optional.isEmpty()) {
            return;
        }

        Match match = optional.get();
        if (match.getLeague() == null || match.getLeague().getLeagueId() == null) {
            return;
        }

        String leagueId = match.getLeague().getLeagueId();
        String nextPageToken = null;
        Set<String> visitedTokens = new HashSet<>();

        while (true) {
            MatchScheduleResponse response = matchApiService.fetchScheduleByLeagueIdAndPageToken(leagueId, nextPageToken);
            if (response == null || response.getData() == null || response.getData().getSchedule() == null) {
                break;
            }

            if (response.getData().getSchedule().getEvents() != null) {
                for (MatchScheduleResponse.EventDto event : response.getData().getSchedule().getEvents()) {
                    if (event.getMatch() == null || event.getMatch().getId() == null) {
                        continue;
                    }
                    if (!matchId.equals(event.getMatch().getId())) {
                        continue;
                    }

                    if (event.getStartTime() != null) {
                        LocalDateTime eventDateTime = OffsetDateTime.parse(event.getStartTime())
                                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                .toLocalDateTime();
                        match.setStartTime(eventDateTime);
                    }

                    match.setState(event.getState());
                    match.setBlockName(event.getBlockName());

                    if (event.getMatch().getStrategy() != null) {
                        int count = event.getMatch().getStrategy().getCount();
                        String type = event.getMatch().getStrategy().getType();
                        match.setGameCount(count);
                        match.setStrategy(type + count);
                    }

                    matchRepository.save(match);
                    log.info("오버라이드 삭제 후 외부 일정 기준 복원 완료: matchId={}", matchId);
                    return;
                }
            }

            String older = response.getData().getSchedule().getPages() == null
                    ? null
                    : response.getData().getSchedule().getPages().getOlder();

            if (older == null || !visitedTokens.add(older)) {
                break;
            }
            nextPageToken = older;
        }

        log.warn("오버라이드 삭제 후 외부 일정에서 매치를 찾지 못해 원복을 건너뜀: matchId={}", matchId);
    }
}
