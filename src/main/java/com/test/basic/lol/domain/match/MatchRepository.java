package com.test.basic.lol.domain.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByMatchId(String id);
    List<Match> findAllByMatchIdOrderByIdAsc(String matchId);
    void deleteByMatchId(String matchId);

    @Query("""
        SELECT DISTINCT m FROM Match m
        JOIN m.matchTeams mt
        WHERE m.state NOT IN ('completed', 'unneeded')
            AND (m.startTime >= :startOfDay AND m.startTime < :startOfNextDay)
            AND mt.outcome IS NULL
    """)
    List<Match> findMatchesByDate(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );

    @Query("SELECT m.matchId FROM Match m WHERE m.league.leagueId = :leagueId")
    List<String> findMatchIdByLeague_LeagueId(String leagueId);

    @Query("""
        SELECT MIN(m.startTime)
        FROM Match m
        WHERE m.startTime >= :startOfDay AND m.startTime < :endOfDay
            AND m.state NOT IN ('completed', 'unneeded')
    """)
    Optional<LocalDateTime> findFirstMatchTimeOfDay(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 연관 관계 테이블 데이터를 JOIN해서 미리 조회 (EAGER LOADING)
    @EntityGraph(attributePaths = {"matchTeams", "matchTeams.team"})
    @Query("""
        SELECT m FROM Match m
        WHERE m.league.leagueId = :leagueId
            AND m.startTime BETWEEN :startOfDay AND :endOfDay
    """)
    List<Match> findMatchByLeagueIdAndDate(String leagueId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Match> findByMatchIdIn(List<String> matchIds);
    List<Match> findByMatchIdIn(Set<String> matchIds);
    List<Match> findAllByLeague_LeagueIdAndStartTimeBetween(String leagueId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT m FROM Match m 
        WHERE m.startTime >= :start AND m.startTime < :end
        AND m.state NOT IN ('completed', 'unneeded')
    """)
    List<Match> findMatchesStartingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
