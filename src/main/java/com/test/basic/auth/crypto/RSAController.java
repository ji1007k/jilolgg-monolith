package com.test.basic.auth.crypto;

import com.test.basic.common.utils.RSAUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;

@RestController
@RequestMapping("/rsa")
@Tag(name = "RSA API", description = "RSA 암호화 관련 API")
class RSAController {

    @GetMapping(value = "/generate")
    @Operation(summary = "RSA 암호화 키 생성", description = "사용자 정보 암호화를 위한 RSA 암호화 키를 생성합니다.")
    public ResponseEntity<String> generateRSAKeyPair(HttpSession session) {
        try {
            KeyPair keyPair = RSAUtil.generateRSAKeyPair();
            session.setAttribute("privateKey", keyPair.getPrivate());
            String publicKey = RSAUtil.getPublicKeyAsString(keyPair.getPublic());
            return ResponseEntity.ok().body(publicKey);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
