package com.test.basic.common.support;

import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.user.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SpringBootTest에선 자동 스캔되지 않아 @Import 필요.
//  * 테스트클래스와 동일한 패키지 또는 하위 패키지에 위치할 경우 자동 스캔됨
@TestComponent  
@Slf4j
public class AuthTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;


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

    public JwtTokenInfo loginAdminAndCreateJWT(String email, String password) throws Exception {
        log.info("======================================================");
        log.info("...테스트용 관리자 계정 로그인 및 JWT 토큰 발급");
        log.info("======================================================");

        String userInfo = String.join(":", List.of(email, password));
        Base64 base64Encoded = Base64.encode(userInfo.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc
                .perform(get("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        log.info("Set-Cookie: {}", setCookieHeader);

        return new JwtTokenInfo(
                getJwtTokenStr("access_token", result.getResponse().getCookies()),
                getJwtTokenStr("refresh_token", result.getResponse().getCookies())
        );
    }

    private String getJwtTokenStr(String key, Cookie[] cookies) {
        return jwtTokenProvider.getJwtStrFromCookie(cookies, key);
    }

    /**
     * CSRF 토큰 발급
     * @param jwtAccessTokenCookie
     * @param session
     */
    public String createCsrfToken(Cookie jwtAccessTokenCookie, MockHttpSession session) throws Exception {
        log.info("======================================================");
        log.info("...CSRF 토큰 발급");
        log.info("======================================================");

        return mockMvc.perform(get("/csrf")
                        .cookie(jwtAccessTokenCookie)
                        .session(session))
                .andReturn()
                .getResponse()
                .getHeader("X-CSRF-TOKEN");
    }

    // TODO 자주 사용하는 MockMvc 요청 패턴들 메서드로 추상화
    //  ..



// 이너클래스 -============================================
    public static class JwtTokenInfo {
        @Getter private String jwtAccess;
        @Getter private String jwtRefresh;
        @Getter private Cookie jwtAccessCookie;
        @Getter private Cookie jwtRefreshCookie;

        public JwtTokenInfo(String accessTokenStr, String refreshTokenStr) {
            this.jwtAccess = accessTokenStr;
            this.jwtRefresh = refreshTokenStr;
            this.jwtAccessCookie = new Cookie("access_token", this.jwtAccess);;
            this.jwtRefreshCookie = new Cookie("refresh_token", this.jwtRefresh);;
        }
    }

}
