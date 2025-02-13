/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.test.basic.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.test.basic.auth.jwt.JwtCookieFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Security configuration for the main application.
 *
 * @author Josh Cummings
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Value("${jwt.public.key}")
	RSAPublicKey key;

	@Value("${jwt.private.key}")
	RSAPrivateKey priv;

	/**
	 * authorizeRequests().permitAll()은 특정 URL 경로에 대해 인증 없이 접근할 수 있도록 설정합니다.
	 * formLogin().permitAll()은 로그인 폼을 모든 사용자가 접근할 수 있도록 설정합니다.
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// @formatter:off
		// 토큰 유효성 검증 필터 등록 (커스텀 필터)
//		http.addFilterBefore(new JwtAuthenticationFilter(jwtDecoder()), UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(new JwtCookieFilter(jwtDecoder()), UsernamePasswordAuthenticationFilter.class);

		http
				// CSRF(Cross-Site Request Forgery) 공격은 사용자가 의도하지 않은 요청을 다른 사용자나 시스템에 보내는 공격 방어 비활성화
				// jwt는 stateless 하기 때문에 session 쿠키 등을 통해 저장된 정보로 다른 사이트에 접근하는 공격인 csrf에 대한 방어 불필요
				// But!!! 생성된 jwt 토큰을 session 쿠키에 저장해 사용하려면 방어 설정 필요!!
//				.csrf((csrf) -> csrf.disable()) // security는 기본적으로 csrf(보안 공격) 공격에 대한 방어 세팅이 있다. (disable로 해제도 가능)
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(
								"/",
								"/favicon.ico",
								"/css/**", "/js/**",	// static 파일
								"/user/login", "/user/logout",
//								"/error/**",
								"/swagger",      		// Swagger 관련 url
								"/v3/api-docs/**",     // OpenAPI 3.0 문서
								"/swagger-ui/**",      // Swagger UI
								"/swagger-ui.html",    // Swagger UI 메인 페이지
								"/webjars/**"          // Swagger 관련 리소스
						).permitAll()	// 인증 해제
						.anyRequest().authenticated()	// 그 외 요청은 인증 필요
				)
				// 특정 요청에서 CSRF 해제
				.csrf((csrf) -> csrf.ignoringRequestMatchers("/token/**", "/token/generate/*"))
				// Basic Authentication 인증 설정
//				.httpBasic(Customizer.withDefaults())	// Basic Authentication 활성화
				.httpBasic(httpBasic -> httpBasic
						.authenticationEntryPoint((request, response, authException) -> {
							// 처음 토큰 발급 요청인 경우만 처리
							if (request.getRequestURI().equals("/token/generate/sc")) {
								// 커스텀 인증 엔트리 포인트. (팝업 방지)
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType("application/json");
								response.getWriter().write("{\"error\": \"Unauthorized\"}");
							}
						})
				)
				// 250207. jwt() in OAuth2ResourceServerConfigurer has been deprecated and marked for removal
				// JWT 기반 OAuth2 인증 설정
//				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)	// 시큐리티 6 이전 버전
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))	// 시큐리티 6 이상 버전
				// 세션 사용 X (REST API 용)
				.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling((exceptions) -> exceptions
//						.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
//						.accessDeniedHandler(new BearerTokenAccessDeniedHandler())
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\": \"Unauthorized\"}"); // JSON 응답으로 변경
						})
						.accessDeniedHandler(new BearerTokenAccessDeniedHandler())
				);


		/*http
				// 세션 기반 로그인/로그아웃 시 아래 설정 사용
				// 로그인 페이지
				.formLogin(formLogin ->
						formLogin
								.loginPage("/user/login")  // 커스터마이징된 로그인 페이지 경로
								.defaultSuccessUrl("/home", true) // 로그인 성공 후 이동할 페이지
								.failureForwardUrl("/user/login")
								.permitAll()  // 로그인 페이지는 누구나 접근 가능하도록 설정
				)
				// 로그아웃 권한
				.logout(logout ->
						logout
								.permitAll()  // 로그아웃은 누구나 가능
				)*/

		// @formatter:on
		return http.build();
	}

	@Bean
	UserDetailsService users() {
		// @formatter:off
		return new InMemoryUserDetailsManager(
			User.withUsername("admin")
				.password("{noop}admin")
				.authorities("ADMIN")
				.build(),
			User.withUsername("user")
				.password("{noop}password")
				.authorities("USER")	// SCOPE_ 접두사 자동 추가
				.build()
		);
		// @formatter:on
	}

	// JWT 토큰 검증 (서명 확인)
	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(this.key).build();
	}

	// 토큰 요청을 보내면, 서버가 RSA 개인키로 서명한 JWT를 클라이언트에게 반환
	@Bean
	JwtEncoder jwtEncoder() {
		JWK jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

    //. Spring Boot에서 CORS를 사용하고 있다면, 쿠키를 보내기 위해 Access-Control-Allow-Credentials: true를 설정해야 해.
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins("http://localhost:3000")  // 클라이언트 주소
						.allowedMethods("GET", "POST", "PUT", "DELETE")
						.allowCredentials(true);  // 인증 정보(쿠키) 포함 허용
			}
		};
	}

}