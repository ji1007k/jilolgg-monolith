package com.test.basic.common.support;

import com.test.basic.auth.security.user.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestComponent
@Slf4j
public class AuthRestTemplateTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;


    /**
     * 테스트용 관리자 생성
     */
    public UserDetails createTestAdminUser() {
        return this.createTestUser(
                "admin",
                "$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy",
                "admin",
                "SCOPE_ADMIN");
    }

    private UserDetails createTestUser(String email, String password, String username, String autorities) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(autorities.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        UserDetails mockUser = new CustomUserDetails(
                1L, // 혹은 UUID.randomUUID()
                email,      // email
                password,   // password
                username,    // username
                authorities
        );

        return mockUser;
    }

    // TestRestTemplate 기반 로그인
    public String loginAdminAndCreateJWT(String email, String password) {
        String userInfo = String.join(":", email, password);
        String base64Encoded = Base64.getEncoder().encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.GET,
                new HttpEntity<>(null, createHeaders("Basic " + base64Encoded)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 쿠키 처리 로직
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        log.info("Set-Cookie: {}", cookies);

        List<String> filteredCookies = new ArrayList<>();
        for (String cookie : cookies) {
            filteredCookies.add(cookie.split(";")[0]); // 첫 번째 값만 저장 (만료일, secure 등 옵션 제거)
        }

        return String.join("; ", filteredCookies); 
    }

    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // 방법 1. 쿠키 저장 O - 쿠키에서 CSRF 토큰 추출 (STATELESS) ============
    // CSRF 토큰 생성 (TestRestTemplate 기반)
    public List<String> createCsrfToken(String jwtTokenCookieStr) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, jwtTokenCookieStr);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> csrfResponse = restTemplate.exchange(
                "/csrf",
                HttpMethod.GET,
                request,
                String.class
        );

        // XSRF-TOKEN=e1b7a378-b833-4e20-8d20-3756b9173fa6; Path=/
        return csrfResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    }

    public StringBuilder getCookieBuilder(List<String> setCookies) {
        StringBuilder cookieBuilder = new StringBuilder();

        for (String c : setCookies) {
            String[] parts = c.split(";", 2); // "XSRF-TOKEN=xxx; Path=/..."
            String cookiePart = parts[0];
            cookieBuilder.append(cookiePart).append("; ");
        }

        return cookieBuilder;
    }

    public String getCsrfTokenFromCookies(List<String> setCookies) {
        for (String c : setCookies) {
            String[] parts = c.split(";", 2); // "XSRF-TOKEN=xxx; Path=/..."
            String cookiePart = parts[0];

            if (cookiePart.startsWith("XSRF-TOKEN")) {
                return cookiePart.split("=")[1];
            }
        }

        return null;
    }

    // 방법 2. 쿠키 저장X - 헤더에서 CSRF 토큰 추출 (JSESSIONID 필요) ============
    public String getCsrfTokenFromHeader(HttpHeaders headers, String key) {
        return headers.getFirst(key);
    }

    public String getJSessionIdFromHeader(List<String> cookies, String key) {
        for (String cookie : cookies) {
            if (cookie.startsWith(key)) {
                return cookie.split(";")[0].split("=")[1];  // JSESSIONID 값 추출
            }
        }

        return null;
    }

}