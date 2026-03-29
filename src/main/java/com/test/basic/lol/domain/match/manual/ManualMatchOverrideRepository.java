package com.test.basic.lol.domain.match.manual;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManualMatchOverrideRepository extends JpaRepository<ManualMatchOverride, Long> {
    Optional<ManualMatchOverride> findByMatchId(String matchId);
    void deleteByMatchId(String matchId);
}
