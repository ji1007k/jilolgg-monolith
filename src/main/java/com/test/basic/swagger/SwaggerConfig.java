package com.test.basic.swagger;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
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
        name = "BearerAuth",    // Swagger 인증용: Bearer Token (JWT)
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SecurityScheme(
        name = "IgnoreCSRF",    // Swagger API 테스트 시 CSRF 인증 우회용
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-From-Swagger"
)
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("JIKIM.GG API")
                .version("1.0.0")
                .description("Spring Boot를 이용한 Demo 웹 애플리케이션 API 문서입니다.")
                .contact(new Contact()
                        .name("JIKIM.GG")
                        .email("ji1007k@gmail.com")
                        .url("https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/jikimi")
                );

        //  Swagger UI에서 보여줄 서버 목록 설정
        return new OpenAPI()
                .components(new Components())
                .info(info)
                .servers(List.of(
                        new Server()
                                .url("https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/api")
                                .description("Production Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Server")
                ));
    }

}
