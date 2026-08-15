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
    
    /** 구독 주체. 로그인 사용자는 'u:<userId>', 비로그인 기기는 'd:<deviceId>'. */
    @Column(nullable = false, length = 80)
    private String ownerKey;

    /** 로그인 사용자만 채워진다. 조회 키는 ownerKey를 쓴다(기존 데이터 보존용으로만 남김). */
    private Long userId;

    @Column(nullable = false)
    private String token;
    
    private String deviceInfo;
    private LocalDateTime updatedAt;
}
