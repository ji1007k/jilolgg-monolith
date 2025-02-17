package com.test.basic.auth;

import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.users.UserEntity;
import com.test.basic.users.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import java.util.Optional;

@Tag(name = "Auth API", description = "권한 관리 API")
@RequestMapping("/auth")
@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;

    private final UserRepository userRepository;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }


    @GetMapping(value = { "/signup" })
    public String signupPage() {
        return "signup";
    }

    @GetMapping(value = { "/login" })
    public String loginPage() {
        return "login";
    }

    //	@PreAuthorize("hasAuthority('ADMIN') and #user.username == authentication.name")
    @PreAuthorize("hasAuthority('ADMIN')")
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
    }

    @PostMapping(value = { "/signup" })
    public ResponseEntity signupPage(@RequestBody UserEntity user) {

        String encodedPwd = new BCryptPasswordEncoder().encode(user.getPassword());
        user.setPassword(encodedPwd);

        // save
        Optional<UserEntity> newUser = Optional.of(userRepository.save(user));

        if (newUser.isPresent()) {
            return ResponseEntity.ok("/auth/login");
        }

        return ResponseEntity.badRequest().build();
    }

    // Spring Security는 Basic Auth 방식에서 자동으로 username과 password를 추출해서 Authentication 객체에 넣어줌
    @PostMapping("/login")
    public ResponseEntity login(Authentication authentication, HttpServletResponse response) throws Exception {
        // 1. Basic Authentication 정보는 이미 authentication 객체에 담겨 있음
        String username = authentication.getName(); // Basic Auth에서 username 추출
//		String password = (String) authentication.getCredentials(); // Basic Auth에서 password 추출

        // 4. 인증 성공 시 JWT 토큰 생성
        // authentication이 이미 인증된 상태라면 불필요한 authenticate() 호출을 방지
        logger.info("Login successful for user: {}", username);
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
                "mainPageUrl", "/"
        );

        // 상태 코드 200과 함께 빈 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        // 세션 쿠키를 만료시켜서 삭제
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);  // 자바스크립트에서 쿠키를 접근할 수 없도록 설정
        cookie.setPath("/");  // 쿠키의 경로를 현재 웹사이트의 루트로 설정
        cookie.setMaxAge(0);  // 쿠키 만료 시간 설정 (0으로 설정하면 즉시 만료됨)
//        cookie.setSecure(true);  // HTTPS 연결에서만 쿠키가 전달됨
        cookie.setSecure(false);  // HTTP 연결에서 쿠키가 전달됨
        response.addCookie(cookie);  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

        Cookie refreshTokenCookie = new Cookie("refresh_token", null);
        refreshTokenCookie.setHttpOnly(true);  // 자바스크립트에서 쿠키를 접근할 수 없도록 설정
        refreshTokenCookie.setPath("/");  // 쿠키의 경로를 현재 웹사이트의 루트로 설정
        refreshTokenCookie.setMaxAge(0);  // 쿠키 만료 시간 설정 (0으로 설정하면 즉시 만료됨)
//        refreshTokenCookie.setSecure(true);  // refreshToken 쿠키에 대해서도 동일
        refreshTokenCookie.setSecure(false);  // HTTP 연결에서 쿠키가 전달됨
        response.addCookie(refreshTokenCookie);  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

        // 추가적으로 헤더에서 JWT 토큰 삭제 (필터에서 처리되는 부분)
        response.setHeader("Authorization", ""); // Authorization 헤더를 비워서 토큰 삭제

        // 로그아웃 후 인증 정보를 삭제
        SecurityContextHolder.clearContext();

        // 홈 페이지나 다른 페이지로 리디렉션
        return "redirect:/";
    }

    @PostMapping(value = {  "/token/refresh" })
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
}
