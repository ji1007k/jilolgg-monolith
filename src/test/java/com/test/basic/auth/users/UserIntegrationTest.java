package com.test.basic.auth.users;

import com.test.basic.handler.AcceptanceTestExecutionListener;
import com.test.basic.utils.RSAUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
class UserIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;  // 실제 HTTP 요청을 보내는 객체

    @BeforeEach
    void setUp() {}

    @Test
    @Order(1)
    @DisplayName("유저 생성 - DB 저장 및 HTTP 응답 테스트")
    void testCreateUser() {
        // given (요청할 URL과 파라미터 설정)
        String url = "/api/users";
        User user = new User(null, "newpass", "new@example.com", "newuser", null, null, null);
        // 요청 데이터 중 사용자 비밀번호 rsa 암호화

        String rsaUrl = "/api/users/rsa";
        ResponseEntity<String> res = restTemplate.getForEntity(rsaUrl, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 세션 쿠키 추출 (Set-Cookie 헤더에서 JSESSIONID 찾기)
        List<String> cookies = res.getHeaders().get("Set-Cookie");
        String sessionId = null;
        for (String cookie : cookies) {
            if (cookie.startsWith("JSESSIONID")) {
                sessionId = cookie.split(";")[0].split("=")[1];  // JSESSIONID 값을 추출
                break;
            }
        }
        assertNotNull(sessionId);
        System.out.println("Session ID: " + sessionId);

        String publicKey = res.getBody();
        assertThat(publicKey).isNotNull();

        try {
            user.setPassword(RSAUtil.encryptWithPublicKey(user.getPassword(), RSAUtil.getPublicKeyFromString(publicKey)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // when (실제 HTTP POST 요청)
        // 두 번째 요청 (세션 쿠키 포함하여 보내기)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", "JSESSIONID=" + sessionId);  // 세션 쿠키를 헤더에 추가

        // Spring이 자동으로 JSON으로 직렬화(변환)해서 요청 본문에 담아줌
        HttpEntity<User> request = new HttpEntity<>(user, headers);

        ResponseEntity<User> response = restTemplate.postForEntity(url, request, User.class);

        // then (응답 검증)
        assertEquals(HttpStatus.CREATED, response.getStatusCode());  // HTTP 201 응답 확인
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());  // ID가 생성되었는지 확인
        assertEquals("new@example.com", response.getBody().getEmail());  // 이메일 확인
    }

    @Test
    @Order(2)
    @DisplayName("유저 목록 조회 - DB 데이터 검증 및 HTTP 응답 테스트")
    void testGetUsers() {
        // given
        String url = "/api/users?page=0&size=10";

        // when
        // 쿼리 파라미터만 포함된 일반 GET 요청이라면 null을 넣어 헤더 없이 요청 가능
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer my-jwt-token");  // JWT 인증 추가
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));  // JSON 응답 요청

//        HttpEntity<List<User>> request = new HttpEntity<>(headers);
        ResponseEntity<List<User>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,   // requestEntity,  // 헤더 포함
                new ParameterizedTypeReference<>() {}
        );

        List<User> users = response.getBody();

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
        String url = "/api/users/{id}";
        Long userId = 1L;;

        // w
        ResponseEntity<User> response = restTemplate.getForEntity(url, User.class, userId);

        // t
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User user = response.getBody();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    @DisplayName("유저 수정 - DB 및 HTTP 응답 검증")
    void testUpdateUser() {
        // g
        String url = "/api/users/{id}";
        User updateUser = new User();
        updateUser.setId(1L);
        updateUser.setName("newname");

        // w
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<User> request = new HttpEntity<>(updateUser, headers);

        ResponseEntity<User> response = restTemplate.exchange(
                url,  // ID를 경로 변수로 전달
                HttpMethod.PUT,
                request,
                User.class,
                updateUser.getId()   // ID 값 전달
         );

        // t
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User user = response.getBody();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("newname", user.getName());
    }

    @Test
    @DisplayName("유저 삭제 - DB 및 HTTP 응답 검증")
    void testDeleteUser() {
        // g
        String url = "/api/users/{id}";
        Long userId = 1L;

        // w
        ResponseEntity<Void> res = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            Void.class,
            userId
        );

        // t
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
    }

}
