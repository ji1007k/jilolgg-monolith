package com.test.basic.auth.crypto;

import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.AuthController;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.CustomUserDetailsService;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.users.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RSAController.class, AuthController.class})
@Import(SecurityConfig.class)
public class RSAControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(RSAControllerTest.class);

    // HTTP 요청 ====================
    @Autowired
    MockMvc mockMvc;

    // 사용자 인증 ====================
    @MockBean   
    private UserService userService;    // AuthController 의존성 주입용
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    private String jwtAccess;
    private Cookie jwtAccessCookie;


    @BeforeEach
    void loginAdminAndGenerateJWT() throws Exception {
        logger.info("======================================================");
        logger.info("...테스트용 관리자 계정 로그인 및 JWT 토큰 발급");
        logger.info("======================================================");
        UserDetails mockUser = User.withUsername("admin")
                .password("$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy")
                .authorities("ADMIN")
                .build();

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
        this.jwtAccessCookie = new Cookie("access_token", this.jwtAccess);
    }

    @Test
    @DisplayName("RSA 암호화 키페어 생성 테스트")
    void testGenerateRSA() throws Exception {

        MvcResult response = mockMvc
                .perform(get("/rsa/generate")
                        .cookie(this.jwtAccessCookie))
                .andExpect(status().isOk())
                .andReturn();

        String publicKey = response.getResponse().getContentAsString();

        logger.info(publicKey);
        assertThat(publicKey).isNotNull();
    }

}
