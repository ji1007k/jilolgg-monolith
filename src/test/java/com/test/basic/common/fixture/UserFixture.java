package com.test.basic.common.fixture;

import com.test.basic.user.UserEntity;

import java.time.LocalDateTime;

/**
 * Fixture 패턴: 테스트용 고정 데이터
 * - Object Mother 패턴: 미리 정의된 객체를 제공하는 팩토리 클래스
 */
public class UserFixture {

    public static UserEntity adminUser() {
        UserEntity user = new UserEntity();
        user.setEmail("testadmin@email.com");
        user.setPassword("password123");
//        user.setPassword(RSAUtil.encryptWithPublicKey("password", pubKey));
        user.setName("testadmin");
        user.setAuthority("SCOPE_ADMIN");
        user.setCreatedDt(LocalDateTime.now());
        return user;
    }

    public static UserEntity adminUser(Long id) {
        UserEntity user = adminUser();
        user.setId(id);
        return user;
    }

    public static UserEntity defaultUser() {
        UserEntity user = new UserEntity();
        user.setEmail("testuser@email.com");
        user.setPassword("password123");
        user.setName("testuser");
        user.setAuthority("SCOPE_USER");
        user.setCreatedDt(LocalDateTime.now());
        return user;
    }

    public static UserEntity defaultUser(Long id) {
        UserEntity user = defaultUser();
        user.setId(id);
        return user;
    }

    public static UserEntity managerUser() {
        UserEntity user = new UserEntity();
        user.setEmail("testmanager@email.com");
        user.setPassword("password123");
        user.setName("testmanager");
        user.setAuthority("SCOPE_MANAGER");
        user.setCreatedDt(LocalDateTime.now());
        return user;
    }

    public static UserEntity managerUser(Long id) {
        UserEntity user = managerUser();
        user.setId(id);
        return user;
    }
    
    // TODO - Builder 패턴도 적용
    //  체이닝으로 객체를 유연하게 커스터마이징
}
