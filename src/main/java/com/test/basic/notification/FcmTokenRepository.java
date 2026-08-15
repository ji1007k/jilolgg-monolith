package com.test.basic.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByOwnerKey(String ownerKey);
    Optional<FcmToken> findByOwnerKeyAndToken(String ownerKey, String token);

    /**
     * 같은 FCM 토큰이 다른 주체로 등록돼 있는지 확인한다.
     * 비로그인으로 알림을 켜둔 기기에서 로그인하면 같은 토큰이 'd:'와 'u:' 양쪽에 생겨
     * 푸시가 두 번 가므로, 로그인 시 주체를 옮길 때 쓴다.
     */
    List<FcmToken> findByToken(String token);
}
