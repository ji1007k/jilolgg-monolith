package com.test.basic;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	// 루트나 홈으로 오면 통합된 프론트엔드 경로로 리다이렉트
	@GetMapping(value = { "/", "/home" })
	public String mainPage() {
		return "redirect:/jikimi/";
	}

	// /jikimi 경로로 오면 슬래시를 붙여서 리다이렉트 (Next.js basePath 대응)
	@GetMapping("/jikimi")
	public String jikimiRoot() {
		return "redirect:/jikimi/";
	}

	// /jikimi/ 로 오면 index.html 로 포워딩 (URL은 유지)
	@GetMapping("/jikimi/")
	public String index() {
		return "forward:/jikimi/index.html";
	}

}