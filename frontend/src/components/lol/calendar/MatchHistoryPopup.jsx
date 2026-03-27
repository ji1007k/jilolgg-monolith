import Popup from "reactjs-popup";
import { FiX } from "react-icons/fi";
import format from "date-fns/format";
import ko from "date-fns/locale/ko";
import Loading from "@components/common/Loading.js";

function MatchHistoryPopup({ team, matches, open, onClose, isLoading }) {

    return (
        <Popup
            open={open}
            onClose={onClose}
            modal
            closeOnDocumentClick={false}
            contentStyle={{ padding: 0, border: "none", background: "none" }}
            overlayStyle={{ background: "rgba(0,0,0,0.3)" }}
        >
            {(close) => (
                <div className="match-history-popup">
                    {/* 상단 제목 */}
                    <div className="popup-header">
                        <span>{team.name} 전적</span>
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

                    {/* 본문 */}
                    {isLoading ? (
                        <div className="popup-body">
                            <Loading message={team.name + " 전적 로딩 중..."} />
                        </div>
                    ) : (
                        <div className="popup-body">
                            {matches.map((match, index) => {
                                const isLive = match.state === "inProgress";
                                const isCompleted = match.state === "completed";
                                const hasVod = match.flags?.includes("hasVod");
                                const [teamA, teamB] = match.teams;
                                const winner = match.teams.find(t => t.result?.outcome === "win");
                                const matchTime = format(new Date(match.startTime), "yyyy년 M월 d일 (EEE) aaa h:mm", { locale: ko });

                                return (
                                    <div
                                        key={match.matchId}
                                        className="match-card"
                                        style={{
                                            borderColor: winner?.code === team.code ? "#4ade80" : "#f87171",
                                        }}
                                    >
                                        {/* 상태 + 시간 + 영상 */}
                                        <div className="status-time-row">
                                            <div className="status-labels">
                                                <div className="label num-badge">{index + 1}</div>
                                                <span>{matchTime}</span>
                                            </div>
                                            {hasVod && (
                                                <div className="vod-btn-wrapper">
                                                    <button
                                                        onClick={() => window.open(`https://vod.example.com/${match.matchId}`, "_blank")}
                                                        className="vod-btn"
                                                    >
                                                        🎥 영상 보기
                                                    </button>
                                                </div>
                                            )}
                                        </div>

                                        {/* 팀 정보 */}
                                        <div className="teams-row">
                                            <div className="team team-left">
                                                <span className="team-code">{teamA.code}</span>
                                                <span className={`result ${winner?.slug === teamA.slug ? "win" : "lose"}`}>
                                                {winner?.slug === teamA.slug ? "승" : "패"}
                                            </span>
                                            </div>
                                            <div className="score">
                                                {teamA.result?.gameWins ?? 0} : {teamB.result?.gameWins ?? 0}
                                            </div>
                                            <div className="team team-right">
                                            <span className={`result ${winner?.slug === teamB.slug ? "win" : "lose"}`}>
                                                {winner?.slug === teamB.slug ? "승" : "패"}
                                            </span>
                                                <span className="team-code">{teamB.code}</span>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
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
}

export default MatchHistoryPopup;