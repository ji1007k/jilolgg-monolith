package com.test.basic.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JwtCookieFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtCookieFilter.class);

    private final JwtDecoder jwtDecoder;
    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public JwtCookieFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;

        this.authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.authoritiesConverter.setAuthorityPrefix("");    // "SCOPE_" 자동 추가 방지
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 필터 적용 제외
        String path = request.getRequestURI();
        logger.info("Request path: {}", path);

        if (path.startsWith("/user/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = getJwtFromCookie(request, "access_token");

        if (accessToken != null) {
            try {
                Jwt jwt = jwtDecoder.decode(accessToken); // JWT의 서명과 기본 유효성만 확인

                // 로그아웃 요청이 들어오면 처리
                if (path.equals("/user/logout")) {
                    // SecurityContext에서 인증 정보를 삭제
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 토큰 재발급 요청이면 refresh 토큰 유효성 검증
                if (path.equals("/token/refresh")) {
                    String refreshToken = getJwtFromCookie(request, "refresh_token");

                    if (refreshToken != null) {
                        jwtDecoder.decode(refreshToken);
                    }
                }
                // JWT의 권한(Role) 정보는 따로 추출해서 확인
                Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

                // SecurityContext에 저장
                AbstractAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
                authentication.setAuthenticated(true);
                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                SecurityContextHolder.clearContext(); // 인증 실패 시 context 초기화
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromCookie(HttpServletRequest request, String key) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> key.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
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
