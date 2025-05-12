package com.test.basic.lol.teams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.leagues.League;
import com.test.basic.lol.matchteams.MatchTeam;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Data
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id"})
})
@RequiredArgsConstructor
public class Team {
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", unique = true, nullable = false)
    @JsonProperty("id")
    private String teamId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(nullable = false)
    private String image;

    @OneToOne
    @JoinColumn(name = "league_id", referencedColumnName = "league_id", nullable = false)
    private League league;

    @OneToMany(mappedBy = "team")
    private List<MatchTeam> matchTeams;

    public Team(String teamId, String code, String name, String slug, String image, League league) {
        this.teamId = teamId;
        this.code = code;
        this.name = name;
        this.slug = slug;   // url 라우팅용
        this.image = image;
        this.league = league;
    }
}
