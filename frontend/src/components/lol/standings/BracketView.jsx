import React, { useMemo } from 'react';

// previousMatchIds(이전 라운드 경기 참조)의 깊이로 라운드를 계산한다.
// 같은 섹션에 없는 id는 무시하고, 순환 참조가 있어도 무한 재귀하지 않도록 방문 표시를 둔다.
function groupByRound(matches) {
    const byId = new Map(matches.map(m => [m.matchId, m]));
    const depthCache = new Map();

    const depthOf = (m, visiting = new Set()) => {
        if (depthCache.has(m.matchId)) return depthCache.get(m.matchId);
        if (visiting.has(m.matchId)) return 0;
        visiting.add(m.matchId);
        const prevDepths = (m.previousMatchIds || [])
            .map(id => byId.get(id))
            .filter(Boolean)
            .map(p => depthOf(p, visiting) + 1);
        const d = prevDepths.length ? Math.max(...prevDepths) : 0;
        depthCache.set(m.matchId, d);
        return d;
    };

    const rounds = [];
    matches.forEach(m => {
        const d = depthOf(m);
        (rounds[d] = rounds[d] || []).push(m);
    });
    return rounds.filter(Boolean);
}

const STATE_LABEL = { completed: '종료', inProgress: '진행 중' };

const BracketView = ({ matches }) => {
    const rounds = useMemo(() => groupByRound(matches || []), [matches]);

    return (
        <div className="bracket-view">
            {rounds.map((round, ri) => {
                const isLast = ri === rounds.length - 1;
                const title = isLast && rounds.length > 1 ? '결승' : `${ri + 1}라운드`;
                return (
                    <div className="bracket-round" key={ri}>
                        <div className="bracket-round-title">{title}</div>
                        <ul className="stage-match-list">
                            {round.map((m, idx) => (
                                <li key={m.matchId || idx} className="stage-match-item">
                                    {(m.teams || []).map((t, i) => (
                                        <span
                                            key={i}
                                            className={`stage-match-team${t.result?.outcome === 'win' ? ' winner' : ''}`}
                                        >
                                            {t.code || t.name || 'TBD'}
                                            {m.state !== 'unstarted' && ` ${t.result?.gameWins ?? 0}`}
                                        </span>
                                    ))}
                                    <span className="stage-match-state">
                                        {STATE_LABEL[m.state] || '예정'}
                                    </span>
                                </li>
                            ))}
                        </ul>
                    </div>
                );
            })}
        </div>
    );
};

export default BracketView;
