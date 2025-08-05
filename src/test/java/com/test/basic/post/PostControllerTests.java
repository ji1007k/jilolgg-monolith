package com.test.basic.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.basic.auth.AuthController;
import com.test.basic.auth.csrf.CsrfTokenController;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.common.support.AuthTestSupport;
import com.test.basic.post.batch.PostBatchProcessor;
import com.test.basic.user.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest({ PostController.class, AuthController.class, CsrfTokenController.class })
@Import({ SecurityConfig.class, AuthTestSupport.class}) // Security 설정
public class PostControllerTests {
    private static final Logger logger = LoggerFactory.getLogger(PostControllerTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTestSupport authTestSupport;

    @MockBean
    private UserService userService;    // AuthContoller 의존성 주입용

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PostService postService;

    @MockBean
    private PostBatchProcessor postBatchProcessor;

    private AuthTestSupport.JwtTokenInfo jwtTokenInfo;
    private Post post;

    /*
     * CSRF 테스트 처리 방법 3가지:
     *
     * 1. 간편한 방법 (현재 사용 중):
     *    .with(csrf()) - Spring Security Test에서 제공하는 편의 메서드
     *    실제 CSRF 토큰 없이도 CSRF 검증을 통과시켜줌
     *      => 현재 클래스 참고
     *
     * 2. Stateful 방식 (Session 기반, 주석 처리):
     *    HttpSession에 CSRF 토큰을 저장하고 헤더로 전달
     *    전통적인 방식이지만 서버에 세션 상태 저장 필요
     *
     * 3. Stateless 방식 (Cookie 기반, 실제 프로젝트 적용):
     *    CSRF 토큰을 쿠키에 저장하고 헤더로 전달
     *    서버 무상태성 유지하면서 CSRF 보호 가능
     *    Double Submit Cookie 패턴 사용
     *      => UserIntegrationTest.java 참고
     */

    // 실제 CSRF 토큰 방식을 위한 변수들 (현재 미사용)
    // private MockHttpSession mockHttpSession;  // Stateful 방식용
    // private String csrfToken;                 // 실제 CSRF 토큰값

    @BeforeAll
    static void setUp() {
        logger.info("==========================================");
        logger.info("...Start: [{}]", PostControllerTests.class.getSimpleName());
    }

    @BeforeEach
    void beforeEach() throws Exception {
        logger.info("......");

        post = new Post();
        post.setId(1L);
        post.setTitle("게시글 제목");
        post.setContent("게시글 내용");
        post.setCategory("게시글 카테고리");

        // 가짜 UserDetails 설정
        UserDetails mockUser = authTestSupport.createTestAdminUser();
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockUser);

        // 로그인을 통한 JWT 토큰 발급
        this.jwtTokenInfo = authTestSupport.loginAdminAndCreateJWT("admin", "admin");


        /*
         * 실제 CSRF 토큰 방식 설정 (현재 미사용):
         *
         * === Stateful 방식 (Session 기반) ===
         * MockHttpSession mockHttpSession = new MockHttpSession();
         * this.csrfToken = authTestSupport.createCsrfToken(
         *         this.jwtTokenInfo.getJwtAccessCookie(),
         *         mockHttpSession
         * );
         *
         * === Stateless 방식 (Cookie 기반, 실제 프로젝트 적용) ===
         * List<String> setCookies = authTestSupport.createCsrfToken(cookie);
         * csrfToken = authTestSupport.getCsrfTokenFromCookies(setCookies);
         *
         * // CSRF 요청 시 받은 모든 Set-Cookie 값을 다시 전송해야 함 (CSRF 토큰 포함)
         * this.cookie = this.cookie + "; " + authTestSupport.getCookieBuilder(setCookies);
         *
         * 장점: 서버 무상태성 유지, 확장성 좋음
         * 단점: 쿠키 파싱 로직 필요, 설정 복잡
         */
    }

    @AfterAll
    static void afterAll() {
        logger.info("...End: [{}]", PostControllerTests.class.getSimpleName());
        logger.info("==========================================");
    }

