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
        
        // Remove existing token if any
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        return refreshTokenRepository.save(refreshToken);
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
