package com.test.basic.common.fixture;

import java.util.UUID;
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
        // 이름도 유니크해야 한다. 고정값이면 같은 fork에서 두 테스트가 이 픽스처를 저장했을 때
        // findByName이 2건을 받아 NonUniqueResultException이 난다.
        // 테스트 클래스가 늘어 fork 분배가 바뀌는 순간 갑자기 터지는 종류의 결함이다.
        // fork는 별도 JVM이라 시간 기반 값으로는 충돌을 못 막는다.
        String unique = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        return UserEntity.builder()
                .email(unique + "@email.com")
                .password("password123")
                .name(unique)
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
