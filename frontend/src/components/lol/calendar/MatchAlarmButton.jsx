"use client";

import { FiBell, FiBellOff } from "react-icons/fi";

/**
 * 경기 알림 종 버튼.
 *
 * 월간 달력의 일자별 팝업, 주간 일정, 경기 상세 어디서나 같은 모양·동작으로 쓴다.
 * 상태 관리는 useMatchAlarms 훅이 담당하고 여기서는 표시만 한다.
 *
 * 시작하지 않은 경기에만 의미가 있으므로 그 외에는 아무것도 그리지 않는다.
 */
const MatchAlarmButton = ({ match, alarmMap, togglingMatchId, onToggle, className = "" }) => {
    if (match?.state !== "unstarted" || !match?.matchId) return null;

    const isOn = Boolean(alarmMap?.[match.matchId]);
    const isToggling = togglingMatchId === match.matchId;
    const label = isOn ? "알림 해제" : "알림 설정";

    return (
        <button
            type="button"
            className={`alarm-bell-btn ${isOn ? "active" : "inactive"} ${className}`.trim()}
            onClick={(e) => {
                // 카드/이벤트 클릭으로 상세 팝업이 함께 열리는 것을 막는다.
                e.stopPropagation();
                onToggle(match);
            }}
            disabled={isToggling}
            aria-label={label}
            title={label}
        >
            {isToggling ? <span className="alarm-bell-loading">...</span> : isOn ? <FiBell /> : <FiBellOff />}
        </button>
    );
};

export default MatchAlarmButton;
