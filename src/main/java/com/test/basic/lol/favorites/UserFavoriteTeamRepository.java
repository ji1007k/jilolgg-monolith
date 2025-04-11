package com.test.basic.lol.favorites;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteTeamRepository extends JpaRepository<UserFavoriteTeam, Long> {

    List<UserFavoriteTeam> findByUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<UserFavoriteTeam> findByUserIdAndTeamCode(Long userId, String teamCode);

    void deleteByUserIdAndTeamCode(Long userId, String teamCode);
}
