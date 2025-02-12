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

import com.test.basic.auth.jwt.JwtUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

	/*@GetMapping(value = { "/", "/home" })
	public String hello() {
		return "index";
	}*/

	@GetMapping(value = { "/", "/home" })
	public String mainPage(Model model, @AuthenticationPrincipal Jwt jwt) {
		if (jwt != null) {
			model.addAttribute("username", jwt.getClaim("sub"));
			// JWT 만료 시간 추출 및 전달 (한국 시간)
			Instant expirationTime = jwt.getExpiresAt();
			LocalDateTime expirationTimeKST = LocalDateTime.ofInstant(expirationTime, ZoneId.of("Asia/Seoul"));
			model.addAttribute("expirationTime", expirationTimeKST.toString());
		}
		return "index"; // Thymeleaf 템플릿 반환
	}

	@GetMapping(value = { "/user/login" })
	public String loginPage() {
		return "login";
	}



//	@PreAuthorize("hasAuthority('ADMIN') and #user.username == authentication.name")
	@PreAuthorize("hasAuthority('ADMIN')")
	@GetMapping("/mypage/admin")
//	public ResponseEntity<String> getAdminPage(@PathVariable String username, @RequestBody User user) {
	public ResponseEntity adminPage(Authentication authentication) {
		return ResponseEntity.ok("Hello, " + authentication.getAuthorities() + ": " + authentication.getName() + "!");
	}

	@PreAuthorize("hasAuthority('USER')")
	@GetMapping("/mypage")
	public ResponseEntity hello(Authentication authentication) {
		return ResponseEntity.ok("Hello, " + authentication.getName() + "!");
	}


	@GetMapping("/user/logout")
	public String logout(HttpServletResponse response) {
		// 세션 쿠키를 만료시켜서 삭제
		Cookie cookie = new Cookie("access_token", null);
		cookie.setHttpOnly(true);  // 자바스크립트에서 쿠키를 접근할 수 없도록 설정
		cookie.setPath("/");  // 쿠키의 경로를 현재 웹사이트의 루트로 설정
		cookie.setMaxAge(0);  // 쿠키 만료 시간 설정 (0으로 설정하면 즉시 만료됨)
		cookie.setSecure(true);  // HTTPS 연결에서만 쿠키가 전달됨
		response.addCookie(cookie);  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

		Cookie refreshTokenCookie = new Cookie("refresh_token", null);
		refreshTokenCookie.setHttpOnly(true);  // 자바스크립트에서 쿠키를 접근할 수 없도록 설정
		refreshTokenCookie.setPath("/");  // 쿠키의 경로를 현재 웹사이트의 루트로 설정
		refreshTokenCookie.setMaxAge(0);  // 쿠키 만료 시간 설정 (0으로 설정하면 즉시 만료됨)
		refreshTokenCookie.setSecure(true);  // refreshToken 쿠키에 대해서도 동일
		response.addCookie(refreshTokenCookie);  // 쿠키를 응답에 추가하여 클라이언트에서 삭제되도록 함

		// 추가적으로 헤더에서 JWT 토큰 삭제 (필터에서 처리되는 부분)
		response.setHeader("Authorization", ""); // Authorization 헤더를 비워서 토큰 삭제

		return "redirect:/";
	}

}