package com.test.basic.user;

import com.test.basic.common.fixture.UserFixture;
import com.test.basic.common.utils.PasswordUtils;
import com.test.basic.common.utils.RSAUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("== 사용자 관리 Service 단위테스트 ==")
public class UserServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    // @Mock + @InjectMocks (순수한 Mockito 테스트)
    //  - Spring 컨텍스트 없이, 순수한 Java 단위 테스트(Mock 단위 테스트)에서 사용
    //  - Spring을 로드하지 않으므로 실행 속도가 빠름
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserFixture.defaultUser(1L);
    }


    @Test
    @DisplayName("사용자생성_정상_ID생성및비밀번호암호화")
    void testCreateUser() {
        String orgPwd = user.getPassword();

        // When
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity newUser = invocation.getArgument(0);
//            user.setPassword(newUser.getPassword());
            logger.info("==== Before PWD: {}", orgPwd);
            logger.info("==== After PWD: {}", newUser.getPassword());
            logger.info("==== user PWD: {}", user.getPassword());
            return newUser;
        });

        UserEntity createdUser = userService.createUser(user);

        // Then
        assertNotNull(createdUser);
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo(user.getName());
        assertTrue(new BCryptPasswordEncoder().matches(orgPwd, createdUser.getPassword()));
    }

    @Test
    @DisplayName("유저목록조회_정상_리스트반환")
    void testGetAllUsers() {
        UserEntity user2 = new UserEntity();

        when(userRepository.findAll()).thenReturn(List.of(user, user2));

        List<UserEntity> users = userService.getAllUsers(1, 10, "", "");

        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("유저단건조회_존재ID_사용자데이터반환")
    void testGetUserById() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        Optional<UserEntity> foundUser = userService.getUserById(user.getId());

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.get().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("사용자수정_정상_수정된사용자정보반환")
    void testUpdateUser() {
        UserEntity newUser = new UserEntity(user);
        newUser.setId(user.getId());
        newUser.setName("newname");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(userService.updateUser(user.getId(), newUser)).thenReturn(any(Optional.class));

        Optional<UserEntity> updatedUser = userService.updateUser(user.getId(), newUser);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.get().getId()).isEqualTo(user.getId());
        assertThat(updatedUser.get().getName()).isEqualTo(newUser.getName());
    }

    // 비밀번호 확인
    @Test
    @DisplayName("비밀번호검증_정상_BCrypt일치")
    void testCheckPasswordWithBCrypt() {
        String password = user.getPassword();
        String hashedPassword = PasswordUtils.hashPassword(user.getPassword());

        assertThat(PasswordUtils.checkPassword(password, hashedPassword)).isTrue();
    }

    // 비밀번호 확인
    @Test
    @DisplayName("비밀번호검증_RSA암호화된올바른비밀번호_복호화후일치확인")
    void testSuccessCheckPasswordWithBCryptAndRSA() throws Exception {
        String originalPassword = "testPassword";
        user.setPassword(PasswordUtils.hashPassword(originalPassword));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.changePassword(user.getId(), originalPassword);

        // RSA 키 생성
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();

        // RSA 암호화 후 데이터 전송
        String password = "testPassword";
        String encodedPassword = RSAUtil.encryptWithPublicKey(password, keyPair.getPublic());

        // 복호화 후 비밀번호 확인
        String decodedPassword = RSAUtil.decryptWithPrivateKey(encodedPassword, keyPair.getPrivate());
        assertThat(PasswordUtils.checkPassword(decodedPassword, user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호검증_RSA암호화된잘못된비밀번호_복호화후불일치확인")
    void testFailCheckPasswordWithBCryptAndRSA() throws Exception {
        String originalPassword = "testPassword";
        user.setPassword(PasswordUtils.hashPassword(originalPassword));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.changePassword(user.getId(), originalPassword);

        // RSA 키 생성
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();

        // RSA 암호화 후 데이터 전송
        String password = "wrongPassword";
        String encodedPassword = RSAUtil.encryptWithPublicKey(password, keyPair.getPublic());

        // 복호화 후 비밀번호 확인
        String decodedPassword = RSAUtil.decryptWithPrivateKey(encodedPassword, keyPair.getPrivate());
        assertThat(PasswordUtils.checkPassword(decodedPassword, user.getPassword())).isFalse();
    }

    @Test
    @DisplayName("비밀번호변경_새로운비밀번호_암호화된비밀번호저장")
    void testChangePassword() {
        // Given
        String newPassword = "newPassword";

        System.out.println("Before change - user.getPassword() = " + user.getPassword());

        // Mockito 설정: userRepository.findById() 가 기존 user 반환
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        // ✅ save() 메서드가 호출될 때 user 객체의 비밀번호가 실제로 변경되도록 처리
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity savedUser = invocation.getArgument(0); // 전달된 User 객체 가져오기
            user.setPassword(savedUser.getPassword());  // ✅ user 객체의 password 변경
            return savedUser;
        });

        // When: 비밀번호 변경 실행
        boolean changed = userService.changePassword(user.getId(), newPassword);

        // Then: 변경 후 user 객체의 password 확인
        System.out.println("After change - user.getPassword() = " + user.getPassword());

        // ✅ 해시된 비밀번호끼리 직접 비교하면 안 됨 → PasswordUtils.checkPassword() 사용해야 함
        assertTrue(changed);
        assertTrue(PasswordUtils.checkPassword(newPassword, user.getPassword()));  // ✅ 올바른 검증 방식
    }



    @Test
    @DisplayName("사용자삭제_존재ID_성공")
    void testDeleteUser_UserExists() {
        Long userId = user.getId();

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Then
        userService.deleteUser(userId);

        // Verify that deleteById was called
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("사용자삭제_존재하지않는ID_NOTFOUND예외발생")
    void testDeleteUser_UserNotFound() {
        // Given
        Long userId = 1L;

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Then
        // Expect an exception to be thrown
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.deleteUser(userId);
        });

        // Verify the exception details
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
    }

    @Test
    @DisplayName("유저생성_이메일없음_예외발생")
    void createUser_NoEmail_ThrowsException() {
        UserEntity user = UserFixture.defaultUser();
        user.setEmail(null);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }

    @Test
    @DisplayName("유저조회_음수ID_예외발생")
    void getUser_NegativeId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> userService.getUserById(-10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀번호변경_빈문자열_예외발생")
    void changePassword_EmptyString_ThrowsException() {

        assertThatThrownBy(() -> userService.changePassword(user.getId(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
