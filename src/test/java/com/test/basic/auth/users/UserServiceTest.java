package com.test.basic.auth.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Test")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "password", "email", "name", "profileImageUrl", LocalDateTime.now(), null);
    }


    @Test
    @DisplayName("유저 생성 테스트")
    void testCreateUser() {
        // Given
//        User user = new User(1L, "password", "email", "name", "profileImageUrl", null, null);

        // When
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User newUser = invocation.getArgument(0);
            user.setPassword(newUser.getPassword());
            return newUser;
        });

        User createdUser = userService.createUser(user);

        // Then
        assertNotNull(createdUser);
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("name");
        assertTrue(new BCryptPasswordEncoder().matches("password", createdUser.getPassword()));
    }

    @Test
    @DisplayName("유저 목록 조회 테스트")
    void testGetAllUsers() {
        User user1 = new User(1L, "password1", "email1", "name1", "profileImageUrl1", LocalDateTime.now(), null);
        User user2 = new User(2L, "password2", "email2", "name2", "profileImageUrl2", LocalDateTime.now(), null);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<User> users = userService.getAllUsers(1, 10, "", "");

        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("유저 조회 테스트")
    void testGetUserById() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        Optional<User> foundUser = userRepository.findById(user.getId());

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.get().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("유저 수정 테스트")
    void testUpdateUser() {
        User newUser = new User(user);
        newUser.setId(user.getId());
        newUser.setName("newname");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(userService.updateUser(user.getId(), newUser)).thenReturn(any(Optional.class));

        Optional<User> updatedUser = userService.updateUser(user.getId(), newUser);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.get().getId()).isEqualTo(user.getId());
        assertThat(updatedUser.get().getName()).isEqualTo(newUser.getName());
    }

    // 비밀번호 확인
    @Test
    @DisplayName("비밀번호 일치여부 확인 테스트")
    void testCheckPassword() {
        String password = user.getPassword();
        String hashedPassword = PasswordUtils.hashPassword(user.getPassword());

        assertThat(PasswordUtils.checkPassword(password, hashedPassword)).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경 테스트")
    void testChangePassword() {
        // Given
        String newPassword = "newPassword";

        System.out.println("Before change - user.getPassword() = " + user.getPassword());

        // Mockito 설정: userRepository.findById() 가 기존 user 반환
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        // ✅ save() 메서드가 호출될 때 user 객체의 비밀번호가 실제로 변경되도록 처리
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0); // 전달된 User 객체 가져오기
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
    @DisplayName("유저 삭제 테스트 - 유저가 존재하는 경우")
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
    @DisplayName("유저 삭제 테스트 - 유저가 존재하지 않는 경우")
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
}