    @Test
    void testCreatePost() throws Exception {
        when(postService.createPost(any(Post.class))).thenReturn(post);

        MvcResult result = mockMvc
                .perform(post("/posts/non-batch")
                                // 현재 사용 중: 간편한 CSRF 검증 통과
                                .with(csrf())
                                .cookie(this.jwtTokenInfo.getJwtAccessCookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(post))

                        /* 실제 CSRF 토큰 방식 (미사용):
                         * .header("X-CSRF-TOKEN", this.csrfToken) // CSRF 토큰을 헤더로 전달
                         * .session(mockHttpSession)  // CSRF 토큰 검증을 위한 세션 포함
                         */
                )
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString(Charset.defaultCharset());
        Post resultPost = objectMapper.readValue(responseContent, Post.class);

        assertThat(resultPost).isNotNull();
        assertThat(resultPost.getTitle()).isEqualTo(post.getTitle());
        assertThat(resultPost.getContent()).isEqualTo(post.getContent());
    }

    @Test
    void testGetPostById() throws Exception {
        Long id = 1L;
        when(postService.getPostById(anyLong())).thenReturn(Optional.of(post));

        MvcResult result = mockMvc
                .perform(get("/posts/{id}", id)
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Post post = objectMapper.readValue(response, Post.class);

        assertThat(post).isNotNull();
        assertThat(post.getId()).isEqualTo(id);
    }

    @Test
    void testGetAllPosts() throws Exception {
        List<Post> posts = List.of(post);
        when(postService.getAllPosts(anyString(), anyString())).thenReturn(posts);

        MvcResult result = mockMvc
                .perform(get("/posts")
                        .param("keyword", "")
                        .param("sort", "id,asc")
                        .cookie(this.jwtTokenInfo.getJwtAccessCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        List<Post> resultPosts = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(resultPosts).isNotNull();
        assertThat(resultPosts.size()).isGreaterThan(0);
    }

    @Test
    void testUpdatePost() throws Exception {
        // 정보 수정
        post.setTitle("수정된 제목");
        post.setContent("수정된 내용");
        post.setUpdatedDt(LocalDateTime.now());

        when(postService.updatePost(any(Post.class))).thenReturn(post);

        MvcResult result = mockMvc
                .perform(put("/posts/{id}", post.getId())
                                .cookie(this.jwtTokenInfo.getJwtAccessCookie())
                                // 간편한 CSRF 검증 통과
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(post))

                        /* 실제 CSRF 토큰 방식 (미사용):
                         * .header("X-CSRF-TOKEN", this.csrfToken)
                         * .session(mockHttpSession)
                         */
                )
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Post updatedPost = objectMapper.readValue(response, Post.class);

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getId()).isEqualTo(post.getId());
        assertThat(updatedPost.getTitle()).isEqualTo(post.getTitle());
        assertThat(updatedPost.getContent()).isEqualTo(post.getContent());
        assertThat(updatedPost.getUpdatedDt()).isNotNull();
    }

    @Test
    void testDeletePost() throws Exception {
        doNothing().when(postService).deletePost(eq(1L));

        mockMvc
                .perform(delete("/posts/{id}", post.getId())
                                .with(csrf()) // 간편한 CSRF 검증 통과
                                .cookie(this.jwtTokenInfo.getJwtAccessCookie())

                        /* 실제 CSRF 토큰 방식 (미사용):
                         * .header("X-CSRF-TOKEN", this.csrfToken)
                         * .session(mockHttpSession)
                         */
                )
                .andExpect(status().isNoContent());
    }


    /*
     * CSRF 테스트 방법 선택 가이드:
     *
     * 1. .with(csrf()) 사용 시기:
     *    - 빠른 단위 테스트가 목적일 때
     *    - CSRF 로직 자체를 테스트하는 게 아닐 때
     *    - 대부분의 컨트롤러 테스트에서 권장
     *
     * 2. Stateful 방식 (Session) 사용 시기:
     *    - 전통적인 세션 기반 애플리케이션
     *    - 단일 서버 환경
     *    - 간단한 CSRF 구현 필요 시
     *
     * 3. Stateless 방식 (Cookie) 사용 시기:
     *    - 마이크로서비스 아키텍처
     *    - 로드 밸런싱된 다중 서버 환경
     *    - RESTful API 설계 원칙 준수
     *    - JWT 기반 인증과 함께 사용
     *    - 서버 확장성이 중요한 경우
     *
     * Double Submit Cookie 패턴 (Stateless 방식):
     * 1. 클라이언트가 CSRF 토큰 요청
     * 2. 서버가 CSRF 토큰을 쿠키와 응답에 모두 포함하여 반환
     * 3. 클라이언트가 요청 시 쿠키(자동)와 헤더(수동) 모두에 토큰 포함
     * 4. 서버가 쿠키와 헤더의 토큰 일치 여부 검증
     * 5. 일치하면 요청 처리, 불일치하면 403 에러
     */
}

