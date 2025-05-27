package com.test.basic.lol.domain.team.favorite;

import com.test.basic.lol.domain.team.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_favorite_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "team_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy로 하고 fetch join 쓸 예정
    @JoinColumn(name = "team_id", referencedColumnName = "team_id", nullable = false)
    private Team team;


    public UserFavoriteTeam(Long userId, Integer displayOrder, Team team) {
        this.userId = userId;
        this.displayOrder = displayOrder;
        this.team = team;
    }

    public void updateOrder(Integer order) {
        this.displayOrder = order;
    }

}

