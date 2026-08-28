package com.test.basic.user;

import org.springframework.util.StringUtils;
import com.test.basic.common.utils.PasswordUtils;
import com.test.basic.common.utils.RSAUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity createUser(UserEntity user) {
//        String encodedPwd = new BCryptPasswordEncoder().encode(user.getPassword());
//        user.setPassword(encodedPwd);

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String hashedPwd = PasswordUtils.hashPassword(user.getPassword());
        user.setPassword(hashedPwd);

        // 권한이 없으면 로그인 시 권한 파싱에서 막혀 "가입은 되는데 로그인이 안 되는" 계정이 된다.
        // 엔티티에 기본값을 뒀지만 가입은 @RequestBody(no-args 생성자) 경로라 여기서 한 번 더 보장한다.
        if (!StringUtils.hasText(user.getAuthority())) {
            user.setAuthority(UserEntity.DEFAULT_AUTHORITY);
        }

        return userRepository.save(user);
    }

    public String decryptPassword(String encryptedPwd, HttpSession session) throws Exception {
        PrivateKey privateKey = (PrivateKey) session.getAttribute("privateKey");
        if (privateKey == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PrivateKey does not exists");
        }
        
        // 개인키 재사용 불가
        session.removeAttribute("privateKey");
        return RSAUtil.decryptWithPrivateKey(encryptedPwd, privateKey);
    }

    // FIXME 비밀번호 제외하고 조회
    public List<UserEntity> getAllUsers(int page, int size, String keyword, String sort) {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getUserById(Long id) {
        if (id < 1) {
            throw new IllegalArgumentException("Invalid ID");
        }

        return userRepository.findById(id);
    }

    @Transactional
    public Optional<UserEntity> updateUser(Long id, UserEntity user) {
        Optional<UserEntity> existingUser = userRepository.findById(id);

        if (existingUser.isPresent()) {
            UserEntity userEntity = existingUser.get();
            userEntity.setEmail(user.getEmail());
            userEntity.setName(user.getName());
            userEntity.setProfileImageUrl(user.getProfileImageUrl());
            userRepository.save(userEntity);

            return Optional.of(userEntity);
        }

        return Optional.empty();
    }

    public boolean checkPassword(Long id, String password) {
        Optional<UserEntity> user = userRepository.findById(id);

        if (user.isPresent()) {
            String userPassword = user.get().getPassword();

            if (PasswordUtils.checkPassword(password, userPassword)) {
                return true;
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return false;
    }

    public boolean changePassword(Long id, String newPassword) {
        if (newPassword.isEmpty()) {
            throw new IllegalArgumentException("New Password is required");
        }

        Optional<UserEntity> user = userRepository.findById(id);

        if (user.isPresent()) {
            UserEntity userEntity = user.get();
            String hashedPwd = PasswordUtils.hashPassword(newPassword);
            userEntity.setPassword(hashedPwd);
            // Increment version to invalidate all current JWTs & Refresh Tokens
            userEntity.setPasswordVersion(userEntity.getPasswordVersion() + 1);
            userRepository.save(userEntity);

            return true;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public void deleteUser(Long id) {
        Optional<UserEntity> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        userRepository.deleteById(id);
    }
}
