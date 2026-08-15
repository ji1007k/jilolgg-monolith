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
    public void registerToken(NotificationOwner owner, String tokenStr, String deviceInfo) {
        // 같은 기기에서 비로그인으로 쓰다 로그인하면 동일 FCM 토큰이 'd:'와 'u:' 양쪽에 남아
        // 푸시가 중복 발송된다. 토큰 하나는 주체 하나만 갖도록 정리한다.
        // 같은 토큰을 쓰던 이전 주체는 곧 이 사용자이므로, 이때 구독도 함께 넘긴다.
        tokenRepository.findByToken(tokenStr).stream()
                .filter(existing -> !owner.key().equals(existing.getOwnerKey()))
                .forEach(stale -> {
                    log.info("FCM 토큰 주체 이동: {} -> {}", stale.getOwnerKey(), owner.key());
                    migrateAlarms(stale.getOwnerKey(), owner);
                    tokenRepository.delete(stale);
                });

        Optional<FcmToken> existingToken = tokenRepository.findByOwnerKeyAndToken(owner.key(), tokenStr);
        if (existingToken.isPresent()) {
            FcmToken token = existingToken.get();
            token.setDeviceInfo(deviceInfo);
            token.setUpdatedAt(LocalDateTime.now());
            return;
        }

        tokenRepository.save(FcmToken.builder()
                .ownerKey(owner.key())
                .userId(owner.userId())
                .token(tokenStr)
                .deviceInfo(deviceInfo)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    /**
     * 이전 주체(보통 비로그인 기기)가 걸어둔 경기 알림을 새 주체로 옮긴다.
     * 이미 같은 경기를 구독 중이면 중복이므로 이전 것을 버린다.
     */
    private void migrateAlarms(String fromOwnerKey, NotificationOwner to) {
        List<MatchAlarm> alarms = alarmRepository.findByOwnerKey(fromOwnerKey);
        if (alarms.isEmpty()) {
            return;
        }

        for (MatchAlarm alarm : alarms) {
            boolean alreadySubscribed = alarmRepository
                    .findByOwnerKeyAndMatchId(to.key(), alarm.getMatchId())
                    .isPresent();

            if (alreadySubscribed) {
                alarmRepository.delete(alarm);
                continue;
            }

            alarm.setOwnerKey(to.key());
            alarm.setUserId(to.userId());
        }

        log.info("경기 알림 {}건을 {} -> {}로 이관", alarms.size(), fromOwnerKey, to.key());
    }

    @Transactional
    public boolean toggleAlarm(NotificationOwner owner, String matchId) {
        Optional<MatchAlarm> existing = alarmRepository.findByOwnerKeyAndMatchId(owner.key(), matchId);
        if (existing.isPresent()) {
            alarmRepository.delete(existing.get());
            log.info("Alarm disabled for {} match {}", owner.key(), matchId);
            return false; // 알람 해제
        } else {
            alarmRepository.save(MatchAlarm.builder()
                    .ownerKey(owner.key())
                    .userId(owner.userId())
                    .matchId(matchId)
                    .build());
            log.info("Alarm enabled for {} match {}", owner.key(), matchId);
            return true; // 알람 설정
        }
    }

    @Transactional(readOnly = true)
    public Set<String> getEnabledMatchIds(NotificationOwner owner, List<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Set.of();
        }

        return alarmRepository.findByOwnerKeyAndMatchIdIn(owner.key(), matchIds)
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
                List<FcmToken> tokens = tokenRepository.findByOwnerKey(alarm.getOwnerKey());
                // 같은 주체에서 동일 토큰이 중복 저장되었을 수 있어 중복 발송을 방지합니다.
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
    public int sendTestPushToUser(NotificationOwner owner, String title, String body) {
        List<FcmToken> tokens = tokenRepository.findByOwnerKey(owner.key());
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
