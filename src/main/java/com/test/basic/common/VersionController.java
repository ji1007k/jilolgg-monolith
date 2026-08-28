package com.test.basic.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 지금 떠 있는 것이 어느 빌드인지 밖에서 확인하기 위한 엔드포인트.
 *
 * <p>actuator 는 SCOPE_ADMIN 전용이라 배포 직후 확인에 쓸 수 없다. 실제로 로그인이
 * 깨졌을 때 "무엇이 배포된 상태인지" 조차 알 수 없었다. 그래서 인증 없이 열어둔다.
 *
 * <p>값은 Dockerfile 의 빌드 인자로 주입된다. 로컬 실행이나 이미지 없이 띄운 경우
 * 전부 unknown 이 된다 - 그것도 정보다.
 *
 * <p>커밋 해시 외에는 아무것도 담지 않는다. 여기에 환경변수나 설정값을 얹지 말 것.
 */
@RestController
@Tag(name = "메타", description = "배포 버전 확인")
public class VersionController {

    private final String commit;
    private final String imageTag;
    private final String builtAt;

    public VersionController(
            @Value("${app.git-sha:unknown}") String commit,
            @Value("${app.image-tag:unknown}") String imageTag,
            @Value("${app.built-at:unknown}") String builtAt) {
        this.commit = commit;
        this.imageTag = imageTag;
        this.builtAt = builtAt;
    }

    @GetMapping("/version")
    @Operation(summary = "배포 버전", description = "실행 중인 이미지의 커밋과 빌드 시각을 반환합니다.")
    public Map<String, String> version() {
        return Map.of(
                "commit", commit,
                "imageTag", imageTag,
                "builtAt", builtAt
        );
    }
}
