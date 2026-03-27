package com.test.basic.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 프론트엔드에서 보낸 /api 요청을 처리하는 필터
 * 프론트가 모든 요청에 /api 를 붙여서 보내므로, 서버 내부 라우팅을 위해 이를 제거함
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrefixFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (path.startsWith("/api/")) {
            String newPath = path.substring(4); // "/api" 부분만 제거 (예: /api/lol/leagues -> /lol/leagues)

            // Swagger 관련 경로는 그대로 둠 (SecurityConfig에서 처리)
            if (path.startsWith("/api/swagger-ui") || path.startsWith("/api/v3/api-docs")) {
                chain.doFilter(request, response);
                return;
            }

            httpRequest.getRequestDispatcher(newPath).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
