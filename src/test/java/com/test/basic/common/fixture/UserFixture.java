package com.test.basic.common.fixture;

import com.test.basic.user.UserEntity;

import java.time.LocalDateTime;

/**
 * Fixture 패턴: 테스트용 고정 데이터
 * - Object Mother 패턴: 미리 정의된 객체를 제공하는 팩토리 클래스
 */
public class UserFixture {

    public static UserEntity adminUser() {
        return UserEntity.builder()
                .email("testadmin" + System.currentTimeMillis() + "@email.com")
                .password("password123")
//                .password(RSAUtil.encryptWithPublicKey("password", pubKey))
                .name("admin")
                .authority("ROLE_ADMIN")
                .createdDt(LocalDateTime.now())
                .build();
    }

    public static UserEntity adminUser(Long id) {
        UserEntity user = adminUser();
        user.setId(id);
        return user;
    }

    public static UserEntity defaultUser() {
        return UserEntity.builder()
                .email("testuser" + System.currentTimeMillis() + "@email.com")
                .password("password123")
                .name("testuser")
                .authority("SCOPE_USER")
                .createdDt(LocalDateTime.now())
                .build();
    }

    public static UserEntity defaultUser(Long id) {
        UserEntity user = defaultUser();
        user.setId(id);
        return user;
    }

    public static UserEntity managerUser() {
        return UserEntity.builder()
                .email("testmanager" + System.currentTimeMillis() + "@email.com")
                .password("password123")
                .name("testmanager")
                .authority("SCOPE_MANAGER")
                .createdDt(LocalDateTime.now())
                .build();
    }

    public static UserEntity managerUser(Long id) {
        UserEntity user = managerUser();
        user.setId(id);
        return user;
    }
    
    // TODO - Builder 패턴도 적용
    //  체이닝으로 객체를 유연하게 커스터마이징
}
