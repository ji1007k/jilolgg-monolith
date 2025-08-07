package com.test.basic.common.config;

import com.test.basic.chat.ChatWebSocketHandler;
import com.test.basic.common.handler.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 연결 설정 - 인프라/설정 계층
 * - 엔드포인트 등록
 * - CORS 설정
 * - 인터셉터 연결(JWT 인증)
 * - 웹소켓 핸들러 등록
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor, ChatWebSocketHandler chatWebSocketHandler) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 웹소켓 엔드포인트 설정
        registry.addHandler(chatWebSocketHandler, "/chat")  // 웹소켓 핸들러 등록
                .addInterceptors(jwtHandshakeInterceptor)   // 인터셉터 등록 (JWT 인증)
                .setAllowedOrigins(                         // 특정 도메인 허용
                        "https://localhost:3000",
                        "https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com"
                )
//                .setAllowedOrigins("*")                     // CORS 설정. 전체 도메인 허용
        ;
    }

}
