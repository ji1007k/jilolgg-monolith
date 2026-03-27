package com.test.basic.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // POST /api/notification/token
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> registerToken(@RequestBody Map<String, Object> payload,
                                                             Authentication authentication) {
        Long userId = resolveUserId(authentication);
        String token = (String) payload.get("token");
        String device = (String) payload.get("deviceInfo");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "token 값이 필요합니다."
            ));
        }

        notificationService.registerToken(userId, token, device);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "토큰 등록 완료"
        ));
    }

    // POST /api/notification/alarm
    @PostMapping("/alarm")
    public ResponseEntity<Map<String, Object>> toggleAlarm(@RequestBody Map<String, Object> payload,
                                                           Authentication authentication) {
        Long userId = resolveUserId(authentication);
        String matchId = (String) payload.get("matchId");

        if (matchId == null || matchId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "matchId 값이 필요합니다."
            ));
        }

        boolean isSet = notificationService.toggleAlarm(userId, matchId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "matchId", matchId,
                "enabled", isSet,
                "message", isSet ? "알림이 설정되었습니다." : "알림이 해제되었습니다."
        ));
    }

    // GET /api/notification/alarm?matchIds=abc,def
    @GetMapping("/alarm")
    public ResponseEntity<Map<String, Object>> getAlarmStatus(@RequestParam(required = false) String matchIds,
                                                               Authentication authentication) {
        Long userId = resolveUserId(authentication);

        List<String> matchIdList = parseMatchIds(matchIds);
        if (matchIdList.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "enabledMatchIds", Collections.emptyList()
            ));
        }

        Set<String> enabledMatchIds = notificationService.getEnabledMatchIds(userId, matchIdList);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "enabledMatchIds", enabledMatchIds
        ));
    }

    // POST /api/notification/test
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody(required = false) Map<String, Object> payload,
                                                                     Authentication authentication) {
        Long userId = resolveUserId(authentication);
        String title = payload != null && payload.get("title") != null
                ? payload.get("title").toString()
                : "JILoL.gg 테스트 알림";
        String body = payload != null && payload.get("body") != null
                ? payload.get("body").toString()
                : "푸시 알림이 정상 동작합니다.";

        int sentCount = notificationService.sendTestPushToUser(userId, title, body);
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
                                                                          Authentication authentication) {
        Long userId = resolveUserId(authentication);
        String safeTitle = (title == null || title.isBlank()) ? "JILoL.gg 테스트 알림" : title;
        String safeBody = (body == null || body.isBlank()) ? "푸시 알림이 정상 동작합니다." : body;

        int sentCount = notificationService.sendTestPushToUser(userId, safeTitle, safeBody);
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

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return Long.parseLong(jwt.getSubject());
        }

        return Long.parseLong(authentication.getName());
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
