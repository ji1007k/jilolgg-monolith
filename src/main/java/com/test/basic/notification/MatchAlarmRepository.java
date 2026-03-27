package com.test.basic.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MatchAlarmRepository extends JpaRepository<MatchAlarm, Long> {
    Optional<MatchAlarm> findByUserIdAndMatchId(Long userId, String matchId);
    List<MatchAlarm> findByMatchId(String matchId);
}
