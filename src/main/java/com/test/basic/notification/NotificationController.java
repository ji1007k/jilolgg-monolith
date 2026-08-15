package com.test.basic.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "07. Notification", description = "푸시 알림 및 FCM 토큰 관리 API")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "02_BearerAuth")
public class NotificationController {

    /** 비로그인 사용자를 구분하는 브라우저 생성 식별자. */
    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final NotificationService notificationService;

    // POST /api/notification/token
    @PostMapping("/token")
    @io.swagger.v3.oas.annotations.Operation(summary = "FCM 토큰 등록", description = "기기별 FCM 토큰을 서버에 등록합니다.")
    public ResponseEntity<Map<String, Object>> registerToken(@RequestBody Map<String, Object> payload,
                                                             Authentication authentication,
                                                             @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {
        NotificationOwner owner = resolveOwner(authentication, deviceId);
        String token = (String) payload.get("token");
        String device = (String) payload.get("deviceInfo");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "token 값이 필요합니다."
            ));
        }

        notificationService.registerToken(owner, token, device);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "토큰 등록 완료"
        ));
    }

    // POST /api/notification/alarm
    @PostMapping("/alarm")
    @io.swagger.v3.oas.annotations.Operation(summary = "경기 알림 토글", description = "특정 경기에 대한 푸시 알림 수신 여부를 설정/해제합니다.")
    public ResponseEntity<Map<String, Object>> toggleAlarm(@RequestBody Map<String, Object> payload,
                                                           Authentication authentication,
                                                           @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {
        NotificationOwner owner = resolveOwner(authentication, deviceId);
        String matchId = (String) payload.get("matchId");

        if (matchId == null || matchId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "matchId 값이 필요합니다."
            ));
        }

        boolean isSet = notificationService.toggleAlarm(owner, matchId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "matchId", matchId,
                "enabled", isSet,
                "message", isSet ? "알림이 설정되었습니다." : "알림이 해제되었습니다."
        ));
    }

    // GET /api/notification/alarm?matchIds=abc,def
    @GetMapping("/alarm")
    @io.swagger.v3.oas.annotations.Operation(summary = "알림 설정 상태 조회", description = "여러 경기에 대해 현재 사용자가 설정한 알림 상태를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getAlarmStatus(@RequestParam(required = false) String matchIds,
                                                               Authentication authentication,
                                                               @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {
        NotificationOwner owner = resolveOwner(authentication, deviceId);

        List<String> matchIdList = parseMatchIds(matchIds);
        if (matchIdList.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "enabledMatchIds", Collections.emptyList()
            ));
        }

        Set<String> enabledMatchIds = notificationService.getEnabledMatchIds(owner, matchIdList);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "enabledMatchIds", enabledMatchIds
        ));
    }

    // POST /api/notification/test
    @PostMapping("/test")
    @io.swagger.v3.oas.annotations.Operation(summary = "테스트 알림 발송 (POST)", description = "현재 사용자에게 테스트 푸시 알림을 발송합니다.")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody(required = false) Map<String, Object> payload,
                                                                     Authentication authentication,
                                                                     @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {
        NotificationOwner owner = resolveOwner(authentication, deviceId);
        String title = payload != null && payload.get("title") != null
                ? payload.get("title").toString()
                : "JILoL.gg 테스트 알림";
        String body = payload != null && payload.get("body") != null
                ? payload.get("body").toString()
                : "푸시 알림이 정상 동작합니다.";

        int sentCount = notificationService.sendTestPushToUser(owner, title, body);
        if (sentCount == 0) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "등록된 FCM 토큰이 없습니다. 먼저 알림 권한을 허용하고 알림을 1회 설정해주세요.",
                    "sentCount", 0
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 알림 발송 요청 완료",
                "sentCount", sentCount
        ));
    }

    // GET /api/notification/test (CSRF 없이 간편 테스트)
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotificationByGet(@RequestParam(required = false) String title,
                                                                          @RequestParam(required = false) String body,
                                                                          Authentication authentication,
                                                                          @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {
        NotificationOwner owner = resolveOwner(authentication, deviceId);
        String safeTitle = (title == null || title.isBlank()) ? "JILoL.gg 테스트 알림" : title;
        String safeBody = (body == null || body.isBlank()) ? "푸시 알림이 정상 동작합니다." : body;

        int sentCount = notificationService.sendTestPushToUser(owner, safeTitle, safeBody);
        if (sentCount == 0) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "등록된 FCM 토큰이 없습니다. 먼저 알림 권한을 허용하고 알림을 1회 설정해주세요.",
                    "sentCount", 0
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 알림 발송 요청 완료",
                "sentCount", sentCount
        ));
    }

    /**
     * 알림 구독 주체를 결정한다.
     *
     * 로그인 상태면 계정을, 아니면 클라이언트가 보낸 기기 식별자(X-Device-Id)를 쓴다.
     * 로그인 없이도 알림을 받을 수 있게 하려는 것이므로 인증을 요구하지 않는다.
     * 다만 둘 다 없으면 구독을 어디에 매달지 알 수 없어 거절한다.
     */
    private NotificationOwner resolveOwner(Authentication authentication, String deviceId) {
        Long userId = extractUserId(authentication);
        if (userId != null) {
            return NotificationOwner.ofUser(userId);
        }

        if (NotificationOwner.isValidDeviceId(deviceId)) {
            return NotificationOwner.ofDevice(deviceId);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "로그인하거나 X-Device-Id 헤더를 보내야 합니다.");
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            return null;
        }

        try {
            if (principal instanceof Jwt jwt) {
                return Long.parseLong(jwt.getSubject());
            }
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // 사용자 id로 해석할 수 없는 인증 주체. 비로그인으로 취급한다.
            return null;
        }
    }

    private List<String> parseMatchIds(String matchIds) {
        if (matchIds == null || matchIds.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(matchIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
