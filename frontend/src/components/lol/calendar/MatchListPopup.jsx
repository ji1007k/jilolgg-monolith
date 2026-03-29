import { useEffect, useMemo, useState } from 'react';
import Popup from 'reactjs-popup';
import format from 'date-fns/format';
import 'reactjs-popup/dist/index.css';
import { FiBell, FiBellOff, FiX } from 'react-icons/fi';
import ko from "date-fns/locale/ko";
import Loading from "@components/common/Loading.js";
import {useSwipeable} from "react-swipeable";
import { useAuth } from "@/context/AuthContext.js";
import { apiGetAlarmStatus, apiToggleMatchAlarm } from "@/utils/api-notification.js";
import { requestForToken } from "@/utils/firebase.js";

function MatchListPopup ({ open, onClose, matches, date, isLoading, onPrevDate, onNextDate }) {
    const matchDate = format(new Date(date), "yyyy년 M월 d일 (EEE)", { locale: ko });
    const { userId } = useAuth();
    const [alarmMap, setAlarmMap] = useState({});
    const [togglingMatchId, setTogglingMatchId] = useState(null);
    const [toast, setToast] = useState(null);

    const unstartedMatchIds = useMemo(() => {
        return (matches || [])
            .filter(match => match.state === "unstarted")
            .map(match => match.matchId);
    }, [matches]);

    useEffect(() => {
        if (!open || !userId || unstartedMatchIds.length === 0) {
            setAlarmMap({});
            return;
        }

        let cancelled = false;

        async function fetchAlarmStatus() {
            try {
                const enabledMatchIds = await apiGetAlarmStatus(unstartedMatchIds);
                if (cancelled) return;

                const nextAlarmMap = {};
                enabledMatchIds.forEach((matchId) => {
                    nextAlarmMap[matchId] = true;
                });
                setAlarmMap(nextAlarmMap);
            } catch (error) {
                if (!cancelled) {
                    console.error("알림 상태 조회 실패:", error);
                }
            }
        }

        fetchAlarmStatus();

        return () => {
            cancelled = true;
        };
    }, [open, userId, unstartedMatchIds]);

    useEffect(() => {
        if (!toast) return;

        const timer = setTimeout(() => {
            setToast(null);
        }, 2500);

        return () => clearTimeout(timer);
    }, [toast]);

    const handlers = useSwipeable({
        onSwipedLeft: () => onNextDate(),  // 오른쪽에서 왼쪽으로 스와이프 → 다음 날짜
        onSwipedRight: () => onPrevDate(), // 왼쪽에서 오른쪽으로 스와이프 → 이전 날짜
        preventScrollOnSwipe: true, // 스와이프가 인식되었을 때만 preventDefault()를 호출해서 브라우저 스크롤을 막음
        trackMouse: true, // 데스크탑에서도 마우스로 테스트 가능
    });

    const handleToggleAlarm = async (match) => {
        const matchId = match.matchId;

        if (!userId) {
            setToast({ type: "error", message: "알림 설정은 로그인 후 사용할 수 있습니다." });
            return;
        }

        setTogglingMatchId(matchId);
        try {
            const tokenResult = await requestForToken();
            if (!tokenResult?.ok) {
                if (tokenResult?.reason === "permission_denied") {
                    setToast({ type: "error", message: "브라우저 알림 권한이 필요합니다. 사이트 알림을 허용해주세요." });
                } else {
                    setToast({ type: "error", message: "알림 토큰 발급에 실패했습니다. 페이지를 새로고침 후 다시 시도해주세요." });
                }
                return;
            }

            const result = await apiToggleMatchAlarm(matchId);
            const isEnabled = Boolean(result.enabled);

            setAlarmMap(prev => ({
                ...prev,
                [matchId]: isEnabled,
            }));

            if (isEnabled) {
                const matchStartTimeText = format(new Date(match.startTime), "yyyy년 MM월 dd일 HH시 mm분", { locale: ko });
                setToast({ type: "success", message: `${matchStartTimeText}에 알림이 설정되었습니다.` });
            } else {
                setToast({ type: "info", message: "알림 설정이 해제되었습니다." });
            }
        } catch (error) {
            console.error("알림 설정 변경 실패:", error);
            setToast({ type: "error", message: "알림 설정 변경에 실패했습니다." });
        } finally {
            setTogglingMatchId(null);
        }
    };

    return (
        <Popup
            open={open}
            onClose={onClose}
            modal
            closeOnDocumentClick={false}
            contentStyle={{}} // contentStyle 비워두고 className으로만 조절
        >
            {(close) => (
                <div className="match-list-popup" {...handlers}>
                    {/* 상단 제목 */}
                    <div className="popup-header">
                        <span>{matchDate} 경기 일정</span>
                        <button
                            onClick={() => {
                                close();
                                onClose();
                            }}
                            className="close-btn"
                        >
                            <FiX />
                        </button>
                    </div>

                    {toast && (
                        <div className={`alarm-toast ${toast.type}`}>
                            {toast.message}
                        </div>
                    )}

                    {isLoading ? (
                        <div className="popup-body">
                            <Loading message="경기 일정 로딩 중..." />
                        </div>
                    ) : (
                        <div className="popup-body">
                            {matches.length === 0 ? (
                                <p>해당 날짜에 경기가 없습니다.</p>
                            ) : (
                                matches.map((match) => {
                                    const isUnstarted = match.state === "unstarted";
                                    const isLive = match.state === "inProgress";
                                    const isCompleted = match.state === "completed";

                                    const participants = [...match.participants];
                                    if (participants.length === 1) participants.push({ ...participants[0] });
                                    const [teamA, teamB] = participants;
                                    const winner = !isCompleted ? null :
                                        teamA.outcome === 'win' ? teamA : teamB;


                                    return (
                                        <div key={match.matchId} className="match-card">
                                            <div className="status-time-row">
                                                <div className="status-labels">
                                                    {isUnstarted && <span className="label unstarted">예정</span>}
                                                    {isLive && <span className="label live">LIVE</span>}
                                                    {isCompleted && <span className="label completed">종료</span>}
                                                    <span>{format(new Date(match.startTime), 'HH:mm')}</span>
                                                </div>

                                                {isUnstarted && (
                                                    <button
                                                        type="button"
                                                        className={`alarm-bell-btn ${alarmMap[match.matchId] ? "active" : "inactive"}`}
                                                        onClick={() => handleToggleAlarm(match)}
                                                        disabled={togglingMatchId === match.matchId}
                                                        aria-label={alarmMap[match.matchId] ? "알림 해제" : "알림 설정"}
                                                        title={alarmMap[match.matchId] ? "알림 해제" : "알림 설정"}
                                                    >
                                                        {togglingMatchId === match.matchId ? (
                                                            <span className="alarm-bell-loading">...</span>
                                                        ) : alarmMap[match.matchId] ? (
                                                            <FiBell />
                                                        ) : (
                                                            <FiBellOff />
                                                        )}
                                                    </button>
                                                )}
                                            </div>

                                            {(isLive || isCompleted)  && (
                                                <div className="flex justify-center">
                                                    <div className="label strategy">
                                                        {`${match.blockName ?? ""}(${match.strategy})`}
                                                    </div>
                                                </div>
                                            )}

                                            {isUnstarted ? (
                                                <div className="teams-row">
                                                    <div className="team team-left">
                                                        <span className="team-code">{teamA.team.code}</span>
                                                    </div>
                                                    <div className="label strategy">
                                                        {`${match.blockName ?? ""}(${match.strategy})`}
                                                    </div>
                                                    <div className="team team-right">
                                                        <span className="team-code">{teamB.team.code}</span>
                                                    </div>
                                                </div>
                                            ) : (
                                                <div className="teams-row">
                                                    <div className="team team-left">
                                                        <span className="team-code">{teamA.team.code}</span>
                                                        {isCompleted &&
                                                            (<span className={`result ${winner?.team.slug === teamA.team.slug ? "win" : "lose"}`}>
                                                                {winner?.team.slug === teamA.team.slug ? "승" : "패"}
                                                            </span>)
                                                        }
                                                    </div>
                                                    <div className="score">
                                                        {teamA.gameWins ?? 0} : {teamB.gameWins ?? 0}
                                                    </div>
                                                    <div className="team team-right">
                                                        {isCompleted &&
                                                            (<span className={`result ${winner?.team.slug === teamB.team.slug ? "win" : "lose"}`}>
                                                                {winner?.team.slug === teamB.team.slug ? "승" : "패"}
                                                            </span>)
                                                        }
                                                        <span className="team-code">{teamB.team.code}</span>
                                                    </div>
                                                </div>
                                            )
                                            }

                                        </div>
                                    )
                                })
                            )}
                        </div>
                    )}

                    {/* 하단 닫기 */}
                    <div className="popup-footer">
                        <button
                            onClick={() => {
                                close();
                                onClose();
                            }}
                            className="footer-btn"
                        >
                            닫기
                        </button>
                    </div>
                </div>
            )}
        </Popup>
    );
};

export default MatchListPopup;
