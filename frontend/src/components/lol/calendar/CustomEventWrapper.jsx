import Popup from 'reactjs-popup';
import 'reactjs-popup/dist/index.css';
import format from 'date-fns/format';
import {FiX} from "react-icons/fi";

// TODO 
//  - CODE -> SLUG 또는 TEAM_ID 사용
/**
 * 일정 클릭 팝업 이벤트
 */
const CustomEventWrapper = ({ open, event, children }) => {

    return (
        <Popup
            trigger={
                <div onClick={(e) => e.stopPropagation()}>
                    {children}  {/*refineTeamSchedule > title*/}
                </div>
            }
            open={open}
            modal
            closeOnDocumentClick={false}
            contentStyle={{}}
        >
            {(close) => (
                <div className="match-detail-popup">
                    {/* 상단 제목 */}
                    <div className="popup-header">
                        <span>경기 일정 상세</span>
                        <button
                            onClick={() => {
                                close();
                            }}
                            className="close-btn"
                        >
                            <FiX />
                        </button>
                    </div>

                    {
                        (() => {
                            const isUnstarted = event.state === "unstarted";
                            const isLive = event.state === "inProgress";
                            const isCompleted = event.state === "completed";

                            const participants = [...event.participants];
                            if (participants.length === 1) participants.push({ ...participants[0] });
                            const [teamA, teamB] = participants;
                            const winner = !isCompleted ? null :
                                teamA.outcome === 'win' ? teamA : teamB;


                            return (
                                <div className="popup-body">
                                    <div key={event.matchId} className="match-card">
                                        <div className="status-time-row">
                                            <div className="status-labels">
                                                {isUnstarted && <span className="label unstarted">예정</span>}
                                                {isLive && <span className="label live">LIVE</span>}
                                                {isCompleted && <span className="label completed">종료</span>}
                                                <span>{format(new Date(event.startTime), 'HH:mm')}</span>
                                            </div>
                                        </div>

                                        {(isLive || isCompleted) && (
                                            <div className="flex justify-center">
                                                <div className="label strategy">
                                                    {`${event.blockName}(${event.strategy})`}
                                                </div>
                                            </div>
                                        )}

                                        {isUnstarted ? (
                                            <div className="teams-row">
                                                <div className="team team-left">
                                                    <span className="team-code">{teamA.team.code}</span>
                                                </div>
                                                <div className="label strategy">{`${event.blockName}(${event.strategy})`}</div>
                                                <div className="team team-right">
                                                    <span className="team-code">{teamB.team.code}</span>
                                                </div>
                                            </div>
                                        ) : (
                                            <div className="teams-row">
                                                <div className="team team-left">
                                                    <span className="team-code">{teamA.team.code}</span>
                                                    {isCompleted && (
                                                        <span className={`result ${winner?.team.slug === teamA.team.slug ? "win" : "lose"}`}>
                                                            {winner?.team.slug === teamA.team.slug ? "승" : "패"}
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="score">
                                                    {teamA.gameWins ?? 0} : {teamB.gameWins ?? 0}
                                                </div>
                                                <div className="team team-right">
                                                    {isCompleted && (
                                                        <span className={`result ${winner?.team.slug === teamB.team.slug ? "win" : "lose"}`}>
                                                            {winner?.team.slug === teamB.team.slug ? "승" : "패"}
                                                        </span>
                                                    )}
                                                    <span className="team-code">{teamB.team.code}</span>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })()
                    }

                    {/* 하단 닫기 */}
                    <div className="popup-footer">
                        <button
                            onClick={() => {
                                close();
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

export default CustomEventWrapper;