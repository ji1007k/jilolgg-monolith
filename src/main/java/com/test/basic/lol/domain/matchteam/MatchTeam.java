package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.team.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "match_teams")
public class MatchTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", referencedColumnName = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "team_id", nullable = false)
    private Team team;

    @Column(length = 20)
    private String outcome;

    // null일 수 있어서 Integer 사용. int는 null 저장 불가능
    private Integer gameWins;

}
