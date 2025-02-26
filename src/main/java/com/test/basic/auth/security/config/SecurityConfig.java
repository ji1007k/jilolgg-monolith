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

package com.test.basic.auth.security.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.test.basic.auth.jwt.CustomJwtFilter;
import com.test.basic.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
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
		http
				// CSRF(Cross-Site Request Forgery) 공격은 사용자가 의도하지 않은 요청을 다른 사용자나 시스템에 보내는 공격 방어 비활성화
				// jwt는 stateless 하기 때문에 session 쿠키 등을 통해 저장된 정보로 다른 사이트에 접근하는 공격인 csrf에 대한 방어 불필요
				// But!!! 생성된 jwt 토큰을 session 쿠키에 저장해 사용하려면 방어 설정 필요!!
//				.csrf((csrf) -> csrf.disable()) // security는 기본적으로 csrf(보안 공격) 공격에 대한 방어 세팅이 있다. (disable로 해제도 가능)
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers("/", "/favicon.ico").permitAll()
						.requestMatchers( "/css/**", "/js/**", "/images/**").permitAll()	// 정적 리소스 허용
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 허용
						.requestMatchers("/auth/login", "/auth/signup").permitAll()  // 로그인, 회원가입 허용
						.requestMatchers("/mypage/manager").hasAuthority("SCOPE_ADMIN")  // 특정 권한 필요
						.anyRequest().authenticated()  // 나머지 요청은 인증 필요
				)
				// 토큰 유효성 검증 필터 등록 (커스텀 필터)
				// 커스텀 필터가 usesrnamepassword 인증 필터보다 먼저 동작하도록 등록
				.addFilterBefore(new CustomJwtFilter(jwtTokenProvider()), UsernamePasswordAuthenticationFilter.class)

				// 특정 요청에서 CSRF 해제
//				.csrf((csrf) -> csrf.ignoringRequestMatchers("/token/**", "/token/generate/*"))
				.csrf((csrf) -> csrf.ignoringRequestMatchers(
						"/auth/login", "/auth/signup", "/auth/token/refresh"
				))
				// Basic Authentication 인증 설정
//				.httpBasic(Customizer.withDefaults())	// Basic Authentication 활성화
				// basic 인증 실패 시 뜨는 id/pw 재인증 팝업 방지를 위해 커스텀 엔트리 포인트 설정
				.httpBasic(httpBasic -> httpBasic
						.authenticationEntryPoint((request, response, authException) -> {
							// 처음 토큰 발급 요청인 경우만 처리
							if (request.getRequestURI().equals("/auth/login")) {
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
								.loginPage("/auth/login")  // 커스터마이징된 로그인 페이지 경로
								.defaultSuccessUrl("/home", true) // 로그인 성공 후 이동할 페이지
								.failureForwardUrl("/auth/login")
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

	// 애플리케이션 종료 시 사라짐. 테스트용 사용자 정보
	/*@Bean
	UserDetailsService users() {
		// @formatter:off
		UserDetails admin = User.withUsername("admin")
				.password("{noop}admin") //{noop} 암호화 없이 저장
				.authorities("ADMIN")
				.build();

		UserDetails user = User.builder()
				.username("user")
				.password("{noop}password")
				.authorities("USER")
				.build();	// SCOPE_ 접두사 자동 추가

		// @formatter:on
		return new InMemoryUserDetailsManager(admin, user);
	}*/


    //. Spring Boot에서 CORS를 사용하고 있다면, 쿠키를 보내기 위해 Access-Control-Allow-Credentials: true를 설정해야 해.
	// React 등 프론트엔드에서 백엔드 API를 호출할 때 CORS 오류를 방지
	@Configuration
	public class CorsConfig implements WebMvcConfigurer {

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
					// React 개발 서버(localhost:3000)에서 Spring Boot(localhost:8080)로 API 요청 가능
					.allowedOrigins("http://localhost:3000")  // 허용할 클라이언트 주소 (React 등)
					.allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 HTTP 메서드
					// withCredentials: true가 포함된 요청도 허용 (JWT 같은 인증 쿠키 허용).
					.allowCredentials(true);  // 인증 정보(쿠키) 포함 허용
		}
	}


	@Bean
	public JwtTokenProvider jwtTokenProvider() throws Exception {
		return new JwtTokenProvider(jwtEncoder(), jwtDecoder());
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


	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	// BCryptPasswordEncoder를 PasswordEncoder로 등록
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // 비밀번호 암호화
	}

}