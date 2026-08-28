package com.test.basic.lol.domain.match;

import com.test.basic.lol.domain.match.mapping.MatchExternalMapping;
import com.test.basic.lol.domain.matchteam.MatchTeamDto;
import com.test.basic.lol.domain.team.TeamDto;
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

    public Optional<LocalDateTime> getFirstMatchTimeOfDay(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return matchRepository.findFirstMatchTimeOfDay(startOfDay, endOfDay);
    }

    public boolean hasLiveMatches(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return matchRepository.findMatchesByDate(startOfDay, endOfDay).stream()
                .anyMatch(m -> "inProgress".equalsIgnoreCase(m.getState()));
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
            return hideSupersededPlaceholders(dedupeForDisplay(cached));
        }

        logger.info(">>> Redis 캐시 Miss. DB 조회 시작: {}", redisKey);

        List<Match> matches = dbFallback.get();
        List<MatchDto> dtos = matches.stream().map(matchMapper::entityToDto).toList();
        List<MatchDto> deduped = hideSupersededPlaceholders(dedupeForDisplay(dtos));
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

    /** 외부 API가 대진 확정 전 자리를 채워두는 플레이스홀더 팀. 코드는 TBDC지만 name/slug는 TBD다. */
    private static final String PLACEHOLDER_TEAM_SLUG = "tbd";

    /**
     * 대진이 확정되면서 밀려난 플레이스홀더 경기를 화면에서 감춘다.
     *
     * 외부 API는 플레이오프/플레이-인 대진표 자리와 실제 편성 경기에 서로 다른 matchId를 발급한다.
     * 확정되면 API 응답에서 자리 쪽은 사라지지만, 동기화가 삽입·갱신만 하고 삭제는 하지 않아
     * DB에는 옛 자리가 남는다. 그 결과 같은 시각에 "TBD vs TBD"와 실제 경기가 함께 노출된다.
     *
     * 같은 시각에 실제 경기가 있을 때만 자리를 숨긴다.
     * 아직 대진이 안 잡힌 자리는 "이 시간에 경기가 예정돼 있다"는 정보이므로 그대로 둔다.
     *
     * 데이터는 건드리지 않는다. DB 정리는 동기화 쪽에서 따로 다룬다.
     */
    private List<MatchDto> hideSupersededPlaceholders(List<MatchDto> source) {
        if (source == null || source.size() < 2) {
            return source;
        }

        // 같은 시각에 실제 경기가 하나라도 있는지
        Set<String> slotsWithRealMatch = source.stream()
                .filter(dto -> dto != null && !isPlaceholderMatch(dto))
                .map(MatchDto::getStartTime)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (slotsWithRealMatch.isEmpty()) {
            return source;
        }

        return source.stream()
                .filter(dto -> {
                    if (dto == null || !isPlaceholderMatch(dto)) {
                        return true;
                    }

                    boolean superseded = slotsWithRealMatch.contains(dto.getStartTime());
                    if (superseded) {
                        logger.debug(">>> 확정 경기에 밀려난 플레이스홀더 숨김: matchId={}, startTime={}",
                                dto.getMatchId(), dto.getStartTime());
                    }
                    return !superseded;
                })
                .collect(Collectors.toList());
    }

    /** 참가 팀이 없거나 전부 TBD면 아직 대진이 확정되지 않은 자리로 본다. */
    private boolean isPlaceholderMatch(MatchDto dto) {
        List<MatchTeamDto> participants = dto.getParticipants();

        // 참가 팀이 아예 없는 경기는 플레이스홀더가 아니다.
        // 대진표 자리가 아니라 팀 데이터가 유실된 실제 경기이며(현재 1,248건),
        // 같은 시각에 경기가 여러 개인 리그에서는 이걸 자리로 오인하면
        // 멀쩡한 완료 경기까지 화면에서 사라진다.
        if (participants == null || participants.isEmpty()) {
            return false;
        }

        return participants.stream().allMatch(participant -> {
            TeamDto team = participant == null ? null : participant.getTeam();
            // 코드(TBDC)는 팀마다 다를 수 있어 slug로 판별한다. TB(Team Bliss) 같은 실제 팀과 섞이면 안 된다.
            return team != null && PLACEHOLDER_TEAM_SLUG.equalsIgnoreCase(team.getSlug());
        });
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
