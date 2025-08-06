package com.test.basic.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.common.fixture.UserFixture;
import com.test.basic.common.support.AuthTestSupport;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
@ActiveProfiles("test")  // 테스트 실행 시 특정 프로필(test)을 강제로 활성화
@AutoConfigureMockMvc   // MockMvc 자동 설정 (가짜 HTTP 요청 테스트용)
@Import(AuthTestSupport.class)
@Transactional  // 테스트 후 DB 롤백
@Sql(
        scripts = {"/db/h2/user.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
) // 테스트 전 H2용 스키마 및 초기 데이터 실행
@DisplayName("== 사용자 관리 MockMvc 통합테스트 ==")
public class UserIntegrationTestWithMockMvc {
    // 기본 테스트 도구 ====================
    @Autowired
    private MockMvc mockMvc;  // HTTP 요청 테스트를 위한 MockMvc

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환을 위한 ObjectMapper

    // Repository ====================
    @Autowired
    private UserRepository userRepository;  // DB 검증을 위한 Repository

    // Mock Bean ====================
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // Test Support ====================
    @Autowired
    private AuthTestSupport authTestSupport;

    // Test Data ====================
    private UserEntity testUser;
    private AuthTestSupport.JwtTokenInfo jwtTokenInfo;


    @BeforeEach
    void setUp() throws Exception {
        testUser = userRepository.save(UserFixture.defaultUser());  // DB에 실제 저장

        UserDetails mockUser = authTestSupport.createTestAdminUser();
        when(customUserDetailsService.loadUserByUsername(mockUser.getUsername())).thenReturn(mockUser);
        this.jwtTokenInfo = authTestSupport.loginAdminAndCreateJWT("admin", "admin");
    }

    @Test
    @DisplayName("사용자생성_정상_201응답및DB저장")
    void testCreateUser() throws Exception {
        UserEntity newUser = UserFixture.managerUser();
        String userJson = objectMapper.writeValueAsString(newUser);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())  // HTTP 201 응답 확인
                .andExpect(jsonPath("$.id").isNumber())  // ID가 숫자인지 확인
                .andExpect(jsonPath("$.email").value(newUser.getEmail()));  // 이메일 검증

        // DB에 저장되었는지 확인
        Optional<UserEntity> savedUser = userRepository.findByEmail(newUser.getEmail());
        assertTrue(savedUser.isPresent());  // DB에 존재하는지 확인
        assertEquals(newUser.getName(), savedUser.get().getName());  // 저장된 데이터 검증
    }

    @Test
    @DisplayName("사용자목록조회_정상_200응답및리스트반환")
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$").isArray())  // body로 담겨온 데이터가 배열인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(0)))  // 최소 0개 이상인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThan(0)));  // 1개 이상인지
    }

    @Test
    @DisplayName("사용자단건조회_정상_200응답및데이터반환")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/users/{id}", testUser.getId())
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));  // 이메일 검증
    }

    @Test
    @DisplayName("사용자수정_정상_200응답및DB반영")
    void testUpdateUser() throws Exception {
        UserEntity updateUser = UserFixture.defaultUser(testUser.getId());
        updateUser.setName("newname");

        mockMvc.perform(put("/users/{id}", updateUser.getId())
                    .with(csrf())
                    .cookie(this.jwtTokenInfo.getJwtAccessCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())  // HTTP 200 응답 확인
                .andExpect(jsonPath("$.id").value(testUser.getId()))  // ID 검증
                .andExpect(jsonPath("$.name").value("newname"));  // 이름 검증
    }

    @Test
    @DisplayName("사용자삭제_정상_204응답및DB삭제")
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/{id}", testUser.getId())
                        .with(csrf())
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isNoContent());  // HTTP 204 응답 확인

        // DB에서 삭제되었는지 확인
        Optional<UserEntity> deletedUser = userRepository.findById(testUser.getId());
        assertFalse(deletedUser.isPresent());  // 삭제되었는지 확인
    }
}
