package com.test.basic.lol.teams.favorites;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteTeamRepository extends JpaRepository<UserFavoriteTeam, Long> {

    List<UserFavoriteTeam> findByUserIdOrderByDisplayOrderDesc(Long userId);

    Optional<UserFavoriteTeam> findByUserIdAndTeamId(Long userId, Long teamId);

    void deleteByUserIdAndTeamId(Long userId, Long teamId);

    @Query("SELECT f FROM UserFavoriteTeam f JOIN FETCH f.team WHERE f.userId = :userId ORDER BY f.displayOrder DESC")
    List<UserFavoriteTeam> findByUserIdWithTeam(@Param("userId") Long userId);

    @Query("""
        SELECT new com.test.basic.lol.teams.favorites.FavoriteTeamResponse(
            t.id, t.teamCode, t.teamName, f.displayOrder
        )
        FROM UserFavoriteTeam f
        JOIN f.team t
        WHERE f.userId = :userId
        ORDER BY f.displayOrder DESC
    """)
    List<FavoriteTeamResponse> findFavoriteTeamsByUserIdOrderByDisplayOrderDesc(@Param("userId") Long userId);

    @Query("SELECT MAX(u.displayOrder) FROM UserFavoriteTeam u WHERE u.userId = :userId")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);
}
