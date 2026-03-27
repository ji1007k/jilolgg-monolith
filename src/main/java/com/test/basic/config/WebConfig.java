package com.test.basic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /jikimi/ 로 시작하는 모든 요청 및 /jikimi.txt 등 특수 경로 처리
        registry.addResourceHandler("/jikimi/**", "/jikimi*")
                .addResourceLocations("classpath:/static/jikimi/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {

                        // 1. 기본 인덱스 처리
                        if (resourcePath == null || resourcePath.isEmpty() || resourcePath.equals("/")
                                || resourcePath.equals("jikimi")) {
                            return location.createRelative("index.html");
                        }

                        // 2. Next.js App Router 특수 패턴 처리 (basePath:/jikimi 인 경우 /jikimi.txt 요청이 올 수 있음)
                        if (resourcePath.equals("jikimi.txt")) {
                            return location.createRelative("index.txt");
                        }

                        // 3. 실제 존재하는 리소스 확인
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        // 4. SPA 라우팅 지원
                        // 확장자가 있는 파일 요청(예: .js, .css, .txt, .json)이 실패한 경우 index.html을 주면 Hydration 에러
                        // 발생
                        // 확장자가 없는 경로(라우팅 경로)인 경우만 index.html 반환
                        if (resourcePath.contains(".")) {
                            return null;
                        }

                        return location.createRelative("index.html");
                    }
                });
    }
}
