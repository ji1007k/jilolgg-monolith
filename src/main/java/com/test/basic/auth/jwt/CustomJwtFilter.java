package com.test.basic.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// OncePerRequestFilter를 상속 시 요청 하나당 한 번만 필터 실행
@Component
public class CustomJwtFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CustomJwtFilter.class);

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public CustomJwtFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;

        this.authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.authoritiesConverter.setAuthorityPrefix("");    // "SCOPE_" 자동 추가 방지
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.info("Request path: {}", path);

        // 정적 리소스가 요청된 경우, 인증 건너뜀
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
//        String method = request.getMethod(); // 요청 메서드 가져오기
//        if (path.startsWith("/auth/login") && "GET".equalsIgnoreCase(method)) {
        List<String> whitelist = List.of("/auth/login", "/auth/signup", "/token/generate");

        if (whitelist.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // no-cache → 캐시 검증 후 사용 (완전한 방지는 아님)
        // no-store → 브라우저에 저장 ❌
        response.setHeader("Cache-Control", "no-cache");

        // 헤더에서 JWT 토큰을 추출
        String accessToken = jwtTokenProvider.getJwtStrFromCookie(request.getCookies(), "access_token");

        if (accessToken != null) {
            try {
                if (!jwtTokenProvider.validateToken(accessToken)) {
                    throw new JwtException("Invalid token");
                }

                // 토큰 재발급 요청이면 refresh 토큰 유효성 검증
                if (path.equals("/auth/token/refresh")) {
                    String refreshToken = jwtTokenProvider.getJwtStrFromCookie(request.getCookies(), "refresh_token");

                    if (refreshToken != null) {
                        if (!jwtTokenProvider.validateToken(refreshToken)) {
                            throw new JwtException("Invalid token");
                        }
                    }
                }

                // JWT의 권한(Role) 정보는 따로 추출해서 확인
                Jwt token = jwtTokenProvider.getJwtFromStr(accessToken);
                Collection<GrantedAuthority> authorities = extractAuthorities(token);

                AbstractAuthenticationToken authentication = new JwtAuthenticationToken(token, authorities);
                authentication.setAuthenticated(true);

                // SecurityContext에 인증 정보 저장 (인증 중복 방지)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                SecurityContextHolder.clearContext(); // 인증 실패 시 context 초기화
            }
        }

        filterChain.doFilter(request, response);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // JwtGrantedAuthoritiesConverter를 사용하여 권한 변환
        // JwtGrantedAuthoritiesConverter: 기본적으로 OAuth2의 scope 클레임을 권한(GrantedAuthority)으로 변환
        //  -> 이때 모든 권한 앞에 SCOPE_가 자동으로 붙는다.
        /*List<GrantedAuthority> authorities = authoritiesConverter.convert(jwt)
                .stream()
                .collect(Collectors.toList());*/

        List<GrantedAuthority> authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
        logger.info("Converted Authorities: {}", authorities);

        return authorities;
    }

}
