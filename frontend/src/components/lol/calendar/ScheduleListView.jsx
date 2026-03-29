import React, { useMemo } from "react";
import { addDays, endOfWeek, format, isSameDay, startOfWeek } from "date-fns";
import ko from "date-fns/locale/ko";

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

function MatchCard({ match }) {
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
                <div className="schedule-meta">
                    {blockName}({match.strategy})
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

export function ScheduleDayListView({ events = [], date }) {
    const matches = useMemo(() => dedupeByMatchId(events), [events]);
    const title = format(date, "yyyy년 M월 d일 (EEE)", { locale: ko });

    return (
        <div className="schedule-list-view">
            <div className="schedule-day-title">{title}</div>
            <div className="schedule-day-body">
                {matches.length === 0 ? (
                    <div className="schedule-empty">해당 날짜 경기 일정이 없습니다.</div>
                ) : (
                    matches.map((match) => <MatchCard key={match.matchId} match={match} />)
                )}
            </div>
        </div>
    );
}

export function ScheduleWeekListView({ events = [], date }) {
    const matches = useMemo(() => dedupeByMatchId(events), [events]);
    const weekStart = startOfWeek(date, { weekStartsOn: 1 });
    const weekEnd = endOfWeek(date, { weekStartsOn: 1 });

    const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

    return (
        <div className="schedule-list-view">
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
                                dayMatches.map((match) => <MatchCard key={match.matchId} match={match} />)
                            )}
                        </section>
                    );
                })}
            </div>
        </div>
    );
}
