package com.test.basic.lol.domain.matchteam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, Long> {
    Optional<MatchTeam> findByMatch_MatchIdAndTeam_TeamId(String matchId, String teamId);

    @Query("select mt from MatchTeam mt where mt.match.matchId = :matchId and mt.team.teamId = :teamId")
    List<MatchTeam> findAllByMatch_MatchIdAndTeam_TeamId(String matchId, String teamId);

    @Query("""
        SELECT mt
        FROM MatchTeam mt
        WHERE mt.match.matchId = :matchId
    """)
    List<MatchTeam> findByMatch_MatchId(String matchId);
    @Query("SELECT DISTINCT mt.team.teamId FROM MatchTeam mt WHERE mt.match.matchId IN :matchIds")
    List<String> findDistinctTeamIdByMatchIdIn(List<String> matchIds);

    void deleteByMatch_MatchIdAndTeam_Name(String matchMatchId, String teamName);

    List<MatchTeam> findByMatch_MatchIdIn(Set<String> matchIds);

    void deleteByMatch_MatchId(String matchId);

    void deleteByMatch_MatchIdIn(Set<String> matchIds);

    @Modifying
    @Query(value = """
        INSERT INTO match_teams (match_id, team_id, outcome, game_wins)
        VALUES (:matchId, :teamId, :outcome, :gameWins)
        ON CONFLICT (match_id, team_id)
        DO UPDATE SET
            outcome = EXCLUDED.outcome,
            game_wins = EXCLUDED.game_wins
        """, nativeQuery = true)
    void upsertByMatchIdAndTeamId(
            @Param("matchId") String matchId,
            @Param("teamId") String teamId,
            @Param("outcome") String outcome,
            @Param("gameWins") Integer gameWins
    );
}
