package com.test.basic.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.AuthController;
import com.test.basic.auth.csrf.CsrfTokenController;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetails;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.common.utils.RSAUtil;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// == 보안이 적용된 컨트롤러 단위 테스트 =====================
// Controller만 테스트하기 위해 사용
// 서비스 로직 없이 컨트롤러의 HTTP 요청/응답만 테스트하려는 목적으로 사용
@WebMvcTest({ UserController.class, AuthController.class, CsrfTokenController.class })
// JUnit 5 (@Test) 환경에서 Mockito(목킹 프레임워크)를 사용. => 목킹(Mock) 기능을 확장
// Spring 컨텍스트를 로드하지 않고 가벼운 단위 테스트 가능
@ExtendWith(MockitoExtension.class)
// Spring Security 인증/인가 설정 로드 => 인증/인가 테스트 정상 실행 위함
@Import({ SecurityConfig.class })
public class UserControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerTest.class);

    // 실제 HTTP 요청 없이 가상의 HTTP 요청을 만들어 컨트롤러를 테스트
    // 실제 서버를 실행하지 않고도 빠르고 독립적인 컨트롤러 테스트 가능
    @Autowired
    private MockMvc mockMvc;  // Spring MVC 테스트에서 HTTP 요청을 모의(Mock)할 수 있는 MockMvc를 사용

    @Autowired
    private ObjectMapper objectMapper;  // ObjectMapper를 사용하여 JSON 변환

    // UserService를 Mock 처리하여 실제 서비스 호출을 Mocking
    // @MockBean을 사용했지만, Spring Boot 3.4부터는 @MockitoBean을 사용
    // 25-02-06 jikim: Swagger ui 와 spring boot 3.4 버전 충돌로 sb 버전 3.3.1 로 변경함에 따라 MockBean 사용
    @MockBean   // 테스트에 필요한 서비스 계층 주입 (Spring 컨텍스트가 포함되는 경우)
    private UserService userService;

    // 사용자 인증 ====================
    @MockBean
    private CustomUserDetailsService customUserDetailsService;  // Security의 인증 로직 Mocking

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity user;

    private String jwtAccess;
    private String jwtRefresh;
    private Cookie jwtAccessCookie;
    private Cookie jwtRefreshCookie;
    private MockHttpSession mockSession;
    private String csrfToken;   // POST, PUT, DELETE 요청에서 CSRF 공격 방지
    // ==============================


    @BeforeEach
    void setUp() throws Exception {
        logger.info("======================================================");
        logger.info("...사용자 API 테스트 시작");
        logger.info("======================================================");
        
        // CSRF 검증용 세션 MOCK
//        mock(MockHttpSession.class); // 행동을 when()으로 정의해야 하며 sessionId 등 모든 값이 null
        mockSession = new MockHttpSession();

        KeyPair keyPair = RSAUtil.generateRSAKeyPair();
        PublicKey pubKey = keyPair.getPublic();

        user = new UserEntity();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setPassword(RSAUtil.encryptWithPublicKey("password", pubKey));
        user.setName("username");

        loginAdminAndGenerateJWT();
        generateCsrfToken();
    }

    void loginAdminAndGenerateJWT() throws Exception {
        logger.info("======================================================");
        logger.info("...테스트용 관리자 계정 로그인 및 JWT 토큰 발급");
        logger.info("======================================================");
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

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        logger.info("Set-Cookie: {}", setCookieHeader);

        this.jwtAccess = jwtTokenProvider.getJwtStrFromCookie(result.getResponse().getCookies(), "access_token");
        this.jwtRefresh = jwtTokenProvider.getJwtStrFromCookie(result.getResponse().getCookies(), "refresh_token");
        this.jwtAccessCookie = new Cookie("access_token", this.jwtAccess);
        this.jwtRefreshCookie = new Cookie("refresh_token", this.jwtRefresh);
    }

    void generateCsrfToken() throws Exception {
        logger.info("======================================================");
        logger.info("...CSRF 토큰 발급");
        logger.info("======================================================");
        this.csrfToken = mockMvc
                .perform(get("/csrf")
                        .cookie(this.jwtAccessCookie)
                        .session(this.mockSession))
                .andReturn()
                .getResponse()
                .getHeader("X-CSRF-TOKEN");
    }

    @Test
    @DisplayName("유저 생성 - HTTP 응답만 검증")
    void testCreateUser() throws Exception {

        // 테스트용 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        when(userService.decryptPassword(anyString(), any(MockHttpSession.class))).thenReturn("password");

        // Mocking된 서비스가 반환할 유저 설정
        when(userService.createUser(any(UserEntity.class))).thenReturn(user);

        // 서비스 없이 컨트롤러에서 HTTP 응답만 확인
        mockMvc.perform(post("/users")
                        .with(csrf())   // 자동으로 가짜 CSRF 토큰을 생성해서 테스트 우회
//                        .header("X-CSRF-TOKEN", this.csrfToken) // csrf 검증
//                        .session(mockSession)  // csrf 토큰 공유용
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user))
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isCreated())  // HTTP 201 상태 확인
                .andExpect(jsonPath("$.email").value(user.getEmail()))  // 이메일 검증
                .andExpect(jsonPath("$.name").value(user.getName()));  // 이름 검증
    }

    @Test
    @DisplayName("유저 목록 조회 - HTTP 응답만 검증")
    void testGetUsers() throws Exception {
        // 테스트용 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
//        UserEntity newUser = new UserEntity(null, "password123", "email@example.com", "username", null, null, null);
        UserEntity newUser = new UserEntity();

        List<UserEntity> users = List.of(newUser);

        // Mocking된 서비스가 반환할 유저 목록 설정
        when(userService.getAllUsers(1, 10, "", "id,asc")).thenReturn(users);

        // GET 요청으로 유저 목록 조회
        mockMvc.perform(get("/users")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "")
                        .param("sort", "id,asc")
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$").isArray())  // 배열 형태로 응답인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(1)));  // 1개 이상의 유저가 있는지 확인
    }

    @Test
    @DisplayName("유저 단건 조회 - HTTP 응답만 검증")
    void testGetUserById() throws Exception {
        // Mocking된 서비스가 반환할 유저 설정
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        // 특정 ID로 유저 조회
        mockMvc.perform(get("/users/{id}", 1L)
                        .cookie(this.jwtAccessCookie))  // 예시 ID: 1
                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$.id").value(1L))  // ID 검증
                .andExpect(jsonPath("$.email").value(user.getEmail()));  // 이메일 검증
    }

    @Test
    @DisplayName("유저 수정 - HTTP 응답만 검증")
    void testUpdateUser() throws Exception {
        // 수정하려는 기존 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        UserEntity updatedUser = new UserEntity(user);
        updatedUser.setId(user.getId());// 수정할 유저의 ID를 설정
        updatedUser.setName("newname");// 수정할 유저의 ID를 설정

        // Mocking된 서비스가 반환할 유저 설정
        // Mockito는 모든 인자에 ArgumentMatchers를 사용해야 함
        when(userService.updateUser(eq(1L), any(UserEntity.class))).thenReturn(Optional.of(updatedUser));

        // 특정 ID로 유저 수정
        mockMvc.perform(put("/users/{id}", 1L)  // URL 경로에서 id 값을 1L로 전달
                        .header("X-CSRF-TOKEN", this.csrfToken)
                        .session(this.mockSession)
                        .cookie(this.jwtAccessCookie)
                        .contentType(MediaType.APPLICATION_JSON)  // JSON 형식으로 요청
                        .content(objectMapper.writeValueAsString(updatedUser)))  // updatedUser를 JSON으로 변환하여 본문에 담기
                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$.id").value(1L))  // ID가 1L인지 확인
                .andExpect(jsonPath("$.name").value(updatedUser.getName()));  // 이름이 "newusername"인지 확인
    }

    @Test
    @DisplayName("유저 삭제 - HTTP 응답만 검증")
    void testDeleteUser() throws Exception {
        // UserService의 deleteUser 메서드를 호출할 때, 아무것도 반환하지 않도록 설정
        doNothing().when(userService).deleteUser(eq(1L));

        // DELETE 요청을 보내고 HTTP 상태 코드가 204 (No Content)인지를 검증
        mockMvc.perform(delete("/users/{id}", 1L)
                        .header("X-CSRF-TOKEN", this.csrfToken)
                        .cookie(this.jwtAccessCookie)
                        .session(this.mockSession))
                .andExpect(status().isNoContent());
    }

    /*@Test
    @Disabled
    @DisplayName("RSA Key 생성 테스트")
    void testGenerateRSA() throws Exception {
        // Given
        KeyPair keyPair = RSAUtil.generateRSAKeyPair();  // 실제 키 쌍을 생성
        PublicKey publicKey = keyPair.getPublic();  // 공개 키를 반환

        // UserService의 generateRSAKeyPair 메서드를 mock
        when(userService.generateRSAKeyPair(any(MockHttpSession.class))).thenReturn(publicKey.toString());

        // When & Then
        mockMvc.perform(get("/users/rsa")
                        .cookie(this.jwtAccessCookie)
                        .session(this.mockSession))  // 세션을 포함한 요청
                .andExpect(status().isOk())  // 응답 상태가 OK인지 검증
                .andExpect(content().string(containsString(publicKey.toString())));  // 반환된 공개키 포함 여부 확인
    }*/
}
