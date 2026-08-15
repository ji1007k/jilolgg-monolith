import React, { useMemo } from "react";
import { addDays, endOfWeek, format, isSameDay, startOfWeek } from "date-fns";
import ko from "date-fns/locale/ko";
import { useMatchAlarms } from "@components/lol/calendar/hooks/useMatchAlarms.js";
import MatchAlarmButton from "@components/lol/calendar/MatchAlarmButton.jsx";

function dedupeByMatchId(events = []) {
    const map = new Map();
    for (const event of events) {
        if (!event?.matchId) continue;
        if (!map.has(event.matchId)) {
            map.set(event.matchId, event);
        }
    }
    return Array.from(map.values()).sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
}

function normalizeParticipants(participants = []) {
    const teams = [...participants];
    if (teams.length === 1) teams.push({ ...teams[0] });
    return teams.slice(0, 2);
}

function MatchCard({ match, alarmProps }) {
    const isUnstarted = match.state === "unstarted";
    const isLive = match.state === "inProgress";
    const isCompleted = match.state === "completed";
    const blockName = match.blockName ?? "";
    const [teamA, teamB] = normalizeParticipants(match.participants || []);

    const winner = !isCompleted
        ? null
        : teamA?.outcome === "win"
            ? teamA
            : teamB;

    return (
        <div className="schedule-match-card" key={match.matchId}>
            <div className="schedule-status-row">
                <div className="schedule-status-left">
                    {isUnstarted && <span className="schedule-badge">예정</span>}
                    {isLive && <span className="schedule-badge live">LIVE</span>}
                    {isCompleted && <span className="schedule-badge">종료</span>}
                    <span>{format(new Date(match.startTime), "HH:mm")}</span>
                </div>
                <div className="schedule-status-right">
                    <span className="schedule-meta">
                        {blockName}({match.strategy})
                    </span>
                    {/* 월간 달력의 일자별 팝업과 동일한 알림 버튼 */}
                    <MatchAlarmButton match={match} {...alarmProps} />
                </div>
            </div>

            {isUnstarted ? (
                <div className="schedule-teams">
                    <div className="team-left">{teamA?.team?.code || "-"}</div>
                    <div className="score">vs</div>
                    <div className="team-right">{teamB?.team?.code || "-"}</div>
                </div>
            ) : (
                <div className="schedule-teams">
                    <div className="team-left">
                        {teamA?.team?.code || "-"}
                        {isCompleted && (
                            <span className={`schedule-result ${winner?.team?.slug === teamA?.team?.slug ? "win" : "lose"}`}>
                                {winner?.team?.slug === teamA?.team?.slug ? "승" : "패"}
                            </span>
                        )}
                    </div>
                    <div className="score">{teamA?.gameWins ?? 0} : {teamB?.gameWins ?? 0}</div>
                    <div className="team-right">
                        {isCompleted && (
                            <span className={`schedule-result ${winner?.team?.slug === teamB?.team?.slug ? "win" : "lose"}`}>
                                {winner?.team?.slug === teamB?.team?.slug ? "승" : "패"}
                            </span>
                        )}
                        {teamB?.team?.code || "-"}
                    </div>
                </div>
            )}
        </div>
    );
}

/** 알림 토스트. 목록 위에 잠깐 떠서 결과를 알린다. */
function AlarmToast({ toast }) {
    if (!toast) return null;
    return <div className={`schedule-alarm-toast ${toast.type}`}>{toast.message}</div>;
}

function useScheduleAlarms(matches) {
    const { alarmMap, togglingMatchId, toast, toggleAlarm } = useMatchAlarms(matches, true);

    const handleToggle = (match) =>
        toggleAlarm(match, (m) => format(new Date(m.startTime), "M월 d일 HH시 mm분", { locale: ko }));

    return { toast, alarmProps: { alarmMap, togglingMatchId, onToggle: handleToggle } };
}

export function ScheduleDayListView({ events = [], date }) {
    const matches = useMemo(() => dedupeByMatchId(events), [events]);
    const title = format(date, "yyyy년 M월 d일 (EEE)", { locale: ko });
    const { toast, alarmProps } = useScheduleAlarms(matches);

    return (
        <div className="schedule-list-view">
            <AlarmToast toast={toast} />
            <div className="schedule-day-title">{title}</div>
            <div className="schedule-day-body">
                {matches.length === 0 ? (
                    <div className="schedule-empty">해당 날짜 경기 일정이 없습니다.</div>
                ) : (
                    matches.map((match) => (
                        <MatchCard key={match.matchId} match={match} alarmProps={alarmProps} />
                    ))
                )}
            </div>
        </div>
    );
}

export function ScheduleWeekListView({ events = [], date }) {
    const matches = useMemo(() => dedupeByMatchId(events), [events]);
    const weekStart = startOfWeek(date, { weekStartsOn: 1 });
    const weekEnd = endOfWeek(date, { weekStartsOn: 1 });
    const { toast, alarmProps } = useScheduleAlarms(matches);

    const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

    return (
        <div className="schedule-list-view">
            <AlarmToast toast={toast} />
            <div className="schedule-day-title">
                {format(weekStart, "M/d", { locale: ko })} - {format(weekEnd, "M/d", { locale: ko })} 주간 일정
            </div>
            <div className="schedule-week-body">
                {days.map((day) => {
                    const dayMatches = matches.filter((m) => isSameDay(new Date(m.startTime), day));
                    return (
                        <section className="schedule-week-section" key={day.toISOString()}>
                            <div className="schedule-week-day">{format(day, "M월 d일 (EEE)", { locale: ko })}</div>
                            {dayMatches.length === 0 ? (
                                <div className="schedule-empty">일정 없음</div>
                            ) : (
                                dayMatches.map((match) => (
                                    <MatchCard key={match.matchId} match={match} alarmProps={alarmProps} />
                                ))
                            )}
                        </section>
                    );
                })}
            </div>
        </div>
    );
}
