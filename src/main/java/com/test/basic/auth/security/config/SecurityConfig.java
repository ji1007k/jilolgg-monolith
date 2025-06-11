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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.regex.Pattern;

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

	@Value("${cookie.secure}")
	boolean isSecure;
	@Value("${cookie.same-site}")
	String sameSite;

	/**
	 * authorizeRequests().permitAll()은 특정 URL 경로에 대해 인증 없이 접근할 수 있도록 설정
	 * formLogin().permitAll()은 로그인 폼을 모든 사용자가 접근할 수 있도록 설정
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// @formatter:off
		http
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers("/", "/favicon.ico").permitAll()
						// 정적 리소스
						.requestMatchers( "/css/**", "/js/**", "/images/**", "/html/**").permitAll()
						// Swagger
						.requestMatchers("/api/swagger-ui/**", "/api/v3/api-docs/**").permitAll()
						// 로그인, 회원가입
						.requestMatchers("/auth/login", "/auth/signup").permitAll()
						// lol
						.requestMatchers(
								"/lol/teams/sync",
								"/lol/teams/sync-with-csrf",
								"/lol/favorites/**",
                                "/lol/matches/sync",
								"/lol/batch/**"
						).authenticated()
						.requestMatchers(
								"/lol/leagues",
								"/lol/tournaments",
								"/lol/teams/**",
								"/lol/tournaments",
								"/lol/matches", "/lol/matches/**",
								"/lol/standings/**",
								"/lol/matchhistory", "/lol/matchhistory/**"
						).permitAll()
						// post
						.requestMatchers(HttpMethod.GET, "/posts").permitAll()
						.requestMatchers(HttpMethod.POST, "/posts", "/posts/**").authenticated()
						.requestMatchers("/mypage/manager").hasAuthority("SCOPE_ADMIN")  // 특정 권한 필요
						.anyRequest().authenticated()  // 나머지 요청은 인증 필요
				)

				// 토큰 유효성 검증 필터 등록 (커스텀 필터)
				// 커스텀 필터가 usesrnamepassword 인증 필터보다 먼저 동작하도록 등록
				.addFilterBefore(customJwtFilter(jwtTokenProvider()), UsernamePasswordAuthenticationFilter.class)
//				.addFilterBefore(new CustomCsrfFilter(csrfRequireMatcher()), CustomJwtFilter.class) // CSRF 필터

				// CORS 설정
				.cors(Customizer.withDefaults()) // corsConfigurationSource() 자동 연결
				
				// 특정 요청에서 CSRF 해제
				.csrf((csrf) -> csrf
						.ignoringRequestMatchers(
							new AntPathRequestMatcher("/auth/login"),
							new AntPathRequestMatcher("/auth/signup"),
							new AntPathRequestMatcher("/auth/token/refresh"),
							// 경기 전적 조회
							new AntPathRequestMatcher("/lol/matchhistory")
						)
						.csrfTokenRepository(customCsrfTokenRepository())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())	// 토큰 해석기 지정(spring security 6~)
						.requireCsrfProtectionMatcher(csrfRequireMatcher())
				)
				// CSRF(Cross-Site Request Forgery) 공격은 사용자가 의도하지 않은 요청을 다른 사용자나 시스템에 보내는 공격
				// jwt는 stateless 하기 때문에 session 쿠키 등을 통해 인증된 정보로 다른 사이트에 접근하는 공격인 csrf에 대한 방어 불필요
				// 	But!!! 생성된 jwt 토큰을 session 쿠키에 저장해 사용하려면 방어 설정 필요!!
//				.csrf((csrf) -> csrf.disable()) // security는 기본적으로 csrf(보안 공격) 공격에 대한 방어 세팅이 있으며, disable로 해제 가능

				// Basic Authentication 인증 설정
//				.httpBasic(Customizer.withDefaults())	// Basic Authentication 활성화
				// basic 인증 실패 시 뜨는 id/pw 재인증 팝업 방지를 위해 커스텀 엔트리 포인트 설정
				.httpBasic(httpBasic -> httpBasic
						// Security 필터 체인에서 발생하는 인증/인가 관련 예외를 처리
						// Spring MVC 컨트롤러까지 요청이 도달하지 못하고 여기서 처리됨
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
						// 인증되지 않은 사용자가 /auth/login 이외의 API를 요청하면
//						.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\": \"Unauthorized\"}"); // JSON 응답으로 변경
						})
						// 인가되지 않은 사용자가 접근하면
						.accessDeniedHandler(new BearerTokenAccessDeniedHandler())
				);

		// @formatter:on
		return http.build();
	}

	@Bean
	public CustomJwtFilter customJwtFilter(JwtTokenProvider jwtTokenProvider) {
		return new CustomJwtFilter(jwtTokenProvider);
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


	// Spring Security 필터에서 CORS 처리 (필터체인 레벨에서 동작)
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(
				"http://localhost:3000",
				"https://localhost:3000",
				"http://localhost:8080",
				"https://localhost:8080",
				"http://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com",
				"https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com"));  // 도메인 리스트
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN", "X-From-Swagger"));
		config.setExposedHeaders(List.of("X-XSRF-TOKEN"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public CsrfTokenRepository customCsrfTokenRepository() {
		CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repo.setCookieName("XSRF-TOKEN");

		repo.setCookieCustomizer(builder -> builder
				.path("/")
				.httpOnly(false)
				.secure(isSecure)
				.sameSite(sameSite)
//				.maxAge(Duration.ofHours(1))
		);

		return repo;
	}

	@Bean
	public RequestMatcher csrfRequireMatcher() {
		return new RequestMatcher() {
			private static final Pattern ALLOWED_METHODS = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");

			@Override
			public boolean matches(HttpServletRequest request) {
				if (ALLOWED_METHODS.matcher(request.getMethod()).matches()) {
					return false;
				}

				// swagger 전용 헤더가 포함되어있다면 CORS 처리 X
				if (request.getHeader("X-From-Swagger") != null
						&& request.getHeader("X-From-Swagger").equalsIgnoreCase("skip")) {
					return false;	// CSRF 보호를 건너뜀
				}
				return true;	// 나머지 요청은 CSRF 보호 유지
			}
		};
	}



//	TODO 삭제	===================================================================

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

	
	// Spring MVC 레벨 CORS 처리 (필터체인 이후 동작)
	//	-> Spring Security 환경에서는 불충분한 설정 (이미 필터체인에서 cors 거부가 끝날 수 있기 때문)
	// Spring Boot에서 CORS를 사용하고 있다면, 쿠키를 보내기 위해 Access-Control-Allow-Credentials: true를 설정해야 해.
	// React 등 프론트엔드에서 백엔드 API를 호출할 때 CORS 오류를 방지
	/*@Configuration
	public static class CorsConfig implements WebMvcConfigurer {
		public static final String ALLOWED_METHOD_NAMES = "GET,POST,PATCH,PUT,DELETE,OPTIONS";

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/**")	// 모든 경로에 대해 CORS 허용
					// 크로스 도메인 요청에서 쿠키를 허용하려면 특정 도메인(allowedOrigins)을 지정해야 함
					.allowedOrigins(
							"http://localhost:3000",
							"https://localhost:3000",
							"http://localhost:8080",
							"https://localhost:8080",
							"http://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com",
							"https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com"
					) // 허용할 프론트엔드 도메인
					.allowedMethods(ALLOWED_METHOD_NAMES.split(","))	// 허용할 HTTP 메서드
					.allowedHeaders("Content-Type", "Authorization", "X-XSRF-TOKEN", "X-From-Swagger") 	// 클라이언트가 보낼 수 있는 헤더
					.exposedHeaders("X-XSRF-TOKEN")	// 클라이언트가 응답에서 읽을 수 있는 헤더
					.allowCredentials(true);	// JWT, CSRF 등 인증 정보(쿠키) 포함 허용 (credentials: 'include'가 포함된 요청 허용)
		}
	}*/
}