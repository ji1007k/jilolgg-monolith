package com.test.basic.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FcmToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String token;
    
    private String deviceInfo;
    private LocalDateTime updatedAt;
}
