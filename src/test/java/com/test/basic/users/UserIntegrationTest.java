package com.test.basic.users;

import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.CustomUserDetailsService;
import com.test.basic.common.handler.AcceptanceTestExecutionListener;
import com.test.basic.common.utils.RSAUtil;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Spring Boot 애플리케이션의 통합 테스트를 실행하기 위한 애너테이션.
// Spring 애플리케이션 컨텍스트(ApplicationContext) 를 로드하고, 필요한 빈(bean)을 주입
// webEnvironment : 랜덤 포트에서 실제 서버 실행
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // 테스트 실행 시 특정 프로필(test)을 강제로 활성화
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// 이렇게 실행된 쿼리는 Hibernate를 거치지 않고, 스프링의 DataSource를 통해 직접 실행되기 때문에
// Hibernate SQL 로그에 출력 안됨
@Sql(scripts = "/db/h2/users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestExecutionListeners(
        value = {AcceptanceTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DisplayName("== 사용자 관리 통합 테스트 ==")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // 테스트 인스턴스를 클래스 단위로 유지
public class UserIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(UserIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;  // 실제 HTTP 요청을 보내는 객체. + CSRF 자동 처리

    @MockBean
    private CustomUserDetailsService customUserDetailsService;  // ✅ MockBean으로 주입

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity user;

    // jwt 토큰 저장
    private String cookie;

    // CSRF 토큰은 세션과 연결되므로, 발급받은 세션을 유지하면서 전송해야 유효함
    private String csrfToken;
    private String sessionId;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        logger.info(baseUrl);

        user = new UserEntity();
        user.setId(1L);
        user.setEmail("email123@example.com");
        user.setPassword("password123");
        user.setName("user123");

        loginAdminAndGenerateJWT();
        generateCsrfToken();
    }

    void loginAdminAndGenerateJWT() {
        logger.info("======================================================");
        logger.info("...테스트용 관리자 계정 로그인 및 JWT 토큰 발급");
        logger.info("======================================================");

        UserDetails mockUser = User.withUsername("admin")
                .password("$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy")
                .authorities("ADMIN")
                .build();

        when(customUserDetailsService.loadUserByUsername(mockUser.getUsername())).thenReturn(mockUser);

        String userInfo = "admin:admin";
        String base64Encoded = Base64.getEncoder().encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));

        // ✅ 로그인 요청
        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.GET,
                new HttpEntity<>(null, createHeaders("Basic " + base64Encoded)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ✅ 쿠키에서 JWT 토큰 추출
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        logger.info("Set-Cookie: {}", cookies);

        List<String> filteredCookies = new ArrayList<>();
        for (String cookie : cookies) {
            filteredCookies.add(cookie.split(";")[0]); // 첫 번째 값만 저장 (만료일, secure 등 옵션 제거)
        }

        cookie = String.join("; ", filteredCookies); // 쿠키를 한 줄로 합치기
    }

    void generateCsrfToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        HttpEntity<Void> request = new HttpEntity<>(headers);  // Body 없이 헤더만 포함
        ResponseEntity<String> csrfResponse = restTemplate.exchange(
                "/csrf",
                HttpMethod.GET,
                request,
                String.class
        );

        // CSRF 토큰을 헤더에서 추출
        csrfToken = csrfResponse.getHeaders().getFirst("X-CSRF-TOKEN");

        // 🚨 CSRF 토큰이 없으면 예외 발생
        assertNotNull(csrfToken, "CSRF Token이 없습니다!");
        logger.info("CSRF Token: {}", csrfToken);

        // ✅ 세션 쿠키 추출
        List<String> cookies = csrfResponse.getHeaders().get("Set-Cookie");
        sessionId = null;
        for (String cookie : cookies) {
            if (cookie.startsWith("JSESSIONID")) {
                sessionId = cookie.split(";")[0].split("=")[1];  // JSESSIONID 값 추출
            }
        }
        assertNotNull(sessionId, "세션ID가 없습니다.");
        System.out.println("Session ID: " + sessionId);
    }

    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @Order(1)
    @DisplayName("유저 생성 - DB 저장 및 HTTP 응답 테스트")
    void testCreateUser() {
        String userUrl = "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ CSRF 토큰 설정
        logger.info("Sending CSRF Token: {}", csrfToken);
        headers.set("X-CSRF-TOKEN", csrfToken);
        headers.add(HttpHeaders.COOKIE, cookie + "; JSESSIONID=" + sessionId);  // 값 추가

        // when (실제 HTTP POST 요청)
        HttpEntity<UserEntity> request = new HttpEntity<>(user, headers);
        ResponseEntity<UserEntity> response = restTemplate.postForEntity(userUrl, request, UserEntity.class);

        // then (응답 검증)
        assertEquals(HttpStatus.CREATED, response.getStatusCode());  // HTTP 201 응답 확인
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());  // ID가 생성되었는지 확인
        assertEquals(user.getEmail(), response.getBody().getEmail());  // 이메일 확인
    }


    @Test
    @Order(2)
    @DisplayName("유저 목록 조회 - DB 데이터 검증 및 HTTP 응답 테스트")
    void testGetUsers() {
        // given
        String url = "/users?page=0&size=10";

        // when
        // 쿼리 파라미터만 포함된 일반 GET 요청이라면 null을 넣어 헤더 없이 요청할 수도 있지만
        //  여기선 jwt 토큰 전송을 위해 헤더 포함 요청 전달
        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer my-jwt-token");  // JWT 토큰 직접 추가
        headers.add(HttpHeaders.COOKIE, cookie);    // 헤더에 JWT 토큰이 포함된 쿠키 추가
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));  // JSON 응답 요청

        HttpEntity<List<User>> request = new HttpEntity<>(headers);
        ResponseEntity<List<UserEntity>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,  // 헤더 포함
                new ParameterizedTypeReference<>() {}
        );

        List<UserEntity> users = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());  // HTTP 200 응답 확인
        assertNotNull(users);  // 응답이 null이 아닌지 확인
        assertTrue(users instanceof List);  // 응답이 배열(List)인지 확인
        assertTrue(users.size() >= 0);  // 최소 0개 이상인지 확인
        assertTrue(users.size() > 0);  // 1개 이상인지 확인 (데이터가 있는 경우)
    }

    @Test
    @DisplayName("유저 단건 조회 - DB와 HTTP 응답 검증")
    void testGetUserById() {
        // g
        String url = "/users/{id}";
        Long userId = 2L;

        // w
        // getForEntity()는 body나 header를 커스터마이징 불가능.
//        ResponseEntity<UserEntity> response = restTemplate.getForEntity(url, UserEntity.class, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<UserEntity> request = new HttpEntity<>(headers);

        // CSRF 토큰, 인증 쿠키 등을 추가한 헤더 커스터마이징을 위해 exchange 사용
        ResponseEntity<UserEntity> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                UserEntity.class,
                userId
        );

        // t
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserEntity user = response.getBody();
        assertNotNull(user);
        assertEquals(2L, user.getId());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    @DisplayName("유저 수정 - DB 및 HTTP 응답 검증")
    void testUpdateUser() {
        // g
        String url = "/users/{id}";
        UserEntity updateUser = new UserEntity();
        updateUser.setId(2L);
        updateUser.setName("newname");

        // w
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Csrf-Token", csrfToken);
        headers.add(HttpHeaders.COOKIE, cookie + "; JSESSIONID=" + sessionId);
        HttpEntity<UserEntity> request = new HttpEntity<>(updateUser, headers);

        ResponseEntity<UserEntity> response = restTemplate.exchange(
                url,  // ID를 경로 변수로 전달
                HttpMethod.PUT,
                request,
                UserEntity.class,
                updateUser.getId()   // ID 값 전달
         );

        // t
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserEntity user = response.getBody();
        assertNotNull(user);
        assertEquals(2L, user.getId());
        assertEquals("newname", user.getName());
    }

    @Test
    @DisplayName("유저 삭제 - DB 및 HTTP 응답 검증")
    void testDeleteUser() {
        // g
        String url = "/users/{id}";
        Long userId = 2L;

        // w
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Csrf-Token", csrfToken);
        headers.add(HttpHeaders.COOKIE, cookie + "; JSESSIONID=" + sessionId);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> res = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            request,
            Void.class,
            userId
        );

        // t
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
    }


    void testRSARequest() {
        // given (요청할 URL과 파라미터 설정)
        String rsaUrl = "/rsa/generate";

        HttpHeaders rsaHeaders = new HttpHeaders();
        rsaHeaders.add(HttpHeaders.COOKIE, cookie);  // 값 추가
//        rsaHeaders.add(HttpHeaders.COOKIE, cookie + "; JSESSIONID=" + sessionId);  // 값 추가

        // ✅ RSA 공개 키 요청 (세션 유지)
        ResponseEntity<String> rsaResponse = restTemplate.exchange(
                rsaUrl,
                HttpMethod.GET,
                new HttpEntity<>(null, rsaHeaders),
                String.class
        );
        assertThat(rsaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> cookies = rsaResponse.getHeaders().get("Set-Cookie");
        String rsaSessionId = null;
        for (String cookie : cookies) {
            if (cookie.startsWith("JSESSIONID")) {
                rsaSessionId = cookie.split(";")[0].split("=")[1];  // JSESSIONID 값 추출
                break;
            }
        }

        // ✅ 공개 키 저장
        String publicKey = rsaResponse.getBody();
        assertThat(publicKey).isNotNull();

        // ✅ 비밀번호 RSA 암호화
        String originalPassword = user.getPassword();
        String encryptedPassword;
        try {
            encryptedPassword = RSAUtil.encryptWithPublicKey(originalPassword, RSAUtil.getPublicKeyFromString(publicKey));
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 암호화 실패", e);
        }

        // ✅ 암호화된 비밀번호 적용
        user.setPassword(encryptedPassword);
    }
}
