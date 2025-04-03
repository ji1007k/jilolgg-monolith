package com.test.basic.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordUtilsTest {

    @Test
    void testPasswordMatchingSuccess() {
        // 저장된 해시된 비밀번호
        String storedPasswordHash = PasswordUtils.hashPassword("password123");

        // 올바른 비밀번호 입력
        boolean isMatch = PasswordUtils.checkPassword("password123", storedPasswordHash);

        // 비밀번호가 일치해야 함
        assertTrue(isMatch);
    }

    @Test
    void testPasswordMatchingFailure() {
        // 저장된 해시된 비밀번호
        String storedPasswordHash = PasswordUtils.hashPassword("password123");

        // 잘못된 비밀번호 입력
        boolean isMatch = PasswordUtils.checkPassword(storedPasswordHash, "wrongpassword");

        // 비밀번호가 일치하지 않아야 함
        assertFalse(isMatch);
    }
}
