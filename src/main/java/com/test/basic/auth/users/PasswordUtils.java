package com.test.basic.auth.users;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtils {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 비밀번호를 해시하는 메서드
    public static String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    // 비밀번호 매칭 여부 검사
    public static boolean checkPassword(String inputPassword, String storedPasswordHash) {
        return passwordEncoder.matches(inputPassword, storedPasswordHash);
    }
}
