package com.test.basic.lol.domain.match.mapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchExternalMappingRepository extends JpaRepository<MatchExternalMapping, Long> {
    Optional<MatchExternalMapping> findByProviderAndExternalMatchId(String provider, String externalMatchId);
    List<MatchExternalMapping> findAllByProviderAndExternalMatchIdIn(String provider, List<String> externalMatchIds);
    Optional<MatchExternalMapping> findFirstByProviderAndMatchId(String provider, String matchId);
    List<MatchExternalMapping> findAllByProviderAndMatchIdOrderByUpdatedAtDesc(String provider, String matchId);
    void deleteByProviderAndExternalMatchId(String provider, String externalMatchId);
    void deleteByMatchId(String matchId);
}
