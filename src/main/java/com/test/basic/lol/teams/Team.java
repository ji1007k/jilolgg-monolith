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

    // 테이블에 매핑되지 않는 필드
    @Transient
    private String teamId;

    @Transient
    private String rank;

    @Transient
    private String record; // "8,0,10" => 순서대로 win,losses,gameDiff count. 승리/패배 경기수, 득실차

    public Team(String teamCode, String teamName, String slug, String image, String homeLeague) {
        this.teamCode = teamCode;
        this.teamName = teamName;
        this.slug = slug;   // url 라우팅용
        this.image = image;
        this.homeLeague = homeLeague;
    }
}
