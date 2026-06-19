package com.test.basic.lol.domain.player;

import com.test.basic.lol.domain.team.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String playerId;

    @Column(nullable = false)
    private String name;

    private String role;

    private String image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team team;
}
