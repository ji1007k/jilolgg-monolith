package com.test.basic.lol.domain.match;

import com.test.basic.lol.domain.match.mapping.MatchExternalMapping;
import com.test.basic.lol.domain.match.mapping.MatchExternalMappingRepository;
import com.test.basic.lol.domain.match.mapping.MatchExternalMappingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class MatchService {
    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final MatchApiService matchApiService;
    private final MatchCacheService matchCacheService;
    private final MatchMapper matchMapper;
    private final MatchRepository matchRepository;
    private final MatchExternalMappingRepository matchExternalMappingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public MatchService(
            MatchMapper matchMapper,
            MatchRepository matchRepository,
            MatchCacheService matchCacheService,
            MatchApiService matchApiService,
            MatchExternalMappingRepository matchExternalMappingRepository
    ) {
        this.matchMapper = matchMapper;
        this.matchRepository = matchRepository;
        this.matchCacheService = matchCacheService;
        this.matchApiService = matchApiService;
        this.matchExternalMappingRepository = matchExternalMappingRepository;
    }

    public List<MatchDto> getAllMatches() {
        List<MatchDto> cached = matchCacheService.getMemoryCachedMatches();
        if (cached != null) return cached;

        // TTL 지났거나 최초 요청이면 새로 로딩
        List<MatchDto> freshMatches = matchApiService.fetchAllMatches();
        matchCacheService.setMemoryCachedMatches(freshMatches);
        return freshMatches;
    }

    public List<MatchDto> getMatchesByLeagueId(String leagueId) {
        List<MatchDto> cached = matchCacheService.getMemoryCachedMatches();
        if (cached != null) return cached;  // 아직 TTL 안 지났으면 캐시 데이터 사용

        // TTL 지났거나 최초 요청이면 새로 로딩
        List<MatchDto> freshMatches = matchApiService.fetchMatchesByLeague(leagueId);
        matchCacheService.setMemoryCachedMatches(freshMatches);
        return freshMatches;
    }

    public List<MatchDto> getMatchesByTeamName(String name) {
        return getAllMatches().stream()
                .filter(dto -> dto.getParticipants().stream()
                        .anyMatch(matchTeamDto -> matchTeamDto
                                .getTeam().getName()
                                .equalsIgnoreCase(name)))
                .collect(Collectors.toList());
    }
    public List<Match> getMatchesByDate(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return matchRepository.findMatchesByDate(startOfDay, endOfDay);
    }

    @Cacheable(value = "firstMatchTime", key = "#startOfDay + '_' + #endOfDay", unless = "#result == null")
    public LocalDateTime getFirstMatchTimeOfDay(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return matchRepository.findFirstMatchTimeOfDay(startOfDay, endOfDay).orElse(null);
    }

    public List<MatchDto> getMatchesByLeagueIdAndDate(String leagueId, LocalDate startDate, LocalDate endDate) {
        logger.info("==================== [경기 일정 조회 시작] ====================");
        StopWatch sw = new StopWatch(); sw.start();

        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfDay = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<MatchDto> result = getMatchesWithCache(leagueId, startDate, endDate, () ->
                matchRepository.findMatchByLeagueIdAndDate(leagueId, startOfDay, endOfDay)
        );

        sw.stop();
        logger.info(">>> 소요 시간: {}ms", sw.getTotalTimeMillis());
        logger.info("==================== [경기 일정 조회 완료] ====================");
        return result;
    }

    public List<MatchDto> getMatchesWithCache(
            String leagueId,
            LocalDate startDate,
            LocalDate endDate,
            Supplier<List<Match>> dbFallback
    ) {
        String redisKey = String.join(":", "match", leagueId, startDate.toString(), endDate.toString());
        List<MatchDto> cached = matchCacheService.getCachedMatchList(redisKey);

        if (cached != null) {
            logger.info(">>> Redis 캐시 Hit: {}", redisKey);
            return dedupeForDisplay(cached);
        }

        logger.info(">>> Redis 캐시 Miss. DB 조회 시작: {}", redisKey);

        List<Match> matches = dbFallback.get();
        List<MatchDto> dtos = matches.stream().map(matchMapper::entityToDto).toList();
        List<MatchDto> deduped = dedupeForDisplay(dtos);
        matchCacheService.cacheMatchList(redisKey, deduped);

        return deduped;
    }

    public List<MatchDto> getMatchesByMatchIds(List<String> matchIds) {
        return matchRepository.findByMatchIdIn(matchIds)
                .stream()
                .map(matchMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public List<Match> getMatchEntitiesByMatchIds(Set<String> matchIds) {
        return matchRepository.findByMatchIdIn(matchIds);
    }

    public List<Match> saveMatches(List<Match> matchesToSave) {
        return matchRepository.saveAll(matchesToSave);
    }

    private List<MatchDto> dedupeForDisplay(List<MatchDto> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        List<String> ids = source.stream()
                .map(MatchDto::getMatchId)
                .filter(StringUtils::hasText)
                .toList();

        if (ids.isEmpty()) {
            return source;
        }

        Map<String, MatchExternalMapping> mappingByExternalId = matchExternalMappingRepository
                .findAllByProviderAndExternalMatchIdIn(MatchExternalMappingService.PROVIDER_LOL_ESPORTS, ids)
                .stream()
                .collect(Collectors.toMap(
                        MatchExternalMapping::getExternalMatchId,
                        mapping -> mapping,
                        (a, b) -> a
                ));

        if (mappingByExternalId.isEmpty()) {
            return source;
        }

        Map<String, MatchDto> grouped = new LinkedHashMap<>();
        Map<String, Boolean> canonicalPresent = new LinkedHashMap<>();

        for (MatchDto dto : source) {
            if (dto == null || !StringUtils.hasText(dto.getMatchId())) {
                continue;
            }

            String originalId = dto.getMatchId();
            MatchExternalMapping mapping = mappingByExternalId.get(originalId);
            String targetId = mapping == null ? originalId : mapping.getMatchId();

            MatchDto normalized = cloneDto(dto);
            normalized.setMatchId(targetId);

            boolean sourceIsCanonical = Objects.equals(originalId, targetId);
            MatchDto existing = grouped.get(targetId);

            if (existing == null) {
                grouped.put(targetId, normalized);
                canonicalPresent.put(targetId, sourceIsCanonical);
                continue;
            }

            boolean hasCanonical = Boolean.TRUE.equals(canonicalPresent.get(targetId));
            if (sourceIsCanonical && !hasCanonical) {
                grouped.put(targetId, mergePreferLeft(normalized, existing));
                canonicalPresent.put(targetId, true);
            } else {
                grouped.put(targetId, mergePreferLeft(existing, normalized));
                canonicalPresent.put(targetId, hasCanonical || sourceIsCanonical);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    private MatchDto cloneDto(MatchDto src) {
        MatchDto copy = new MatchDto();
        copy.setMatchId(src.getMatchId());
        copy.setStartTime(src.getStartTime());
        copy.setState(src.getState());
        copy.setStrategy(src.getStrategy());
        copy.setBlockName(src.getBlockName());
        copy.setWinningTeamCode(src.getWinningTeamCode());
        copy.setParticipants(src.getParticipants());
        return copy;
    }

    private MatchDto mergePreferLeft(MatchDto left, MatchDto right) {
        MatchDto merged = new MatchDto();
        merged.setMatchId(StringUtils.hasText(left.getMatchId()) ? left.getMatchId() : right.getMatchId());
        merged.setStartTime(StringUtils.hasText(left.getStartTime()) ? left.getStartTime() : right.getStartTime());
        merged.setState(StringUtils.hasText(left.getState()) ? left.getState() : right.getState());
        merged.setStrategy(StringUtils.hasText(left.getStrategy()) ? left.getStrategy() : right.getStrategy());
        merged.setBlockName(StringUtils.hasText(left.getBlockName()) ? left.getBlockName() : right.getBlockName());
        merged.setWinningTeamCode(StringUtils.hasText(left.getWinningTeamCode()) ? left.getWinningTeamCode() : right.getWinningTeamCode());
        merged.setParticipants(left.getParticipants() != null && !left.getParticipants().isEmpty()
                ? left.getParticipants()
                : right.getParticipants());
        return merged;
    }


    // 250526 미사용. 참고용. ==================================================

    // 양방향 연관관계로 인한 순환참조 이슈 해결 방법 더 알아보기
    //  -> findMatchByLeagueIdAndDate 메서드 사용으로 변경
    /*public List<MatchDto> getMatchesFromDB(String year, String leagueId) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM Match m WHERE 1 = 1");

        if (year != null) {
            jpql.append(" AND FUNCTION('date_part', 'year', m.startTime) = :year");
        }
        if (leagueId != null) {
            jpql.append(" AND m.league.leagueId = :leagueId");
        }

        // JPQL 쿼리 생성
        TypedQuery<Match> query = entityManager.createQuery(jpql.toString(), Match.class);

        // 파라미터 설정
        if (year != null) {
            query.setParameter("year", Integer.parseInt(year)); // YEAR 함수는 정수로 비교
        }
        if (leagueId != null) {
            query.setParameter("leagueId", leagueId);
        }

        // 결과 반환
        List<Match> matches = query.getResultList();

        return matches.stream()
                .map(matchMapper::entityToDto)
                .toList();
    }*/


}
