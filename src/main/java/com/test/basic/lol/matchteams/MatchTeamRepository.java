package com.test.basic.lol.matchteams;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, Long> {
    Optional<MatchTeam> findByMatch_MatchIdAndTeam_TeamId(String matchId, String teamId);

    @Query("SELECT DISTINCT mt.team.teamId FROM MatchTeam mt WHERE mt.match.matchId IN :matchIds")
    List<String> findDistinctTeamIdByMatchIdIn(List<String> matchIds);
}
