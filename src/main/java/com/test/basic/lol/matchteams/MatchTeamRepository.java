package com.test.basic.lol.matchteams;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, Long> {
    Optional<MatchTeam> findByMatch_MatchIdAndTeam_TeamId(String matchId, String teamId);
}
