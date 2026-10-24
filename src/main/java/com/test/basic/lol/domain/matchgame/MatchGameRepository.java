package com.test.basic.lol.domain.matchgame;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchGameRepository extends JpaRepository<MatchGame, Long> {
    List<MatchGame> findByMatchIdOrderByNumberAsc(String matchId);
}
