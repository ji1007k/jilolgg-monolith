package com.test.basic.auth.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Spring Boot 애플리케이션의 통합 테스트를 실행하기 위한 애너테이션.
// Spring 애플리케이션 컨텍스트(ApplicationContext) 를 로드하고, 필요한 빈(bean)을 주입
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 테스트 후 DB 롤백
@ActiveProfiles("test")  // 테스트 실행 시 특정 프로필(test)을 강제로 활성화
public class UserIntegrationTestWithMockMvc {
    @Autowired
    private MockMvc mockMvc;  // HTTP 요청 테스트를 위한 MockMvc

    @Autowired
    private UserRepository userRepository;  // DB 검증을 위한 Repository

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환을 위한 ObjectMapper

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(null, "password123", "email@example.com", "username", null, null, null);
        testUser = userRepository.save(testUser);  // DB에 실제 저장
    }

    @Test
    @DisplayName("유저 생성 - DB 저장 및 HTTP 응답 테스트")
    void testCreateUser() throws Exception {
        User newUser = new User(null, "newpass", "new@example.com", "newuser", null, null, null);
        String userJson = objectMapper.writeValueAsString(newUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())  // HTTP 201 응답 확인
                .andExpect(jsonPath("$.id").isNumber())  // ID가 숫자인지 확인
                .andExpect(jsonPath("$.email").value("new@example.com"));  // 이메일 검증

        // DB에 저장되었는지 확인
        Optional<User> savedUser = userRepository.findByEmail("new@example.com");
        assertTrue(savedUser.isPresent());  // DB에 존재하는지 확인
        assertEquals("newuser", savedUser.get().getName());  // 저장된 데이터 검증
    }

    @Test
    @DisplayName("유저 목록 조회 - DB 데이터 검증 및 HTTP 응답 테스트")
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$").isArray())  // body로 담겨온 데이터가 배열인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(0)))  // 최소 0개 이상인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThan(0)));  // 1개 이상인지
    }

    @Test
    @DisplayName("유저 단건 조회 - DB와 HTTP 응답 검증")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.email").value("email@example.com"));  // 이메일 검증
    }

    @Test
    @DisplayName("유저 수정 - DB 및 HTTP 응답 검증")
    void testUpdateUser() throws Exception {
        User updateUser = new User();
        updateUser.setId(testUser.getId());
        updateUser.setName("newname");

        mockMvc.perform(put("/api/users/{id}", updateUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.name").value("newname"));  // 이름 검증
    }

    @Test
    @DisplayName("유저 삭제 - DB 및 HTTP 응답 검증")
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", testUser.getId()))
                .andExpect(status().isNoContent());  // HTTP 204 응답 확인

        // DB에서 삭제되었는지 확인
        Optional<User> deletedUser = userRepository.findById(testUser.getId());
        assertFalse(deletedUser.isPresent());  // 삭제되었는지 확인
    }
}
