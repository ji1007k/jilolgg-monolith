package com.test.basic.lol.domain.match.mapping;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchCacheService;
import com.test.basic.lol.domain.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchExternalMappingService {

    public static final String PROVIDER_LOL_ESPORTS = "LOL_ESPORTS";

    private final MatchExternalMappingRepository matchExternalMappingRepository;
    private final MatchRepository matchRepository;
    private final MatchCacheService matchCacheService;

    @Transactional
    public String resolveCanonicalMatchId(String externalMatchId) {
        if (!StringUtils.hasText(externalMatchId)) {
            return externalMatchId;
        }

        return matchExternalMappingRepository.findByProviderAndExternalMatchId(PROVIDER_LOL_ESPORTS, externalMatchId)
                .map(mapping -> resolveCanonicalFromMapping(mapping, externalMatchId))
                .orElse(externalMatchId);
    }

    @Transactional(readOnly = true)
    public String resolveExternalMatchId(String canonicalMatchId) {
        if (!StringUtils.hasText(canonicalMatchId)) {
            return canonicalMatchId;
        }
        return matchExternalMappingRepository.findFirstByProviderAndMatchId(PROVIDER_LOL_ESPORTS, canonicalMatchId)
                .map(MatchExternalMapping::getExternalMatchId)
                .filter(StringUtils::hasText)
                .orElse(canonicalMatchId);
    }

    @Transactional
    public MatchExternalMapping linkExternalMatch(String canonicalMatchId, String externalMatchId, String actor) {
        if (!StringUtils.hasText(canonicalMatchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "canonical matchId는 필수입니다.");
        }
        if (!StringUtils.hasText(externalMatchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalMatchId는 필수입니다.");
        }

        Match canonicalMatch = matchRepository.findByMatchId(canonicalMatchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기준 matchId를 찾을 수 없습니다: " + canonicalMatchId));

        MatchExternalMapping saved = saveMapping(canonicalMatch.getMatchId(), externalMatchId, actor);
        matchCacheService.invalidateAllCaches();
        return saved;
    }

    @Transactional
    public void unlinkExternalMatch(String canonicalMatchId, String externalMatchId) {
        MatchExternalMapping mapping = matchExternalMappingRepository.findByProviderAndExternalMatchId(PROVIDER_LOL_ESPORTS, externalMatchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "externalMatchId 매핑을 찾을 수 없습니다: " + externalMatchId));

        if (!mapping.getMatchId().equals(canonicalMatchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "요청한 matchId와 외부 매핑 대상이 일치하지 않습니다.");
        }

        matchExternalMappingRepository.deleteByProviderAndExternalMatchId(PROVIDER_LOL_ESPORTS, externalMatchId);
        matchCacheService.invalidateAllCaches();
    }

    @Transactional
    public void deleteMappingsByCanonicalMatchId(String canonicalMatchId) {
        if (StringUtils.hasText(canonicalMatchId)) {
            matchExternalMappingRepository.deleteByMatchId(canonicalMatchId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<MatchExternalMapping> findLatestMappingByCanonicalMatchId(String canonicalMatchId) {
        if (!StringUtils.hasText(canonicalMatchId)) {
            return Optional.empty();
        }
        return matchExternalMappingRepository
                .findAllByProviderAndMatchIdOrderByUpdatedAtDesc(PROVIDER_LOL_ESPORTS, canonicalMatchId)
                .stream()
                .findFirst();
    }

    private MatchExternalMapping saveMapping(String canonicalMatchId, String externalMatchId, String actor) {
        MatchExternalMapping mapping = matchExternalMappingRepository.findByProviderAndExternalMatchId(PROVIDER_LOL_ESPORTS, externalMatchId)
                .orElseGet(MatchExternalMapping::new);

        mapping.setProvider(PROVIDER_LOL_ESPORTS);
        mapping.setExternalMatchId(externalMatchId);
        mapping.setMatchId(canonicalMatchId);
        mapping.setUpdatedBy(actor);
        return matchExternalMappingRepository.save(mapping);
    }

    private String resolveCanonicalFromMapping(MatchExternalMapping mapping, String fallbackExternalMatchId) {
        String mappedMatchId = mapping.getMatchId();
        if (!StringUtils.hasText(mappedMatchId)) {
            return fallbackExternalMatchId;
        }

        if (matchRepository.findByMatchId(mappedMatchId).isPresent()) {
            return mappedMatchId;
        }

        matchExternalMappingRepository.delete(mapping);
        log.warn("기준 match가 없어 외부 매핑을 정리했습니다. externalMatchId={}, staleMatchId={}",
                mapping.getExternalMatchId(), mappedMatchId);
        return fallbackExternalMatchId;
    }

}
