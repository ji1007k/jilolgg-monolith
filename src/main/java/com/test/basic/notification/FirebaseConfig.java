package com.test.basic.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                try (InputStream serviceAccount = resolveServiceAccountStream()) {
                    if (serviceAccount == null) {
                        log.error("Firebase 초기화 실패: 서비스 계정 정보를 찾을 수 없습니다. " +
                                "firebase.credentials.json 또는 firebase.credentials.path 설정을 확인하세요.");
                        return;
                    }

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("FirebaseApp [DEFAULT] initialized successfully.");
                }
            }
        } catch (Exception e) {
            log.error("Firebase 초기화 중 오류가 발생했습니다.", e);
        }
    }

    private InputStream resolveServiceAccountStream() {
        // 1) 환경변수/설정에 JSON 본문이 직접 들어온 경우 (권장: 비밀키 파일 미커밋)
        if (StringUtils.hasText(firebaseCredentialsJson)) {
            return new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        // 2) 외부 파일 경로를 전달한 경우
        if (StringUtils.hasText(firebaseCredentialsPath)) {
            try {
                return new FileInputStream(firebaseCredentialsPath);
            } catch (Exception e) {
                log.error("firebase.credentials.path 파일을 열 수 없습니다: {}", firebaseCredentialsPath, e);
                return null;
            }
        }

        // 3) 클래스패스 기본 파일 (기존 방식)
        try {
            ClassPathResource resource = new ClassPathResource("jilolgg-firebase-adminsdk-fbsvc-c4bfb4e380.json");
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.error("Classpath Firebase 서비스 계정 파일을 여는 중 오류가 발생했습니다.", e);
        }

        return null;
    }
}
