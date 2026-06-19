package com.test.basic.lol.domain.player;

import com.test.basic.lol.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByPlayerId(String playerId);
    List<Player> findByTeam(Team team);
}
