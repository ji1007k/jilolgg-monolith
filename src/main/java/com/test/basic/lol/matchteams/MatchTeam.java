package com.test.basic.lol.matchteams;

import com.test.basic.lol.matches.Match;
import com.test.basic.lol.teams.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
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

    private Integer gameWins;

}
