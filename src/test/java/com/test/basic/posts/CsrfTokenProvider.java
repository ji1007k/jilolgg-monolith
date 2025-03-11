package com.test.basic.posts;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csrf")
public class CsrfTokenProvider {

    @GetMapping
    public ResponseEntity<?> csrf(CsrfToken csrfToken) {
        // CSRF 토큰을 응답 헤더에 포함하여 반환
        return ResponseEntity.ok()
                .header("X-CSRF-TOKEN", csrfToken.getToken())
                .build();
    }
}
