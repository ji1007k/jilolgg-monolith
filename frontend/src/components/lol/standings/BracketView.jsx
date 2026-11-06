import React, { useMemo } from 'react';
import { groupBySingleElimination } from '@utils/standings-util.js';

// 단판 토너먼트(8강 → 4강 → 결승) 전용 트리.
// 순위표로 표현되는 스위스·플레이-인은 Standings가 경기에서 집계한 순위 카드로 보여준다.
// 팀이 둘 다 미정(TBD)인 경기는 흐리게 표시한다.

const isTbd = (t) => !t?.teamId || t.code === 'TBD';

const BracketView = ({ matches }) => {
    const rounds = useMemo(() => groupBySingleElimination(matches || []), [matches]);
    if (!rounds) return null;

    return (
        <div className="bracket-view">
            {rounds.map((round, ri) => (
                <div className="bracket-round" key={ri}>
                    <div className="bracket-round-title">{round.title}</div>
                    <div className="bracket-round-matches">
                        {round.items.map((m, idx) => {
                            const teams = m.teams || [];
                            const pending = teams.every(isTbd);
                            return (
                                <div
                                    key={m.matchId || idx}
                                    className={`bracket-match${pending ? ' pending' : ''}`}
                                >
                                    {teams.map((t, i) => (
                                        <div
                                            key={i}
                                            className={`bracket-team${t.result?.outcome === 'win' ? ' winner' : ''}`}
                                        >
                                            <span>{t.code || t.name || 'TBD'}</span>
                                            <span>{m.state === 'unstarted' ? '' : (t.result?.gameWins ?? 0)}</span>
                                        </div>
                                    ))}
                                </div>
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default BracketView;
