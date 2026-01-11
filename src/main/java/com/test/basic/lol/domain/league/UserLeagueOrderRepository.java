package com.test.basic.lol.domain.league;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLeagueOrderRepository extends JpaRepository<UserLeagueOrder, Long> {
    List<UserLeagueOrder> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserLeagueOrder u WHERE u.userId = :userId")
    void deleteByUserId(Long userId);
}
