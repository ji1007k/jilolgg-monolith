// 세트별 스코어보드. winnerTeamId가 null인 세트는 미확정으로 표시한다.
function GameScoreboard({ games, teamA, teamB }) {
    if (!games || games.length === 0) return null;

    const sorted = [...games].sort((a, b) => a.number - b.number);
    const codeOf = (id) => (id === teamA.teamId ? teamA.code : id === teamB.teamId ? teamB.code : null);

    return (
        <div className="game-scoreboard">
            {sorted.map((g) => {
                const winnerCode = codeOf(g.winnerTeamId);
                const blue = codeOf(g.blueTeamId);
                const red = codeOf(g.redTeamId);
                return (
                    <div key={g.number} className="game-row">
                        <span className="game-number">{g.number}세트</span>
                        <span className="game-sides">
                            {blue && red ? blue + "(블루) vs " + red + "(레드)" : ""}
                        </span>
                        {winnerCode ? (
                            <span className="game-winner">{winnerCode} 승</span>
                        ) : (
                            <span className="game-unconfirmed">미확정</span>
                        )}
                    </div>
                );
            })}
        </div>
    );
}

export default GameScoreboard;
