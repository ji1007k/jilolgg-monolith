package com.test.basic.auth.csrf;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csrf")
@Tag(name = "[TEST] CSRF API", description = "CSRF 토큰 발급 API")
public class CsrfTokenController {

    // RestTemplate이나 테스트 환경에선 CSRF 자동 관리가 안돼서 테스트용으로 만든 API
    @GetMapping
    public ResponseEntity<?> csrf(CsrfToken csrfToken) {
        // CSRF 토큰을 응답 헤더에 포함하여 반환
        return ResponseEntity.ok()
                .header("X-CSRF-TOKEN", csrfToken.getToken())
                .build();
    }
}
