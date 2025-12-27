package com.test.basic.common.config.swagger;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/*
// Swagger에서 사용할 Basic 인증 (기본 사용자/비밀번호 방식)
@SecurityScheme(name = "BasicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
// Swagger 인증용 Bearer 토큰 (JWT 방식)
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
// Swagger API 테스트 시 CSRF 우회용 헤더 (X-From-Swagger)
@SecurityScheme(name = "IgnoreCSRF", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-From-Swagger")
// 실제 CSRF 인증용 헤더 (X-XSRF-TOKEN), CORS 환경에선 서버 검증 필요
// → Swagger UI 내부 JS가 동일 도메인일 경우 자동 전송
@SecurityScheme(name = "CSRF", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-XSRF-TOKEN")
*/
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("JILoL.gg API")
                .version("1.0.0")
                .description("Spring Boot를 이용한 Demo 웹 애플리케이션 API 문서")
                .contact(new Contact()
                        .name("JILoL.gg")
                        .email("ji1007k@gmail.com")
                        .url("https://jilolgg.up.railway.app/jikimi")
//                        .url("https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/jikimi")
                );

        //  Swagger UI에서 보여줄 서버 목록 설정
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                "01_BasicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                        )
                        .addSecuritySchemes(
                                "02_BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer").bearerFormat("JWT")
                        )
                        .addSecuritySchemes(
                                "03_CSRF",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-XSRF-TOKEN")
                        )
                        .addSecuritySchemes(
                                "04_IgnoreCSRF",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-From-Swagger")
                        )
                )
                .info(info)
                .servers(List.of(
                        new Server()
                                .url("https://localhost:8080")
                                .description("Local Server")
                        , new Server()
                                .url("https://jilolgg.up.railway.app/api")
                                .description("Railway Server (prod)")
                        /*, new Server()
                                .url("https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/api")
                                .description("AWS EC2 Server (prod)"),*/

                ));
    }

}
