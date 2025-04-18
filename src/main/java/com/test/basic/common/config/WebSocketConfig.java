package com.test.basic.common.config;

import com.test.basic.chat.ChatWebSocketHandler;
import com.test.basic.common.handler.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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
        registry.addHandler(chatWebSocketHandler, "/chat")
                .addInterceptors(jwtHandshakeInterceptor)  // 인터셉터 등록
//                .setAllowedOrigins("*")    // CORS 설정. 전체 도메인 허용
                .setAllowedOrigins(
                        "https://localhost:3000",
                        "https://ec2-43-200-4-84.ap-northeast-2.compute.amazonaws.com"
                )    // CORS 설정
        ;
    }

}
