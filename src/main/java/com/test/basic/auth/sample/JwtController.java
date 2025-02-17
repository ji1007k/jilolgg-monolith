package com.test.basic.auth.sample;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;


@Tag(name = "JWT 토큰 관리 API", description = "JWT 토큰 발급/갱신 API")  // API 그룹 태그
@RestController
@RequestMapping("/token")
public class JwtController {
	private static final Logger logger = LoggerFactory.getLogger(JwtController.class);

	private static JwtEncoder encoder;

	@Value("${jwt.public.key}")
	RSAPublicKey key;

	@Value("${jwt.private.key}")
	RSAPrivateKey priv;

	private static final long ACCESS_TOKEN_EXPIRY = 3600L;
	private static final long REFRESH_TOKEN_EXPIRY = 3600L;

	@Autowired
	public JwtController(JwtEncoder encoder) {
		this.encoder = encoder;
	}

//	@PostMapping(value = { "/generate/sc" })
	public ResponseEntity generateTokenWithSessionCookie(Authentication authentication, HttpServletResponse response) {
		Jwt accessToken = makeAccessToken(authentication);
		ResponseCookie accessTokenCookie = makeResponseCookie("access_token", accessToken.getTokenValue());
		ResponseCookie refreshTokenCookie = makeRefreshToken(authentication);

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

//	@PostMapping(value = {  "/refresh" })
	public ResponseEntity refreshToken(Authentication authentication, HttpServletResponse response) {
		Jwt accessToken = makeAccessToken(authentication);
		ResponseCookie accessTokenCookie = makeResponseCookie("access_token", accessToken.getTokenValue());
		ResponseCookie refreshTokenCookie = makeRefreshToken(authentication);

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

	public Jwt makeAccessToken(Authentication authentication) {
		Instant now = Instant.now();
		// @formatter:off
		String scope = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(" "));

		// 클레임(payload)
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("self")	// JWT를 발급한 주체
				.issuedAt(now)	// JWT가 발급된 시간
				.expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRY))	// JWT의 만료 시간 (현재 시간 + expiry 초)
				.subject(authentication.getName())	// JWT의 주체 (여기서는 인증된 사용자의 이름)
				.claim("scope", scope)	// 사용자가 가진 권한
				.build();
		// @formatter:on

		return this.encoder.encode(JwtEncoderParameters.from(claims));
	}

	public ResponseCookie makeResponseCookie(String key, String token) {
		//  **ResponseCookie**는 서버에서 클라이언트에게 응답을 보낼 때 설정되는 것이고,
		//  클라이언트는 서버로부터 받은 쿠키를 자동으로 저장하고,
		//  그 후의 요청에서 그 쿠키를 자동으로 포함시켜 서버로 보낸다
		ResponseCookie cookie = ResponseCookie.from(key, token)
				.httpOnly(true)  // 클라이언트 JS에서 접근 불가능. XSS 공격이 JWT를 읽을 수 없으므로 보안성이 향상
				.secure(true)    // HTTPS에서만 전송
				.path("/")       // 모든 경로에서 쿠키 사용 가능
				.maxAge(ACCESS_TOKEN_EXPIRY)    // 만료 시간 설정
				.sameSite("None")  // CORS 환경에서 사용 가능하도록 설정 (필요하면 변경 가능)
				.build();

		return cookie;
	}

	public ResponseCookie makeRefreshToken(Authentication authentication) {
		Instant now = Instant.now();

		// 리프레시 토큰 클레임(payload)
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("self")  // JWT를 발급한 주체
				.issuedAt(now)    // JWT가 발급된 시간
				.expiresAt(now.plusSeconds(REFRESH_TOKEN_EXPIRY))  // 리프레시 토큰의 만료 시간 (몇 일 뒤)
				.subject(authentication.getName())   // 주체 (사용자 정보)
				.claim("type", "refresh") // 리프레시 토큰 구분을 위한 "type" 클레임
				.build();

		// 리프레시 토큰 생성 (JWT 인코딩)
		String refreshToken = this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

		return makeResponseCookie("refresh_token", refreshToken);
	}


}