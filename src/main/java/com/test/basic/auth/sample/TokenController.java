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

package com.test.basic.auth.sample;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * A controller for the token resource.
 *
 * @author Josh Cummings
 */

@Tag(name = "JWT 토큰 발급 테스트 API", description = "JWT 토큰 발급 테스트 API")
@RestController
@RequestMapping("/token")
public class TokenController {
	private static JwtEncoder encoder;

	@Autowired
	public TokenController(JwtEncoder encoder) {
		this.encoder = encoder;
	}

	/*@Operation(summary = "JWT 토큰 발급", description = "JWT 토큰을 발급합니다.")
	@GetMapping("/generate")
	public ResponseEntity<?> getToken(@CookieValue(value = "access_token", required = false) String token) {
		if (token == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No token found");
		}
		return ResponseEntity.ok(Collections.singletonMap("token", token));
	}*/

	// spring security 샘플 프로젝트
	@GetMapping("/generate")
	@Operation(summary = "JWT 토큰 발급", description = "JWT 토큰을 발급합니다.")
	@SecurityRequirement(name = "BasicAuth")  // 🔥 Swagger에서 Basic Auth로 인증 가능
	public String generateTokenSample(Authentication authentication) {
		Instant now = Instant.now();

		long expiry = 36000L;
		// @formatter:off
		String scope = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(" "));

		// 클레임(payload)
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("self")	// JWT를 발급한 주체
				.issuedAt(now)	// JWT가 발급된 시간
				.expiresAt(now.plusSeconds(expiry))	// JWT의 만료 시간 (현재 시간 + 10시간)
				.subject(authentication.getName())	// JWT의 주체 (여기서는 인증된 사용자의 이름)
				.claim("scope", scope)	// 사용자가 가진 권한
				.build();
		// @formatter:on

		return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

}