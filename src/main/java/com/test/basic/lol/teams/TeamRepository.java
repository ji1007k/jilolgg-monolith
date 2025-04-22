package com.test.basic.lol.teams;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByTeamCode(String teamCode);
    Optional<Team> findBySlug(String slug);
    @Query("SELECT t.teamCode FROM Team t WHERE t.id = :id")
    String findTeamCodeById(@Param("id") Long id);
    @Query("SELECT t.teamName FROM Team t WHERE t.id = :id")
    String findTeamNameById(@Param("id") Long id);

    List<Team> findByHomeLeague(String homeLeague);
    // SELECT * FROM team WHERE slug IN ('slug1', 'slug2', 'slug3');
    List<Team> findBySlugIn(List<String> slugs);
    List<Team> findByHomeLeagueAndSlugIn(String homeLeague, List<String> slugs);
    List<Team> findBySlugContaining(String slug);


    //
//    findByTeamName(String teamName)
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
