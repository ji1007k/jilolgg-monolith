package com.test.basic.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserId(Long userId);
}
