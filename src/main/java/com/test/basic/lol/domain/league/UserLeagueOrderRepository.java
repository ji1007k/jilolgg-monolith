package com.test.basic.lol.domain.league;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLeagueOrderRepository extends JpaRepository<UserLeagueOrder, Long> {
    List<UserLeagueOrder> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
