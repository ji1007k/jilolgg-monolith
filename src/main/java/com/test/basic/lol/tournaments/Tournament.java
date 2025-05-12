package com.test.basic.lol.tournaments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.leagues.League;
import com.test.basic.lol.matches.Match;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "tournaments")
public class Tournament {
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false, unique = true)
    @JsonProperty("id")
    private String tournamentId;

    private String slug;

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "league_id", referencedColumnName = "league_id", nullable = false)
    private League league;

    @OneToMany(mappedBy = "tournament")
    private List<Match> matches;

    // Lombok의 @Data에는 자동 toString() 생성이 포함되어 있는데,
    // Spring이 디버깅용으로 toString() 호출 시도하면서 연관 필드까지 다 찍으려 해서 LAZY 로딩 문제 발생
    // => 메소드 오버라이드하여 연관 필드 출력하지 않도록 수정
    @Override
    public String toString() {
        return "Tournament{" +
                "id=" + id +
                ", tournamentId='" + tournamentId + '\'' +
                ", slug='" + slug + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
