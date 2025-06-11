package com.test.basic.auth;

import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.user.UserEntity;
import com.test.basic.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Tag(name = "[AUTH] 인증/인가 API", description = "사용자 인증/인가 관련 API")
@RequestMapping("/auth")
@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;

    @Value("${cookie.secure}")
    private boolean isSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping(value = { "/signup" })
    @Operation(summary = "회원가입", description = "회원가입 API")
    public ResponseEntity signupPage(@RequestBody UserEntity user) {
        // save
        UserEntity newUser = userService.createUser(user);

        if (newUser != null) {
            return ResponseEntity.ok("/auth/login");
        }

        return ResponseEntity.badRequest().build();
    }

    // Spring Security는 Basic Auth 방식에서 자동으로 username과 password를 추출해서 Authentication 객체에 넣어줌
    @GetMapping("/login")
    @Operation(summary = "로그인", description = "로그인 API")
    @SecurityRequirement(name = "BasicAuth")  // 🔥 Swagger에서 Basic Auth로 인증 가능
    public ResponseEntity login(Authentication authentication, HttpServletResponse response) throws Exception {
        // 1. Basic Authentication 정보는 이미 authentication 객체에 담겨 있음
        // 여기서 authentication의 principal === UserDetails 객체
//        String username = authentication.getName(); // Basic Auth에서 username 추출
//		String password = (String) authentication.getCredentials(); // Basic Auth에서 password 추출

        // 2. 인증 성공 시 JWT 토큰 생성
        // authentication이 이미 인증된 상태라면 불필요한 authenticate() 호출을 방지
        logger.info("Login successful for username: {}", authentication.getName()); // 여기선 UserDetails의 username
        Jwt accessToken = jwtTokenProvider.createToken(authentication); // JWT 토큰 생성

        ResponseCookie accessTokenCookie = jwtTokenProvider.makeResponseCookie("access_token", accessToken.getTokenValue());
        ResponseCookie refreshTokenCookie = jwtTokenProvider.makeRefreshToken(authentication);

        // 서버가 Set-Cookie 헤더로 보낸 쿠키는 자동으로 클라이언트 브라우저에 저장된다
        // 사용자는 쿠키를 수동으로 저장할 필요가 없으며, 브라우저가 이를 처리.
        // 클라이언트가 이후 동일 도메인에 요청을 보낼 때, 브라우저는 저장된 쿠키를 자동으로 포함하여 서버에 요청을 보냅니다
        response.setHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // JWT에서 만료 시간 (exp 클레임) 추출
        Instant expirationTime = accessToken.getExpiresAt();

        // 한국 시간으로 변환
        LocalDateTime expirationTimeKST = LocalDateTime.ofInstant(expirationTime, ZoneId.of("Asia/Seoul"));
        logger.info("Expiration Time in KST (LocalDateTime): {}", expirationTimeKST);

        Map<String, String> result = Map.of(
                "expirationTime", expirationTimeKST.toString(),
                "mainPageUrl", "/",
                "userId", accessToken.getSubject()
        );

        // 상태 코드 200과 함께 빈 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * 쿠키 삭제는 기존 쿠키와 name, path, domain, secure, sameSite 등이 완전히 일치해야 합니다.
     * maxAge(0)만 바꿔서 보내야 브라우저가 기존 쿠키를 "덮어써서 삭제"
     */
    @GetMapping("/logout")
    @SecurityRequirement(name = "BearerAuth")  // 🔥 Swagger에서 JWT 인증 필요
    @Operation(summary = "로그아웃", description = "로그아웃 API")
    public ResponseEntity logout(HttpServletResponse response) {
        // 세션 쿠키를 만료시켜서 삭제
        ResponseCookie cookie = ResponseCookie.from("access_token", null)
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(0)  // 쿠키 만료 시간 0: 즉시 만료
                .build();

        response.addHeader("Set-Cookie", cookie.toString());  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

        // refreshToken 쿠키에 대해서도 동일하게 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", null)
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 추가적으로 헤더에서 JWT 토큰 삭제 (필터에서 처리되는 부분)
        response.setHeader("Authorization", ""); // Authorization 헤더를 비워서 토큰 삭제

        // 로그아웃 후 인증 정보를 삭제
        SecurityContextHolder.clearContext();

        // 메인 페이지 url 전달
        Map<String, Object> result = Map.of(
                "success", true,
                "mainPageUrl", "/"
        );

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping(value = {  "/token/refresh" })
    @SecurityRequirement(name = "BearerAuth")  // 🔥 Swagger에서 JWT 인증 필요
    @Operation(summary = "JWT 토큰 갱신", description = "JWT 토큰 갱신 API")
    public ResponseEntity refreshToken(Authentication authentication, HttpServletResponse response) {
        Jwt accessToken = jwtTokenProvider.makeAccessToken(authentication);
        ResponseCookie accessTokenCookie = jwtTokenProvider.makeResponseCookie("access_token", accessToken.getTokenValue());
        ResponseCookie refreshTokenCookie = jwtTokenProvider.makeRefreshToken(authentication);

        // 서버가 Set-Cookie 헤더로 보낸 쿠키는 자동으로 클라이언트 브라우저에 저장된다
        // 사용자는 쿠키를 수동으로 저장할 필요가 없으며, 브라우저가 이를 처리.
        // 클라이언트가 이후 동일 도메인에 요청을 보낼 때, 브라우저는 저장된 쿠키를 자동으로 포함하여 서버에 요청을 보냅니다
        response.setHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // JWT에서 만료 시간 (exp 클레임) 추출
        Instant expirationTime = accessToken.getExpiresAt();

        // 한국 시간으로 변환
        LocalDateTime expirationTimeKST = LocalDateTime.ofInstant(expirationTime, ZoneId.of("Asia/Seoul"));
        logger.info("Expiration Time in KST (LocalDateTime): {}", expirationTimeKST);

        Map<String, String> result = Map.of(
                "expirationTime", expirationTimeKST.toString(),
                "mainPageUrl", "/"
        );

        // 상태 코드 200과 함께 빈 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }


    // TODO 
    //  - Nextjs 프론트엔드 프로젝트 쪽으로 기능 분리

    //	@PreAuthorize("hasAuthority('ADMIN') and #user.username == authentication.name")
   /* @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/mypage/admin")
//	public ResponseEntity<String> getAdminPage(@PathVariable String username, @RequestBody User user) {
    public ResponseEntity adminPage(Authentication authentication) {
        return ResponseEntity.ok("Hello, " + authentication.getAuthorities() + ": " + authentication.getName() + "!");
    }

    // 글로벌 설정을 통해 권한 확인 예
    @GetMapping("/mypage/manager")
    public ResponseEntity managerPage(Authentication authentication) {
        return ResponseEntity.ok("Hello, " + authentication.getAuthorities() + ": " + authentication.getName() + "!");
    }

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/mypage")
    public ResponseEntity hello(Authentication authentication) {
        return ResponseEntity.ok("Hello, " + authentication.getName() + "!");
    }*/
}
