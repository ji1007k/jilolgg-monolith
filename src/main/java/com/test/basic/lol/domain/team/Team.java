package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
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

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
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
