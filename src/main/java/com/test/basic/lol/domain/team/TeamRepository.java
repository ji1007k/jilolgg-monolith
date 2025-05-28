package com.test.basic.lol.domain.team;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByCode(String code);
    Optional<Team> findBySlug(String slug);

    Optional<Team> findByCodeAndName(String code, String name);

    @Query("SELECT t FROM Team t WHERE t.teamId = :teamId")
    Optional<Team> findByTeam_TeamId(String teamId);

    @Query("SELECT DISTINCT t FROM Team t WHERE t.teamId IN (SELECT DISTINCT mt.team.teamId FROM MatchTeam mt)")
    List<Team> findTeamsWithMatches();

    @Query("SELECT DISTINCT t FROM Team t WHERE t.teamId IN (SELECT DISTINCT mt.team.teamId FROM MatchTeam mt) " +
            "AND (:leagueId IS NULL OR t.league.leagueId = :leagueId) " +
            "AND (:slugs IS NULL OR t.slug IN :slugs)")
    List<Team> findTeamsWithMatchesFiltered(@Param("leagueId") String leagueId,
                                           @Param("slugs") List<String> slugs);

    Optional<Team> findByName(String name);

    List<Team> findByTeamIdIn(List<String> teamIds);


// TODO 삭제 ------------------------------------------------------
    @Query("SELECT t.code FROM Team t WHERE t.id = :id")
    String findCodeById(@Param("id") Long id);
    @Query("SELECT t.name FROM Team t WHERE t.id = :id")
    String findTeamNameById(@Param("id") Long id);

    List<Team> findByLeague_LeagueId(String leagueId);
    // SELECT * FROM team WHERE slug IN ('slug1', 'slug2', 'slug3');
    List<Team> findBySlugIn(List<String> slugs);
    List<Team> findByLeague_LeagueIdAndSlugIn(String leagueId, List<String> slugs);
    List<Team> findBySlugContaining(String slug);

    List<Team> findByCodeIn(Set<String> teamCodes);
    //
//    findByTeamName(String name)
// WHERE team_name = ?

//    findByTeamNameAndLeague(String name, String league)
// WHERE team_name = ? AND league = ?

//    findBySlugIn(List<String> slugs)
// WHERE slug IN (...)

//    findByCreatedAtBetween(LocalDate start, LocalDate end)
// WHERE created_at BETWEEN ? AND ?

//    Like: 직접 % 넣어줘야함. 더 정교한 패턴 (ex: jo%, %hn, _ohn) 설정 가능
//    findByNameLike("%john%")

//    Containing: %keyword% 로 자동으로 감싸줌.
//    findByTitleContaining(String keyword)

    // IsNull / IsNotNull
    // findByEmailIsNull()

    // OrderBy
    // findByStatusOrderByCreatedAtDesc()

}
