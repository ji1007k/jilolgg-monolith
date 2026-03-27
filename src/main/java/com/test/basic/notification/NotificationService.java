package com.test.basic.notification;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class NotificationService {

    private final FcmTokenRepository tokenRepository;
    private final MatchAlarmRepository alarmRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public void registerToken(Long userId, String tokenStr, String deviceInfo) {
        FcmToken token = FcmToken.builder()
                .userId(userId)
                .token(tokenStr)
                .deviceInfo(deviceInfo)
                .updatedAt(LocalDateTime.now())
                .build();
        tokenRepository.save(token);
    }

    @Transactional
    public boolean toggleAlarm(Long userId, String matchId) {
        Optional<MatchAlarm> existing = alarmRepository.findByUserIdAndMatchId(userId, matchId);
        if (existing.isPresent()) {
            alarmRepository.delete(existing.get());
            log.info("Alarm disabled for user {} match {}", userId, matchId);
            return false; // 알람 해제
        } else {
            alarmRepository.save(MatchAlarm.builder().userId(userId).matchId(matchId).build());
            log.info("Alarm enabled for user {} match {}", userId, matchId);
            return true; // 알람 설정
        }
    }

    /**
     * 매 분마다 실행되어 10분 뒤 시작할 경기를 찾고 FCM 푸시를 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void scheduleMatchAlarms() {
        LocalDateTime now = LocalDateTime.now();
        // 10분 뒤에 시작하는 경기를 탐색 (전후 1분 여유를 둡니다)
        LocalDateTime targetTimeStart = now.plusMinutes(9).withSecond(0).withNano(0);
        LocalDateTime targetTimeEnd = now.plusMinutes(11).withSecond(0).withNano(0);
        
        List<Match> upcomingMatches = matchRepository.findMatchesStartingBetween(targetTimeStart, targetTimeEnd);
        if (upcomingMatches.isEmpty()) {
            return;
        }

        log.info("{} 시간에 시작하는 경기가 {}개 있습니다. 알림 발송을 시작합니다.", targetTimeStart, upcomingMatches.size());
        
        for (Match match : upcomingMatches) {
            List<MatchAlarm> alarms = alarmRepository.findByMatchId(match.getMatchId());
            for (MatchAlarm alarm : alarms) {
                List<FcmToken> tokens = tokenRepository.findByUserId(alarm.getUserId());
                for (FcmToken t : tokens) {
                    // 팀 이름 등을 조합해서 알림 메시지 생성
                    String title = "경기 시작 10분 전!";
                    String body = String.format("[%s] 경기가 곧 시작됩니다. 놓치지 마세요!", match.getBlockName());
                    sendFcmPush(t.getToken(), title, body);
                }
            }
        }
    }

    public void sendFcmPush(String targetToken, String title, String body) {
        try {
            com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                 .setToken(targetToken)
                 .setNotification(com.google.firebase.messaging.Notification.builder()
                         .setTitle(title).setBody(body).build())
                 .build();
            com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
            
            log.info("FCM Send TO: [{}] TITLE: [{}] BODY: [{}]", targetToken, title, body);
        } catch (Exception e) {
            log.error("FCM Push Error", e);
        }
    }
}
