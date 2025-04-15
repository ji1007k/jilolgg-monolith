package com.test.basic.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
        Info info = new Info()
                .title("Demo API")
//                .version(appVersion)
                .description("Spring Boot를 이용한 Demo 웹 애플리케이션 API입니다.")
//                .termsOfService("http://swagger.io/terms/")
                .contact(new Contact()
                        .name("jikim")
//                        .email("ji1007k@gmail.com")
                        .url("https://ec2-3-39-239-163.ap-northeast-2.compute.amazonaws.com")
                );
//                .license(new License()
//                        .name("Apache License Version 2.0")
//                        .url("http://www.apache.org/licenses/LICENSE-2.0")
//                );

        //  Swagger UI에서 보여줄 서버 목록 설정
        return new OpenAPI()
                .components(new Components())
                .info(info)
                .servers(List.of(
                        new Server().url("https://ec2-3-39-239-163.ap-northeast-2.compute.amazonaws.com/api").description("Production Server"),
                        new Server().url("http://localhost:8080").description("Local Server")
                ));
    }

}
