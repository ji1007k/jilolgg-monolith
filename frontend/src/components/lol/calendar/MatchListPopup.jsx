import Popup from 'reactjs-popup';
import format from 'date-fns/format';
import 'reactjs-popup/dist/index.css';
import { FiX } from 'react-icons/fi';
import ko from "date-fns/locale/ko";
import Loading from "@components/common/Loading.js";
import {useSwipeable} from "react-swipeable";

function MatchListPopup ({ open, onClose, matches, date, isLoading, onPrevDate, onNextDate }) {
    const matchDate = format(new Date(date), "yyyy년 M월 d일 (EEE)", { locale: ko });

    const handlers = useSwipeable({
        onSwipedLeft: () => onNextDate(),  // 오른쪽에서 왼쪽으로 스와이프 → 다음 날짜
        onSwipedRight: () => onPrevDate(), // 왼쪽에서 오른쪽으로 스와이프 → 이전 날짜
        preventScrollOnSwipe: true, // 스와이프가 인식되었을 때만 preventDefault()를 호출해서 브라우저 스크롤을 막음
        trackMouse: true, // 데스크탑에서도 마우스로 테스트 가능
    });

    return (
        <Popup
            open={open}
            onClose={onClose}
            modal
            closeOnDocumentClick={false}
            contentStyle={{}} // contentStyle 비워두고 className으로만 조절
        >
            {(close) => (
                <div className="match-list-popup" {...handlers}>
                    {/* 상단 제목 */}
                    <div className="popup-header">
                        <span>{matchDate} 경기 일정</span>
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

                    {isLoading ? (
                        <div className="popup-body">
                            <Loading message="경기 일정 로딩 중..." />
                        </div>
                    ) : (
                        <div className="popup-body">
                            {matches.length === 0 ? (
                                <p>해당 날짜에 경기가 없습니다.</p>
                            ) : (
                                matches.map((match) => {
                                    const isUnstarted = match.state === "unstarted";
                                    const isLive = match.state === "inProgress";
                                    const isCompleted = match.state === "completed";

                                    const participants = [...match.participants];
                                    if (participants.length === 1) participants.push({ ...participants[0] });
                                    const [teamA, teamB] = participants;
                                    const winner = !isCompleted ? null :
                                        teamA.outcome === 'win' ? teamA : teamB;


                                    return (
                                        <div key={match.matchId} className="match-card">
                                            <div className="status-time-row">
                                                <div className="status-labels">
                                                    {isUnstarted && <span className="label unstarted">예정</span>}
                                                    {isLive && <span className="label live">LIVE</span>}
                                                    {isCompleted && <span className="label completed">종료</span>}
                                                    <span>{format(new Date(match.startTime), 'HH:mm')}</span>
                                                </div>
                                            </div>

                                            {(isLive || isCompleted)  && (
                                                <div className="flex justify-center">
                                                    <div className="label strategy">
                                                        {`${match.blockName}(${match.strategy})`}
                                                    </div>
                                                </div>
                                            )}

                                            {isUnstarted ? (
                                                <div className="teams-row">
                                                    <div className="team team-left">
                                                        <span className="team-code">{teamA.team.code}</span>
                                                    </div>
                                                    <div className="label strategy">
                                                        {`${match.blockName}(${match.strategy})`}
                                                    </div>
                                                    <div className="team team-right">
                                                        <span className="team-code">{teamB.team.code}</span>
                                                    </div>
                                                </div>
                                            ) : (
                                                <div className="teams-row">
                                                    <div className="team team-left">
                                                        <span className="team-code">{teamA.team.code}</span>
                                                        {isCompleted &&
                                                            (<span className={`result ${winner?.team.slug === teamA.team.slug ? "win" : "lose"}`}>
                                                                {winner?.team.slug === teamA.team.slug ? "승" : "패"}
                                                            </span>)
                                                        }
                                                    </div>
                                                    <div className="score">
                                                        {teamA.gameWins ?? 0} : {teamB.gameWins ?? 0}
                                                    </div>
                                                    <div className="team team-right">
                                                        {isCompleted &&
                                                            (<span className={`result ${winner?.team.slug === teamB.team.slug ? "win" : "lose"}`}>
                                                                {winner?.team.slug === teamB.team.slug ? "승" : "패"}
                                                            </span>)
                                                        }
                                                        <span className="team-code">{teamB.team.code}</span>
                                                    </div>
                                                </div>
                                            )
                                            }
                                        </div>
                                    )
                                })
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
};

export default MatchListPopup;