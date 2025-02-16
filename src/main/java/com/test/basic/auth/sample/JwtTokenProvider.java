//package com.test.basic.auth.sample;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class JwtTokenProvider {
//
//    private final Key key;
//
//    // 토큰 유효시간 (예: 1시간)
//    private static final long EXPIRATION_TIME = 1000 * 60 * 60;
//
//    // 🔹 application.yml 에서 secret 키 불러오기
//    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
//        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
//    }
//
//    // 🔹 1. JWT 생성
//    public String createToken(String username, List<String> roles) {
//        Claims claims = Jwts.claims().setSubject(username);
//        claims.put("roles", roles);
//
//        Date now = new Date();
//        Date validity = new Date(now.getTime() + EXPIRATION_TIME); // 현재 시간 + 유효기간
//
//        return Jwts.builder()
//                .setClaims(claims)
//                .setIssuedAt(now)
//                .setExpiration(validity)
//                .signWith(key, SignatureAlgorithm.HS256) // 서명
//                .compact();
//    }
//
//    // 🔹 2. JWT에서 사용자 정보(username) 추출
//    public String getUsernameFromToken(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//
//    // 🔹 3. JWT에서 역할(roles) 추출
//    public List<SimpleGrantedAuthority> getRolesFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        List<String> roles = (List<String>) claims.get("roles");
//        return roles.stream()
//                .map(SimpleGrantedAuthority::new)
//                .collect(Collectors.toList());
//    }
//
//    // 🔹 4. JWT 유효성 검사
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {
//            return false; // 유효하지 않은 토큰
//        }
//    }
//
//    // 🔹 5. HTTP 요청에서 JWT 추출 (Authorization 헤더에서 가져오기)
//    public String resolveToken(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7); // "Bearer " 제거 후 토큰 반환
//        }
//        return null;
//    }
//}
