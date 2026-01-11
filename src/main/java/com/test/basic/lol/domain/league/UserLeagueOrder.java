package com.test.basic.lol.domain.league;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_league_orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "league_id"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLeagueOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "league_id", nullable = false)
    private String leagueId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }
}
