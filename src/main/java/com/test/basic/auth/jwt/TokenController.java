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

package com.test.basic.auth.jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller for the token resource.
 *
 * @author Josh Cummings
 */
@RestController
@RequestMapping("/token")
public class TokenController {
	private static JwtEncoder encoder;

	@Autowired
	public TokenController(JwtEncoder encoder) {
		this.encoder = encoder;
	}

	@PostMapping("/token")
	public String generateToken(Authentication authentication, HttpServletResponse response) {
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