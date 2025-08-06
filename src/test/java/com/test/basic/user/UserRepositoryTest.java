package com.test.basic.user;

import com.test.basic.common.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Rollback
@DisplayName("=== 사용자 관리 Repository 단위테스트 ===")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("사용자저장_정상_성공")
    void saveUser() {
        UserEntity user = UserFixture.defaultUser();

        UserEntity savedUser = userRepository.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    @DisplayName("사용자수정_정상_성공")
    void updateUser() {
        UserEntity user = UserFixture.defaultUser();
        UserEntity savedUser = userRepository.saveAndFlush(user);
        Long id = savedUser.getId();

        savedUser.setName("updatedName");
        userRepository.saveAndFlush(savedUser);

        UserEntity foundUser = userRepository.findById(id).orElseThrow();
        assertThat(foundUser.getName()).isEqualTo("updatedName");
    }

    @Test
    @DisplayName("사용자삭제_존재ID_성공")
    void deleteUser() {
        UserEntity user = UserFixture.defaultUser();
        UserEntity savedUser = userRepository.saveAndFlush(user);

        userRepository.delete(savedUser);

        Optional<UserEntity> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("사용자단건조회_존재하는email_사용자데이터반환")
    void findByEmail() {
        UserEntity user = UserFixture.defaultUser();
        UserEntity savedUser = userRepository.saveAndFlush(user);

        Optional<UserEntity> foundUser = userRepository.findByEmail(user.getEmail());

        assertThat(foundUser.isPresent()).isTrue();
        assertThat(foundUser.get()).isNotNull();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    @DisplayName("사용자단건조회_존재하는name_사용자데이터반환")
    void findByName() {
        UserEntity user = UserFixture.defaultUser();
        UserEntity savedUser = userRepository.saveAndFlush(user);

        Optional<UserEntity> foundUser = userRepository.findByName(user.getName());

        assertThat(foundUser.isPresent()).isTrue();
        assertThat(foundUser.get()).isNotNull();
        assertThat(foundUser.get().getName()).isEqualTo(savedUser.getName());
    }
}
