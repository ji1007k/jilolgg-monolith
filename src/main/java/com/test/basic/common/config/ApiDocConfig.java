package com.test.basic.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "BasicAuth",          // Swagger에서 사용할 인증 이름
        type = SecuritySchemeType.HTTP,
        scheme = "basic"             // Basic Auth 방식 사용
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class ApiDocConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        // API 요청을 보낼 때 HTTPS를 자동으로 사용
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("https://ec2-3-36-70-95.ap-northeast-2.compute.amazonaws.com/api").description("Production Server"),
                        new Server().url("http://localhost:8080").description("Local Server")
                ));
    }

}
