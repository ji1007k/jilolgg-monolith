package com.test.basic.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.user.CustomUserDetails;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 내장 톰캣 없이 Spring context만 띄움
@SpringBootTest(webEnvironment = MOCK)
// MockMvc 자동 설정 (가짜 HTTP 요청 테스트용)
@AutoConfigureMockMvc
@Transactional  // 테스트 후 DB 롤백
@ActiveProfiles("test")  // 테스트 실행 시 특정 프로필(test)을 강제로 활성화
// 테스트 전 H2용 스키마 및 초기 데이터 실행
@Sql(scripts = {"/db/h2/schema.sql", "/db/h2/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserIntegrationTestWithMockMvc {
    @Autowired
    private MockMvc mockMvc;  // HTTP 요청 테스트를 위한 MockMvc

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;  // DB 검증을 위한 Repository

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환을 위한 ObjectMapper

    private UserEntity testUser;

    private String jwtAccess;
    private Cookie jwtAccessCookie;


    @BeforeEach
    void setUp() throws Exception {
//        testUser = new UserEntity(null, "password123", "email@example.com", "username", null, null, null);
        testUser = new UserEntity();
        testUser.setEmail("email@example.com");
        testUser = userRepository.save(testUser);  // DB에 실제 저장

        loginAdminAndGenerateJWT();
    }

    void loginAdminAndGenerateJWT() throws Exception {
        UserDetails mockUser = new CustomUserDetails(
                1L, // 혹은 UUID.randomUUID()
                "admin",           // email
                "$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy", // password
                "admin",           // username
                List.of(new SimpleGrantedAuthority("SCOPE_ADMIN"))
        );

        when(customUserDetailsService.loadUserByUsername(mockUser.getUsername())).thenReturn(mockUser);

        String userInfo = "admin:admin";
        Base64 base64Encoded = Base64.encode(userInfo.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc
                .perform(get("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        this.jwtAccess = jwtTokenProvider.getJwtStrFromCookie(result.getResponse().getCookies(), "access_token");
        this.jwtAccessCookie = new Cookie("access_token", this.jwtAccess);
    }

    @Test
    @DisplayName("유저 생성 - DB 저장 및 HTTP 응답 테스트")
    void testCreateUser() throws Exception {
        UserEntity newUser = new UserEntity();
        newUser.setName("newUser");
        newUser.setPassword("newPassword");
        newUser.setEmail("newEmail@example.com");
        String userJson = objectMapper.writeValueAsString(newUser);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .cookie(this.jwtAccessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())  // HTTP 201 응답 확인
                .andExpect(jsonPath("$.id").isNumber())  // ID가 숫자인지 확인
                .andExpect(jsonPath("$.email").value("newEmail@example.com"));  // 이메일 검증

        // DB에 저장되었는지 확인
        Optional<UserEntity> savedUser = userRepository.findByEmail("newEmail@example.com");
        assertTrue(savedUser.isPresent());  // DB에 존재하는지 확인
        assertEquals("newUser", savedUser.get().getName());  // 저장된 데이터 검증
    }

    @Test
    @DisplayName("유저 목록 조회 - DB 데이터 검증 및 HTTP 응답 테스트")
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$").isArray())  // body로 담겨온 데이터가 배열인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(0)))  // 최소 0개 이상인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThan(0)));  // 1개 이상인지
    }

    @Test
    @DisplayName("유저 단건 조회 - DB와 HTTP 응답 검증")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/users/{id}", testUser.getId())
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.email").value("email@example.com"));  // 이메일 검증
    }

    @Test
    @DisplayName("유저 수정 - DB 및 HTTP 응답 검증")
    void testUpdateUser() throws Exception {
        UserEntity updateUser = new UserEntity();
        updateUser.setId(testUser.getId());
        updateUser.setName("newname");

        mockMvc.perform(put("/users/{id}", updateUser.getId())
                    .with(csrf())
                    .cookie(this.jwtAccessCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.name").value("newname"));  // 이름 검증
    }

    @Test
    @DisplayName("유저 삭제 - DB 및 HTTP 응답 검증")
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUser.getId())
                        .with(csrf())
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isNoContent());  // HTTP 204 응답 확인

        // DB에서 삭제되었는지 확인
        Optional<UserEntity> deletedUser = userRepository.findById(testUser.getId());
        assertFalse(deletedUser.isPresent());  // 삭제되었는지 확인
    }
}
