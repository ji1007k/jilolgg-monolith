package com.test.basic.auth;

import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.user.UserEntity;
import com.test.basic.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@Tag(name = "01. Auth", description = "사용자 인증 및 계정 관리 API")
@RequestMapping("/auth")
@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${cookie.secure}")
    private boolean isSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider, UserService userService, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping(value = { "/signup" })
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
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
    @Operation(summary = "로그인", description = "ID/PW를 이용해 로그인하고 JWT 쿠키를 발급받습니다.")
    @SecurityRequirement(name = "01_BasicAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공 (Access/Refresh 쿠키 발급)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity login(Authentication authentication, HttpServletResponse response) throws Exception {
        // 1. Basic Authentication 정보는 이미 authentication 객체에 담겨 있음
        // 여기서 authentication의 principal === UserDetails 객체
//        String username = authentication.getName(); // Basic Auth에서 username 추출
//		String password = (String) authentication.getCredentials(); // Basic Auth에서 password 추출

        // 인증 객체 null 체크
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.error("Authentication object is null or principal is null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication failed"));
        }

        // 2. 인증 성공 시 Access JWT 토큰 생성
        logger.info("Login successful for username: {}", authentication.getName());
        Jwt accessToken = jwtTokenProvider.makeAccessToken(authentication);
        
        // 3. Refresh Token 생성 (UUID 방식, DB 저장)
        com.test.basic.auth.security.user.CustomUserDetails userDetails = (com.test.basic.auth.security.user.CustomUserDetails) authentication.getPrincipal();
        
        // userDetails null 체크
        if (userDetails == null || userDetails.getId() == null) {
            logger.error("UserDetails is null or ID is null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "User details not found"));
        }
        
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        ResponseCookie accessTokenCookie = jwtTokenProvider.makeAccessTokenCookie(accessToken.getTokenValue());
        ResponseCookie refreshTokenCookie = jwtTokenProvider.makeRefreshTokenCookie(refreshToken.getToken());

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

        // userId null 체크
        String userId = accessToken.getSubject();
        if (userId == null) {
            logger.error("userId is null from accessToken, userDetails ID: {}", userDetails.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "userId is null"));
        }

        Map<String, String> result = Map.of(
                "expirationTime", expirationTimeKST.toString(),
                "mainPageUrl", "/",
                "userId", userId
        );

        // 상태 코드 200과 함께 빈 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * 쿠키 삭제는 기존 쿠키와 name, path, domain, secure, sameSite 등이 완전히 일치해야 합니다.
     * maxAge(0)만 바꿔서 보내야 브라우저가 기존 쿠키를 "덮어써서 삭제"
     */
    @GetMapping("/logout")
    @SecurityRequirement(name = "02_BearerAuth")
    @Operation(summary = "로그아웃", description = "로그아웃 처리를 하고 브라우저의 JWT 쿠키를 만료시킵니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity logout(HttpServletResponse response) {
        // 세션 쿠키를 만료시켜서 삭제
        ResponseCookie cookie = ResponseCookie.from(JwtTokenProvider.ACCESS_TOKEN_KEY, null)
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(0)  // 쿠키 만료 시간 0: 즉시 만료
                .build();

        response.addHeader("Set-Cookie", cookie.toString());  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

        // refreshToken 쿠키에 대해서도 동일하게 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from(JwtTokenProvider.REFRESH_TOKEN_KEY, null)
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 로그아웃 시 DB에서도 Refresh 토큰 삭제
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.test.basic.auth.security.user.CustomUserDetails userDetails) {
            refreshTokenService.deleteByUserId(userDetails.getId());
        }

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
    @SecurityRequirement(name = "03_CSRF")
    @Operation(summary = "JWT 토큰 갱신", description = "Refresh Token 쿠키를 이용해 새로운 Access Token을 발급받습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "갱신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 유효하지 않음")
    })
    public ResponseEntity refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String requestRefreshToken = jwtTokenProvider.getJwtStrFromCookie(request.getCookies(), JwtTokenProvider.REFRESH_TOKEN_KEY);

        if (requestRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token is missing"));
        }

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Create new authentication object for makeAccessToken
                    com.test.basic.auth.security.user.CustomUserDetails userDetails = new com.test.basic.auth.security.user.CustomUserDetails(
                            user.getId(), user.getEmail(), user.getPassword(), user.getName(), user.getPasswordVersion(),
                            java.util.Arrays.stream(user.getAuthority().split(","))
                                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                    .collect(java.util.stream.Collectors.toList())
                    );
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication = 
                            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    Jwt accessToken = jwtTokenProvider.makeAccessToken(authentication);
                    ResponseCookie accessTokenCookie = jwtTokenProvider.makeAccessTokenCookie(accessToken.getTokenValue());
                    
                    response.setHeader("Set-Cookie", accessTokenCookie.toString());

                    // Return new expiration time
                    return ResponseEntity.ok(Map.of(
                            "expirationTime", LocalDateTime.ofInstant(accessToken.getExpiresAt(), ZoneId.of("Asia/Seoul")).toString(),
                            "mainPageUrl", "/"
                    ));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
