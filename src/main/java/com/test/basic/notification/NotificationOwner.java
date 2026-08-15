package com.test.basic.notification;

import org.springframework.util.StringUtils;

/**
 * 알림 구독의 주체.
 *
 * 로그인 없이도 경기 알림을 받을 수 있어야 하므로 "사용자"만으로는 부족하다.
 * 로그인 사용자는 계정을, 비로그인 사용자는 브라우저가 만든 기기 식별자를 주체로 삼는다.
 *
 * DB에는 {@link #key()} 한 값으로 저장한다(fcm_tokens.owner_key, match_alarms.owner_key).
 * 조회가 분기되지 않아 저장소 코드가 단순해진다.
 *
 * @param userId   로그인 사용자의 id. 비로그인이면 null
 * @param deviceId 비로그인 기기 식별자. 로그인이면 null
 */
public record NotificationOwner(Long userId, String deviceId) {

    private static final String USER_PREFIX = "u:";
    private static final String DEVICE_PREFIX = "d:";

    /** 기기 식별자 최대 길이. owner_key 컬럼이 VARCHAR(80)이라 접두사를 뺀 만큼만 받는다. */
    private static final int MAX_DEVICE_ID_LENGTH = 78;

    public static NotificationOwner ofUser(Long userId) {
        return new NotificationOwner(userId, null);
    }

    public static NotificationOwner ofDevice(String deviceId) {
        return new NotificationOwner(null, deviceId);
    }

    /** DB에 저장되는 주체 키. */
    public String key() {
        return userId != null ? USER_PREFIX + userId : DEVICE_PREFIX + deviceId;
    }

    /**
     * 클라이언트가 보낸 기기 식별자가 쓸 만한지 검사한다.
     * 길이를 넘기면 owner_key 컬럼에서 잘려 다른 기기와 충돌할 수 있으므로 거른다.
     */
    public static boolean isValidDeviceId(String deviceId) {
        return StringUtils.hasText(deviceId) && deviceId.length() <= MAX_DEVICE_ID_LENGTH;
    }
}
