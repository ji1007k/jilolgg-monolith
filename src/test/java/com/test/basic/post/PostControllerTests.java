package com.test.basic.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.AuthController;
import com.test.basic.auth.csrf.CsrfTokenController;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetails;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.post.batch.PostBatchProcessor;
import com.test.basic.user.UserService;
import jakarta.servlet.http.Cookie;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
@Import({ SecurityConfig.class }) // Security 설정
public class PostControllerTests {
    private static final Logger logger = LoggerFactory.getLogger(PostControllerTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;    // AuthContoller 의존성 주입용

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PostService postService;

    @MockBean
    private PostBatchProcessor postBatchProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    private Post post;

    private MockHttpSession mockHttpSession;
    private String csrfToken;

    private String jwtAccessToken;
    private Cookie jwtTokenCookie;

    @BeforeAll
    static void setUp() {
        logger.info("==========================================");
        logger.info("...Start: [{}]", PostControllerTests.class.getSimpleName());
    }

    @BeforeEach
//    @WithMockUser(username = "admin", authorities = {"SCOPE_ADMIN"})
    void beforeEach() throws Exception {
        logger.info("......");

        // MockHttpSession 설정 (csrf 토큰 검증을 위해 토큰을 생성할 세션을 전역변수로 설정)
        mockHttpSession = new MockHttpSession();

        post = new Post();
        post.setId(1L);
        post.setTitle("게시글 제목");
        post.setContent("게시글 내용");
        post.setCategory("게시글 카테고리");

        // ======================
        // 가짜 UserDetails 설정
        UserDetails mockUser = new CustomUserDetails(
                1L, // 혹은 UUID.randomUUID()
                "admin",           // email
                "$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy", // password
                "admin",           // username
                List.of(new SimpleGrantedAuthority("SCOPE_ADMIN"))
        );

        // Mocking 된 `CustomUserDetailsService`가 항상 가짜 유저 반환하도록 설정
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockUser);

        // 로그인을 통한 JWT 토큰 발급
        String userInfo = "admin:admin";
        Base64 base64Encoded = Base64.encode(userInfo.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc
                .perform(get("/auth/login")
//                        .with(httpBasic("user", "password")))
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Set-Cookie 헤더에서 토큰 추출
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        logger.info("Set-Cookie: {}", setCookieHeader);

        // 쿠키를 다음 요청에서 사용할 수 있도록 저장
        this.jwtAccessToken = jwtTokenProvider.getJwtStrFromCookie(result.getResponse().getCookies(), "access_token");
        this.jwtTokenCookie = new Cookie("access_token", this.jwtAccessToken);

        // =================
        // CSRF 검증을 위한 CSRF 토큰 추출
        this.csrfToken = mockMvc.perform(get("/csrf")
                        .cookie(this.jwtTokenCookie)
                        .session(mockHttpSession))  // 세션을 명시적으로 포함
                .andReturn()
                .getResponse()
                .getHeader("X-CSRF-TOKEN"); // CSRF 토큰을 헤더에서 추출

    }

    @AfterAll
    static void afterAll() {
        logger.info("...End: [{}]", PostControllerTests.class.getSimpleName());
        logger.info("==========================================");
    }


    @Test
    void testCreatePost() throws Exception {
        when(postService.createPost(any(Post.class))).thenReturn(post);

        // 2. 추출한 CSRF 토큰을 POST 요청에 헤더로 포함시킴
        MvcResult result = mockMvc
                .perform(post("/posts/non-batch")
//                        .header("X-CSRF-TOKEN", this.csrfToken) // CSRF 토큰 포함
                        .with(csrf())
                        .cookie(this.jwtTokenCookie)
                        .session(mockHttpSession)  // csrf 토큰 검증을 위해 세션을 명시적으로 포함
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())    // HTTP 201 상태 확인
                .andReturn();  // 결과를 MvcResult로 반환받기

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
                        .cookie(this.jwtTokenCookie))
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
                        .cookie(this.jwtTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        List<Post> resultPosts = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(resultPosts).isNotNull();
        assertThat(resultPosts.size()).isGreaterThan(0);
    }

    @Test
    void testUpdatePost() throws JsonProcessingException, Exception {
        // 정보 수정
        post.setTitle("수정된 제목");
        post.setContent("수정된 내용");
        post.setUpdatedDt(LocalDateTime.now());

        when(postService.updatePost(any(Post.class))).thenReturn(post);

        MvcResult result = mockMvc
                .perform(put("/posts/{id}", post.getId())
                        .cookie(this.jwtTokenCookie)
//                        .header("X-CSRF-TOKEN", this.csrfToken) // CSRF 토큰 포함
                        .with(csrf())
                        .session(mockHttpSession)  // csrf 토큰 검증을 위해 세션을 명시적으로 포함
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
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
//                        .header("X-CSRF-TOKEN", this.csrfToken)
                        .with(csrf())
                        .session(mockHttpSession)
                        .cookie(this.jwtTokenCookie))
                .andExpect(status().isNoContent());
    }

}


