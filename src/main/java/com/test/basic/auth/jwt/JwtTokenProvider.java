package com.test.basic.auth.jwt;

import com.test.basic.auth.security.user.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static JwtEncoder encoder;
    private static JwtDecoder decoder;

    private static final long ACCESS_TOKEN_EXPIRY = 3600L;
    private static final long REFRESH_TOKEN_EXPIRY = 3600L;


    public JwtTokenProvider(JwtEncoder encoder, JwtDecoder decoder) throws Exception {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    // JWT 토큰 생성
    public Jwt createToken(Authentication authentication) {
        Jwt accessToken = makeAccessToken(authentication);
        return accessToken;
    }

    public Jwt makeAccessToken(Authentication authentication) {

        Instant now = Instant.now();

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        String userId, email, username;
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            userId = userDetails.getId().toString();
            email = userDetails.getEmail();
            username = userDetails.getUsername();
        } else if (principal instanceof Jwt jwt) {
            userId = jwt.getSubject();
            email = jwt.getClaimAsString("email");
            username = jwt.getClaimAsString("username");
        } else {
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRY))
                .subject(userId)
                .claim("email", email)
                .claim("username", username)
                .claim("scope", scope) // FIXME: 필요한 경우 제외
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims));
    }

    public ResponseCookie makeRefreshToken(Authentication authentication) {

        Instant now = Instant.now();

        // 리프레시 토큰 클레임(payload)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")  // JWT를 발급한 주체
                .issuedAt(now)    // JWT가 발급된 시간
                .expiresAt(now.plusSeconds(REFRESH_TOKEN_EXPIRY))  // 리프레시 토큰의 만료 시간 (몇 일 뒤)
                .subject(authentication.getName())   // 주체 (사용자 정보). 여기서는 jwt.getClaim("sub") 값 == userId
                .claim("type", "refresh") // 리프레시 토큰 구분을 위한 "type" 클레임
                .build();

        // 리프레시 토큰 생성 (JWT 인코딩)
        String refreshToken = this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return makeResponseCookie("refresh_token", refreshToken);
    }

    public ResponseCookie makeResponseCookie(String key, String token) {
        //  **ResponseCookie**는 서버에서 클라이언트에게 응답을 보낼 때 설정되는 것이고,
        //  클라이언트는 서버로부터 받은 쿠키를 자동으로 저장하고,
        //  그 후의 요청에서 그 쿠키를 자동으로 포함시켜 서버로 보낸다
        ResponseCookie cookie = ResponseCookie.from(key, token)
                .httpOnly(true)  // 클라이언트 JS에서 접근 불가능. XSS 공격이 JWT를 읽을 수 없으므로 보안성이 향상
                .secure(true)    // HTTPS에서만 전송. false: HTTP 허용
                .path("/")       // 모든 경로에서 쿠키 사용 가능
                .maxAge(ACCESS_TOKEN_EXPIRY)    // 만료 시간 설정
                .sameSite("Lax")    // GET 메소드 요청에 한해 CORS 허용
//                .sameSite("None")  // CORS 환경에서 사용 가능하도록 설정 (필요하면 변경 가능). 크로스 사이트 요청에서 쿠키 전송
                .build();

        return cookie;
    }

    public Jwt getJwtFromStr(String token) {
        return this.decoder.decode(token);  // 서명 검증
    }

    public String getJwtStrFromCookie(Cookie[] cookies, String key) {
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> key.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            // 방법1) Spring Security의 Nimbus JOSE + JWT 라이브러리를 사용
            // RS256(RSA)와 같은 비대칭 서명 방식에서 **공개키(public key)**를 사용해 JWT 토큰을 검증
            this.decoder.decode(token);  // 서명 검증
            // 방법2) HMAC (대칭키) 방식의 서명 검증
            // 서명과 검증에 동일한 비밀 키를 사용
//            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // FIXME throw
            return false;  // 예외 발생 시 토큰이 유효하지 않음
        }
    }
}
