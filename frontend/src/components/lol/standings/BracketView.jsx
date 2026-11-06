import React, { useMemo } from 'react';

// 실제 getStandings 응답(worlds_2026)을 확인한 결과 previousMatchIds는 모든 스테이지에서 비어 있다.
// 그래서 라운드를 세 단계로 추정한다.
// ① previousMatchIds 깊이(값이 있을 때만)
// ② 단판 토너먼트(경기 수 2^k-1)는 8강/4강/결승처럼 절반씩 줄어드는 순서로
// ③ 스위스는 경기 전 전적(W-L)이 같은 경기끼리
// 어디에도 맞지 않으면 하나로 묶는다.

// 같은 섹션에 없는 id는 무시하고, 순환 참조가 있어도 무한 재귀하지 않도록 방문 표시를 둔다.
function groupByPrevious(matches) {
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
    return rounds.filter(Boolean).map((items, i, arr) => ({
        title: i === arr.length - 1 && arr.length > 1 ? '결승' : `${i + 1}라운드`,
        items,
    }));
}

// 경기 수가 2^k-1이면 응답 순서대로 첫 라운드 2^(k-1)경기, 다음 절반… 마지막 1경기로 본다.
function groupBySingleElimination(matches) {
    const total = matches.length + 1;
    if (matches.length < 3 || (total & (total - 1)) !== 0) return null;
    const rounds = [];
    let size = total / 2;
    let offset = 0;
    while (size >= 1) {
        const title = size === 1 ? '결승' : size === 2 ? '4강' : `${size * 2}강`;
        rounds.push({ title, items: matches.slice(offset, offset + size) });
        offset += size;
        size /= 2;
    }
    return rounds;
}

// 응답 순서대로 승패를 누적해 경기 직전 전적을 구한다. 두 팀 전적이 다르면 스위스가 아니므로 포기한다.
function groupBySwissRecord(matches) {
    const records = new Map();
    const groups = new Map();
    for (const m of matches) {
        const [a, b] = m.teams || [];
        if (!a?.teamId || !b?.teamId) return null;
        const ra = records.get(a.teamId) || [0, 0];
        const rb = records.get(b.teamId) || [0, 0];
        if (ra[0] !== rb[0] || ra[1] !== rb[1]) return null;
        const key = `${ra[0]}-${ra[1]}`;
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(m);
        if (m.state === 'completed') {
            const aWon = a.result?.outcome === 'win';
            const bWon = b.result?.outcome === 'win';
            if (aWon === bWon) return null;
            records.set(a.teamId, aWon ? [ra[0] + 1, ra[1]] : [ra[0], ra[1] + 1]);
            records.set(b.teamId, bWon ? [rb[0] + 1, rb[1]] : [rb[0], rb[1] + 1]);
        }
    }
    return [...groups.entries()].map(([key, items]) => ({ title: `${key} 매치`, items }));
}

function groupMatches(matches) {
    if (matches.some(m => (m.previousMatchIds || []).length > 0)) {
        return groupByPrevious(matches);
    }
    return (
        groupBySingleElimination(matches) ||
        groupBySwissRecord(matches) || [{ title: '경기', items: matches }]
    );
}

const STATE_LABEL = { completed: '종료', inProgress: '진행 중' };

const BracketView = ({ matches }) => {
    const rounds = useMemo(() => groupMatches(matches || []), [matches]);

    return (
        <div className="bracket-view">
            {rounds.map((round, ri) => (
                <div className="bracket-round" key={ri}>
                    <div className="bracket-round-title">{round.title}</div>
                    <ul className="stage-match-list">
                        {round.items.map((m, idx) => (
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
            ))}
        </div>
    );
};

export default BracketView;
