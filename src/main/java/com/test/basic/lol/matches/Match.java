package com.test.basic.lol.matches;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.leagues.League;
import com.test.basic.lol.matchteams.MatchTeam;
import com.test.basic.lol.tournaments.Tournament;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true, length = 64)
    @JsonProperty("id")
    private String matchId;

    @ManyToOne
    @JoinColumn(name = "league_id", referencedColumnName = "league_id", nullable = false)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", referencedColumnName = "tournament_id")
    private Tournament tournament;

    private LocalDateTime startTime;

    @Column(length = 50)
    private String state;

    @Column(length = 100)
    private String blockName;

    private Integer gameCount;

    @Column(length = 50)
    private String strategy;

    @OneToMany(mappedBy = "match")
    private List<MatchTeam> matchTeams;

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", matchId='" + matchId + '\'' +
                ", startTime=" + startTime +
                ", state='" + state + '\'' +
                ", blockName='" + blockName + '\'' +
                ", gameCount=" + gameCount +
                ", strategy='" + strategy + '\'' +
                '}';
    }
}
