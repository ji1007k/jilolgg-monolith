package com.test.basic.auth;

import com.test.basic.user.UserEntity;
import com.test.basic.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        // user_id에 유니크 제약(@OneToOne)이 걸려 있다.
        // 기존 행을 지우고 새로 넣으면 같은 사용자의 로그인 요청이 동시에 들어올 때
        // 두 트랜잭션이 각각 insert를 시도해 제약 위반(409)이 발생한다.
        // 지우지 않고 값만 갱신하면 UPDATE라 경합이 나도 충돌하지 않는다.
        return refreshTokenRepository.findByUser(user)
                .map(existing -> {
                    existing.setToken(token);
                    existing.setExpiryDate(expiryDate);
                    return refreshTokenRepository.save(existing);
                })
                .orElseGet(() -> refreshTokenRepository.save(RefreshToken.builder()
                        .user(user)
                        .token(token)
                        .expiryDate(expiryDate)
                        .build()));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        refreshTokenRepository.deleteByUser(user);
    }
}
