package com.test.basic.auth.jwt;

import com.test.basic.auth.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static JwtEncoder encoder;
    private static JwtDecoder decoder;

    private static final long ACCESS_TOKEN_EXPIRY = 3600L;
    private static final long REFRESH_TOKEN_EXPIRY = 3600L;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

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
        // @formatter:off
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        // FIXME CLAIM에 SCOPE 정보 제외시키기
        //  최소한의 정보만 JWT에 포함시켜야 함. (EX. username)
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

    public Jwt getTokenFromStr(String token) {
        return this.decoder.decode(token);  // 서명 검증
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

    // jwt 토큰 기반 사용자 검증
    public Authentication getAuthentication(String token) {
        Jwt jwt = this.decoder.decode(token);

        String username = jwt.getSubject();
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    }

}
