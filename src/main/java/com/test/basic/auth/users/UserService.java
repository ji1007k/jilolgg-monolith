package com.test.basic.auth.users;

import com.test.basic.utils.PasswordUtils;
import com.test.basic.utils.RSAUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

@Service
class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        String hashedPwd = PasswordUtils.hashPassword(user.getPassword());
        user.setPassword(hashedPwd);
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
    public List<User> getAllUsers(int page, int size, String keyword, String sort) {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public Optional<User> updateUser(Long id, User user) {
        Optional<User> existingUser = userRepository.findById(id);

        if (existingUser.isPresent()) {
            User userEntity = existingUser.get();
            userEntity.setEmail(user.getEmail());
            userEntity.setName(user.getName());
            userEntity.setProfileImageUrl(user.getProfileImageUrl());
            userRepository.save(userEntity);

            return Optional.of(userEntity);
        }

        return Optional.empty();
    }

    public boolean checkPassword(Long id, String password) {
        Optional<User> user = userRepository.findById(id);

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
        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            User userEntity = user.get();
            String hashedPwd = PasswordUtils.hashPassword(newPassword);
            userEntity.setPassword(hashedPwd);
            userRepository.save(userEntity);

            return true;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public void deleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        userRepository.deleteById(id);
    }

    public String generateRSAKeyPair(HttpSession session) throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair();
        session.setAttribute("privateKey", keyPair.getPrivate());
        return RSAUtil.getPublicKeyAsString(keyPair.getPublic());
    }
}
