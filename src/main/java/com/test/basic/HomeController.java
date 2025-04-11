/*
 * Copyright 2002-2020 the original author or authors.
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

package com.test.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A controller for the hello resource.
 *
 * @author Josh Cummings
 */
@Controller
public class HomeController {
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	@GetMapping(value = { "/", "/home" })
	public String mainPage(Model model, Authentication authentication, @AuthenticationPrincipal Jwt jwt) {
		if (authentication != null) {
			logger.info(authentication.getName());	// == jwt.getClaim("sub") == userId
		}

		if (jwt != null) {
			model.addAttribute("userId", jwt.getSubject());
			model.addAttribute("username", jwt.getClaimAsString("username"));
			// JWT 만료 시간 추출 및 전달 (한국 시간)
			Instant expirationTime = jwt.getExpiresAt();
			LocalDateTime expirationTimeKST = LocalDateTime.ofInstant(expirationTime, ZoneId.of("Asia/Seoul"));
			model.addAttribute("expirationTime", expirationTimeKST.toString());
		}
		return "index"; // Thymeleaf 템플릿 반환
	}

}