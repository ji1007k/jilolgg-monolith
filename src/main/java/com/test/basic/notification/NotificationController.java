package com.test.basic.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // POST /api/notification/token
    @PostMapping("/token")
    public ResponseEntity<String> registerToken(@RequestBody Map<String, Object> payload) {
        // 실제 운영 환경에서는 CustomUserDetails나 @AuthenticationPrincipal 에서 유저 ID를 추출합니다.
        Long userId = payload.containsKey("userId") ? Long.parseLong(payload.get("userId").toString()) : 1L;
        String token = (String) payload.get("token");
        String device = (String) payload.get("deviceInfo");
        
        notificationService.registerToken(userId, token, device);
        return ResponseEntity.ok("토큰 등록 완료");
    }

    // POST /api/notification/alarm
    @PostMapping("/alarm")
    public ResponseEntity<String> toggleAlarm(@RequestBody Map<String, Object> payload) {
        Long userId = payload.containsKey("userId") ? Long.parseLong(payload.get("userId").toString()) : 1L;
        String matchId = (String) payload.get("matchId");
        
        boolean isSet = notificationService.toggleAlarm(userId, matchId);
        return ResponseEntity.ok(isSet ? "알림이 설정되었습니다." : "알림이 해제되었습니다.");
    }
}
