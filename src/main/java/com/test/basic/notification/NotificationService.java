package com.test.basic.notification;

import com.test.basic.lol.domain.match.Match;
import com.test.basic.lol.domain.match.MatchRepository;
import com.google.firebase.FirebaseApp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Optional<FcmToken> existingToken = tokenRepository.findByUserIdAndToken(userId, tokenStr);
        if (existingToken.isPresent()) {
            FcmToken token = existingToken.get();
            token.setDeviceInfo(deviceInfo);
            token.setUpdatedAt(LocalDateTime.now());
            return;
        }

        tokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token(tokenStr)
                .deviceInfo(deviceInfo)
                .updatedAt(LocalDateTime.now())
                .build());
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

    @Transactional(readOnly = true)
    public Set<String> getEnabledMatchIds(Long userId, List<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Set.of();
        }

        return alarmRepository.findByUserIdAndMatchIdIn(userId, matchIds)
                .stream()
                .map(MatchAlarm::getMatchId)
                .collect(Collectors.toSet());
    }

    /**
     * 매 분마다 실행되어 "지금 시작하는 경기"를 찾아 FCM 푸시를 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void scheduleMatchAlarms() {
        LocalDateTime now = LocalDateTime.now();
        // 현재 분(초 0)부터 다음 분 직전까지를 "경기 시작 시각"으로 간주
        LocalDateTime targetTimeStart = now.withSecond(0).withNano(0);
        LocalDateTime targetTimeEnd = targetTimeStart.plusMinutes(1);
        
        List<Match> upcomingMatches = matchRepository.findMatchesStartingBetween(targetTimeStart, targetTimeEnd);
        if (upcomingMatches.isEmpty()) {
            return;
        }

        log.info("{} 시각에 시작하는 경기가 {}개 있습니다. 알림 발송을 시작합니다.", targetTimeStart, upcomingMatches.size());
        
        for (Match match : upcomingMatches) {
            List<MatchAlarm> alarms = alarmRepository.findByMatchId(match.getMatchId());
            for (MatchAlarm alarm : alarms) {
                List<FcmToken> tokens = tokenRepository.findByUserId(alarm.getUserId());
                // 같은 유저에서 동일 토큰이 중복 저장되었을 수 있어 중복 발송을 방지합니다.
                List<String> distinctTokens = new ArrayList<>(tokens.stream()
                        .map(FcmToken::getToken)
                        .collect(Collectors.toSet()));

                for (String token : distinctTokens) {
                    // 팀 이름 등을 조합해서 알림 메시지 생성
                    String title = "경기 시작 알림";
                    String body = String.format("[%s] 경기가 시작되었습니다. 지금 확인해보세요!", match.getBlockName());
                    sendFcmPush(token, title, body);
                }
            }
        }
    }

    public void sendFcmPush(String targetToken, String title, String body) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.error("FCM Push skipped: FirebaseApp [DEFAULT] is not initialized.");
                return;
            }

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

    @Transactional(readOnly = true)
    public int sendTestPushToUser(Long userId, String title, String body) {
        List<FcmToken> tokens = tokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            return 0;
        }

        List<String> distinctTokens = new ArrayList<>(tokens.stream()
                .map(FcmToken::getToken)
                .collect(Collectors.toSet()));

        for (String token : distinctTokens) {
            sendFcmPush(token, title, body);
        }

        return distinctTokens.size();
    }
}
