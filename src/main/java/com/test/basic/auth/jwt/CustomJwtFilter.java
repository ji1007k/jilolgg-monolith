package com.test.basic.auth.jwt;

import com.test.basic.auth.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

@Component
public class CustomJwtFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CustomJwtFilter.class);

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public CustomJwtFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;

        this.authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.authoritiesConverter.setAuthorityPrefix(""); // "SCOPE_" 자동 추가 방지
        this.authoritiesConverter.setAuthoritiesClaimName("authorities"); // authorities 클레임 사용
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isStaticResource(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // no-cache → 캐시 검증 후 사용 (완전한 방지는 아님)
        // no-store → 브라우저에 저장 x
        response.setHeader("Cache-Control", "no-cache");

        try {
            if (path.endsWith("/auth/token/refresh")) {
                String refreshToken = jwtTokenProvider.getJwtStrFromCookie(request.getCookies(), "refresh_token");

                if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw new JwtException("Invalid Refresh token");
                }

                // refresh 토큰에서 userId 추출 → DB에서 사용자 정보 조회
                Jwt refreshJwt = jwtTokenProvider.getJwtFromStr(refreshToken);
                UserDetails userDetails = customUserDetailsService.loadUserByUserId(refreshJwt.getSubject());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } else {
                // 일반 요청은 access token 검증
                String accessToken = jwtTokenProvider.getJwtStrFromCookie(request.getCookies(), "access_token");
                if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
                    throw new JwtException("Invalid Access token");
                }

                // JWT의 권한(Role) 정보는 따로 추출해서 확인
                Jwt accessJwt = jwtTokenProvider.getJwtFromStr(accessToken);
                Collection<GrantedAuthority> authorities = extractAuthorities(accessJwt);
                AbstractAuthenticationToken authentication = new JwtAuthenticationToken(accessJwt, authorities);
                authentication.setAuthenticated(true);

                // SecurityContext에 인증 정보 저장 (인증 중복 방지)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException e) {
            SecurityContextHolder.clearContext(); // 인증 실패 시 context 초기화
        }

        filterChain.doFilter(request, response);
    }

    private boolean isStaticResource(String path) {
        // 프론트엔드 정적 파일은 인증 필터를 태울 필요 없음 (속도 및 불필요한 로그 방지)
        return path.startsWith("/jikimi/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg") ||
                path.endsWith(".png");
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // JwtGrantedAuthoritiesConverter를 사용하여 권한 변환
        // JwtGrantedAuthoritiesConverter: 기본적으로 OAuth2의 scope 클레임을
        // 권한(GrantedAuthority)으로 변환
        // -> 이때 모든 권한 앞에 SCOPE_가 자동으로 붙는다.
        /*
         * List<GrantedAuthority> authorities = authoritiesConverter.convert(jwt)
         * .stream()
         * .collect(Collectors.toList());
         */

        List<GrantedAuthority> authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
        logger.info("Converted Authorities: {}", authorities);

        return authorities;
    }

}
