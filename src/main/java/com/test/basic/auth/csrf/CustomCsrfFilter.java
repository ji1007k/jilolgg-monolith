package com.test.basic.auth.csrf;

import com.test.basic.auth.security.config.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import java.io.IOException;

public class CustomCsrfFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";  // CSRF 토큰 헤더 이름
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";    // CSRF 토큰 쿠키 이름

    private final RequestMatcher csrfRequireMatcher;  // CSRF 검사 여부를 결정하는 RequestMatcher

    public CustomCsrfFilter(RequestMatcher csrfRequireMatcher) {
        this.csrfRequireMatcher = csrfRequireMatcher; // CsrfRequireMatcher를 사용
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // CSRF 검사 여부를 결정하는 matcher 호출
        if (csrfRequireMatcher.matches(request)) {
            // CSRF 토큰을 헤더에서 가져옴
            String csrfTokenFromHeader = request.getHeader(CSRF_HEADER_NAME);

            // CSRF 토큰을 쿠키에서 가져옴
            String csrfTokenFromCookie = getCookieValue(request, CSRF_COOKIE_NAME);

            // CSRF 토큰 검증
            if (csrfTokenFromHeader == null || !csrfTokenFromHeader.equals(csrfTokenFromCookie)) {
                // CSRF 토큰이 없거나 일치하지 않으면 403 Forbidden 반환
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        }

        // CSRF 검증이 통과한 경우 요청을 계속해서 처리
        filterChain.doFilter(request, response);
    }

    // 쿠키에서 값 추출하는 헬퍼 메서드
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }
}
