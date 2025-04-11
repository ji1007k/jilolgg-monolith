package com.test.basic.lol.favorites;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_favorite_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "team_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_code", nullable = false)
    private String teamCode;

    @Column(name = "display_order")
    private Integer displayOrder;

//    @Column(name = "memo")
//    private String memo;

    public UserFavoriteTeam(Long userId, String teamCode, Integer displayOrder) {
        this.userId = userId;
        this.teamCode = teamCode;
        this.displayOrder = displayOrder;
    }

    public void updateOrder(Integer order) {
        this.displayOrder = order;
    }
}

