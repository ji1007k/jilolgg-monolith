package com.test.basic.auth.csrf;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test") // test 환경에서만 활성화됨
@RequestMapping("/csrf")
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
