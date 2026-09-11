import Popup from "reactjs-popup";
import { FiX } from "react-icons/fi";
import Loading from "@components/common/Loading.js";

const ROLE_LABELS = {
    top: "탑",
    jungle: "정글",
    mid: "미드",
    bottom: "원딜",
    support: "서포터",
};

function TeamRosterPopup({ team, players, open, onClose, isLoading }) {

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
                <div className="team-roster-popup">
                    {/* 상단 제목 */}
                    <div className="popup-header">
                        <span>{team.name} 로스터</span>
                        <button
                            onClick={() => {
                                close();
                                onClose();
                            }}
                            className="close-btn"
                            aria-label="닫기"
                        >
                            <FiX />
                        </button>
                    </div>

                    {/* 본문 */}
                    {isLoading ? (
                        <div className="popup-body">
                            <Loading message={team.name + " 로스터 로딩 중..."} />
                        </div>
                    ) : (
                        <div className="popup-body">
                            {players.length === 0 ? (
                                <div className="no-roster">등록된 선수 정보가 없습니다.</div>
                            ) : (
                                players.map((player) => (
                                    <div key={player.playerId} className="player-card">
                                        <img
                                            src={player.image}
                                            alt={player.name}
                                            className="player-image"
                                        />
                                        <div className="player-info">
                                            <span className="player-name">{player.name}</span>
                                            <span className="player-role">
                                                {ROLE_LABELS[player.role] ?? player.role}
                                            </span>
                                        </div>
                                    </div>
                                ))
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
}

export default TeamRosterPopup;
