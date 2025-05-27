package com.test.basic.lol.domain.league;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.tournament.Tournament;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Data
@Table(name = "leagues")
@RequiredArgsConstructor
public class League {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false, unique = true)
    @JsonProperty("id") // JSON의 id -> leagueId로 매핑
    private String leagueId;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String region;

    private String image;

    private int priority;
    private int displayPosition;
    private String displayStatus;

    @OneToMany(mappedBy = "league") // 연관관계 엔티티에서 사용중인 필드명
    private List<Tournament> tournaments;

    @OneToMany(mappedBy = "league")
    private List<Match> matches;

    public League(LeagueDto leagueDto) {
        this.leagueId = leagueDto.getLeagueId();
        this.slug = leagueDto.getSlug();
        this.name = leagueDto.getName();
        this.region = leagueDto.getRegion();
        this.image = leagueDto.getImage();
        this.priority = leagueDto.getPriority();
        this.displayPosition = leagueDto.getDisplayPriority().getPosition();
        this.displayStatus = leagueDto.getDisplayPriority().getStatus();
    }


    @Override
    public String toString() {
        return "League{" +
                "id=" + id +
                ", leagueId='" + leagueId + '\'' +
                ", slug='" + slug + '\'' +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", image='" + image + '\'' +
                ", priority=" + priority +
                ", displayPosition=" + displayPosition +
                ", displayStatus='" + displayStatus + '\'' +
                '}';
    }
}
