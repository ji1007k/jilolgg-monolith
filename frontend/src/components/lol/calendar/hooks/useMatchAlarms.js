"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiGetAlarmStatus, apiToggleMatchAlarm } from "@/utils/api-notification.js";
import { requestForToken } from "@/utils/firebase.js";
import { useAuth } from "@/context/AuthContext.js";

/**
 * 경기 알림 상태 관리.
 *
 * 원래 MatchListPopup 안에만 있어서 월간 달력의 일자별 팝업에서만 알림을 켤 수 있었다.
 * 주간 일정과 경기 상세에서도 같은 동작이 필요해 훅으로 분리한다.
 *
 * 로그인 여부는 신경 쓰지 않는다 — api-notification이 비로그인이면 기기 식별자를 붙여 보낸다.
 *
 * @param {Array} matches 화면에 보이는 경기 목록
 * @param {boolean} enabled 화면이 닫혀 있을 때 불필요한 조회를 막기 위한 스위치
 */
export function useMatchAlarms(matches, enabled = true) {
    const { userId } = useAuth();
    const [alarmMap, setAlarmMap] = useState({});
    const [togglingMatchId, setTogglingMatchId] = useState(null);
    const [toast, setToast] = useState(null);

    // 알림을 걸 수 있는 건 아직 시작하지 않은 경기뿐이다.
    const unstartedMatchIds = useMemo(() => {
        return (matches || [])
            .filter((match) => match?.state === "unstarted" && match?.matchId)
            .map((match) => match.matchId);
    }, [matches]);

    // 배열은 매 렌더 새로 만들어지므로 문자열로 비교해 불필요한 재조회를 막는다.
    const unstartedKey = unstartedMatchIds.join(",");

    useEffect(() => {
        if (!enabled || unstartedMatchIds.length === 0) {
            setAlarmMap({});
            return;
        }

        let cancelled = false;

        (async () => {
            try {
                const enabledMatchIds = await apiGetAlarmStatus(unstartedMatchIds);
                if (cancelled) return;

                const next = {};
                enabledMatchIds.forEach((matchId) => {
                    next[matchId] = true;
                });
                setAlarmMap(next);
            } catch (error) {
                if (!cancelled) {
                    console.error("알림 상태 조회 실패:", error);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
        // userId가 바뀌면(로그인/로그아웃) 구독 주체가 달라지므로 다시 조회한다.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [enabled, unstartedKey, userId]);

    useEffect(() => {
        if (!toast) return;

        const timer = setTimeout(() => setToast(null), 2500);
        return () => clearTimeout(timer);
    }, [toast]);

    const toggleAlarm = useCallback(async (match, formatStartTime) => {
        const matchId = match?.matchId;
        if (!matchId) return;

        setTogglingMatchId(matchId);
        try {
            // 알림을 받으려면 브라우저 권한과 FCM 토큰이 먼저 있어야 한다.
            const tokenResult = await requestForToken();
            if (!tokenResult?.ok) {
                setToast({
                    type: "error",
                    message: tokenResult?.reason === "permission_denied"
                        ? "브라우저 알림 권한이 필요합니다. 사이트 알림을 허용해주세요."
                        : "알림 토큰 발급에 실패했습니다. 페이지를 새로고침 후 다시 시도해주세요.",
                });
                return;
            }

            const result = await apiToggleMatchAlarm(matchId);
            const isEnabled = Boolean(result.enabled);

            setAlarmMap((prev) => ({ ...prev, [matchId]: isEnabled }));

            if (isEnabled) {
                const when = formatStartTime ? formatStartTime(match) : null;
                setToast({
                    type: "success",
                    message: when ? `${when}에 알림이 설정되었습니다.` : "알림이 설정되었습니다.",
                });
            } else {
                setToast({ type: "info", message: "알림 설정이 해제되었습니다." });
            }
        } catch (error) {
            console.error("알림 설정 변경 실패:", error);
            setToast({ type: "error", message: "알림 설정에 실패했습니다. 잠시 후 다시 시도해주세요." });
        } finally {
            setTogglingMatchId(null);
        }
    }, []);

    return { alarmMap, togglingMatchId, toast, setToast, toggleAlarm };
}
