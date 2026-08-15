package com.test.basic.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MatchAlarmRepository extends JpaRepository<MatchAlarm, Long> {
    Optional<MatchAlarm> findByOwnerKeyAndMatchId(String ownerKey, String matchId);
    List<MatchAlarm> findByOwnerKey(String ownerKey);
    List<MatchAlarm> findByMatchId(String matchId);
    List<MatchAlarm> findByOwnerKeyAndMatchIdIn(String ownerKey, List<String> matchIds);
}
