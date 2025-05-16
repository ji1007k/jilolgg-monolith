package com.test.basic.lol.matches;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByMatchId(String id);

    @Query("SELECT m FROM Match m WHERE m.startTime >= :startOfDay AND m.startTime < :startOfNextDay")
    List<Match> findMatchesByDate(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );

    @Query("SELECT m.matchId FROM Match m WHERE m.league.leagueId = :leagueId")
    List<String> findMatchIdByLeague_LeagueId(String leagueId);
}
