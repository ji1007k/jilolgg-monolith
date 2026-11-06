// 순위표(rankings)가 비어 있는 스테이지(스위스·플레이-인·단판 토너먼트)를 matches로 해석하는 유틸.
// 실제 getStandings 응답(worlds_2026)에서 previousMatchIds는 비어 있고 stage.type은 null이라,
// 경기 수와 응답 순서만으로 추정한다.

// 경기 수가 2^k-1이면 응답 순서대로 첫 라운드 2^(k-1)경기, 다음 절반… 마지막 1경기로 본다.
export function groupBySingleElimination(matches) {
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

// 응답 순서대로 승패를 누적했을 때 모든 경기가 "전적이 같은 팀끼리" 붙으면 스위스로 본다.
export function isSwissStage(matches) {
    const records = new Map();
    for (const m of matches) {
        const [a, b] = m.teams || [];
        if (!a?.teamId || !b?.teamId) return false;
        const ra = records.get(a.teamId) || [0, 0];
        const rb = records.get(b.teamId) || [0, 0];
        if (ra[0] !== rb[0] || ra[1] !== rb[1]) return false;
        if (m.state === 'completed') {
            const aWon = a.result?.outcome === 'win';
            const bWon = b.result?.outcome === 'win';
            if (aWon === bWon) return false;
            records.set(a.teamId, aWon ? [ra[0] + 1, ra[1]] : [ra[0], ra[1] + 1]);
            records.set(b.teamId, bWon ? [rb[0] + 1, rb[1]] : [rb[0], rb[1] + 1]);
        }
    }
    return true;
}

const SWISS_ADVANCE_WINS = 3;
const SWISS_ELIMINATE_LOSSES = 3;

// 종료된 경기에서 팀별 승·패·세트 득실을 집계해 순위 카드용 배열로 만든다.
// 정렬은 승 → 패 → 득실이며 공식 타이브레이커는 알 수 없어 동률은 공동 순위로 둔다.
export function buildRankingsFromMatches(matches, { swiss = false } = {}) {
    const byTeam = new Map();
    const touch = (t) => {
        if (!byTeam.has(t.teamId)) {
            byTeam.set(t.teamId, {
                teamId: t.teamId, slug: t.slug, name: t.name, code: t.code, image: t.image,
                record: { wins: 0, losses: 0, gameDiff: 0 },
            });
        }
        return byTeam.get(t.teamId);
    };

    for (const m of matches) {
        const teams = (m.teams || []).filter(t => t?.teamId);
        teams.forEach(touch); // 아직 경기를 안 치른 팀도 0-0으로 노출
        if (m.state !== 'completed' || teams.length !== 2) continue;
        const [a, b] = teams;
        const aWins = a.result?.gameWins ?? 0;
        const bWins = b.result?.gameWins ?? 0;
        const ra = byTeam.get(a.teamId).record;
        const rb = byTeam.get(b.teamId).record;
        ra.gameDiff += aWins - bWins;
        rb.gameDiff += bWins - aWins;
        if (a.result?.outcome === 'win') { ra.wins++; rb.losses++; }
        else if (b.result?.outcome === 'win') { rb.wins++; ra.losses++; }
    }

    const list = [...byTeam.values()].sort((x, y) =>
        y.record.wins - x.record.wins ||
        x.record.losses - y.record.losses ||
        y.record.gameDiff - x.record.gameDiff);

    return list.map((t, i) => {
        const prev = list[i - 1];
        const same = prev &&
            prev.record.wins === t.record.wins &&
            prev.record.losses === t.record.losses &&
            prev.record.gameDiff === t.record.gameDiff;
        t.rank = same ? prev.rank : i + 1;
        if (swiss) {
            if (t.record.wins >= SWISS_ADVANCE_WINS) t.badge = '진출';
            else if (t.record.losses >= SWISS_ELIMINATE_LOSSES) t.badge = '탈락';
        }
        return t;
    });
}

// 섹션의 표시용 순위 결정: 외부 rankings가 있으면 그대로, 없으면 경기에서 집계(토너먼트는 제외).
export function resolveSectionRankings(section) {
    if (section?.refinedRankings?.length > 0) return section.refinedRankings;
    const matches = section?.matches || [];
    if (matches.length === 0 || groupBySingleElimination(matches)) return [];
    return buildRankingsFromMatches(matches, { swiss: isSwissStage(matches) });
}
