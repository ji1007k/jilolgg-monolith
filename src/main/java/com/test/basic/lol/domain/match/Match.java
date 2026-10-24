package com.test.basic.lol.domain.match;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.matchteam.MatchTeam;
import com.test.basic.lol.domain.tournament.Tournament;
import com.test.basic.lol.domain.matchgame.MatchGame;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "matches")
public class Match {
    @Id
    @JsonIgnore
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

    // columnDefinition="TEXT"를 쓰면 테스트 설정의 globally_quoted_identifiers가 타입명까지 따옴표로 감싸
    // ("vod_url" "TEXT") H2에서 matches 테이블 생성이 실패한다.
    // 운영 스키마는 schema.sql이 관리하고(ddl-auto=none) vod_url은 거기서 TEXT로 정의되므로 길이만 지정한다.
    @Column(length = 2048)
    private String vodUrl;

    /**
     * 확정 경기에 밀려난 대진표 자리를 감추기 위한 표시. NULL이면 노출한다.
     * 행을 지우지 않는 이유는 잘못 숨겼을 때 되돌릴 수 있어야 하기 때문이다.
     */
    private LocalDateTime hiddenAt;

    @Column(length = 64)
    private String hiddenReason;

    @OneToMany(mappedBy = "match")
    private List<MatchTeam> matchTeams;

    // 세트 목록. match_id 문자열로만 연결하는 읽기 전용 관계다(저장은 MatchGameSyncService가 담당).
    @OneToMany
    @JoinColumn(name = "match_id", referencedColumnName = "match_id", insertable = false, updatable = false)
    @OrderBy("number ASC")
    @BatchSize(size = 50)
    @EqualsAndHashCode.Exclude
    private List<MatchGame> games;

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
