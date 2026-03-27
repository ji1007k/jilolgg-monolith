package com.test.basic.notification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_alarms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchAlarm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String matchId;
}
