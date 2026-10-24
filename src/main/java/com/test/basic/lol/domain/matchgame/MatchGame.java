package com.test.basic.lol.domain.matchgame;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 시리즈(경기)를 구성하는 세트. 외부 API의 game id를 그대로 키로 쓴다.
 * 승자는 확정할 수 있을 때만 채우고, 아니면 NULL로 둔다(추정값을 확정처럼 저장하지 않는다).
 */
@Entity
@Getter @Setter
@Table(name = "match_games")
public class MatchGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, unique = true, length = 64)
    private String gameId;

    @Column(name = "match_id", nullable = false, length = 64)
    private String matchId;

    @Column(name = "game_number", nullable = false)
    private Integer number;

    @Column(length = 50)
    private String state;

    @Column(length = 64)
    private String blueTeamId;

    @Column(length = 64)
    private String redTeamId;

    @Column(length = 64)
    private String winnerTeamId;
}
