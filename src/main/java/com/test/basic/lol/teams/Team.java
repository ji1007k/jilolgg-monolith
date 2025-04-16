package com.test.basic.lol.teams;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"slug"})
})
@RequiredArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_code", nullable = false)
    private String teamCode;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(nullable = false)
    private String image;

    @Column(name = "home_league", nullable = false)
    private String homeLeague;

    public Team(String teamCode, String teamName, String slug, String image, String homeLeague) {
        this.teamCode = teamCode;
        this.teamName = teamName;
        this.slug = slug;   // url 라우팅용
        this.image = image;
        this.homeLeague = homeLeague;
    }
}
