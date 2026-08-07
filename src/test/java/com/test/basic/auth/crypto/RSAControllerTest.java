package com.test.basic.auth.crypto;

import com.test.basic.auth.AuthController;
import com.test.basic.auth.RefreshToken;
import com.test.basic.auth.RefreshTokenRepository;
import com.test.basic.auth.RefreshTokenService;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.common.support.AuthTestSupport;
import com.test.basic.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RSAController.class, AuthController.class})
@Import({SecurityConfig.class, AuthTestSupport.class})
public class RSAControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(RSAControllerTest.class);

    // HTTP 요청 ====================
    @Autowired
    MockMvc mockMvc;

    // 사용자 인증 ====================
    @MockitoBean
    private UserService userService;    // AuthController 의존성 주입용
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;            // AuthController 의존성 주입용
    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;      // AuthController 의존성 주입용

    @Autowired
    private AuthTestSupport authTestSupport;

    private AuthTestSupport.JwtTokenInfo jwtTokenInfo;


    @BeforeEach
    void setup() throws Exception {
        authTestSupport.createTestAdminUser();
        UserDetails mockUser = authTestSupport.createTestAdminUser();
        when(customUserDetailsService.loadUserByUsername(mockUser.getUsername())).thenReturn(mockUser);

        // 로그인 응답에서 refresh_token 쿠키를 만들 때 사용되므로 stub 필요 (없으면 NPE로 500)
        when(refreshTokenService.createRefreshToken(anyLong()))
                .thenReturn(RefreshToken.builder().token("test-refresh-token").build());

        this.jwtTokenInfo = authTestSupport.loginAdminAndCreateJWT("admin", "admin");
    }

    @Test
    @DisplayName("RSA 암호화 키페어 생성 테스트")
    void testGenerateRSA() throws Exception {
        MvcResult response = mockMvc
                .perform(get("/rsa/generate")
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String publicKey = response.getResponse().getContentAsString();

        logger.info(publicKey);
        assertThat(publicKey).isNotNull();
    }

}
