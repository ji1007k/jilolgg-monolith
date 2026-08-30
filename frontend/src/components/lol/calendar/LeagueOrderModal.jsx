import { useState, useEffect } from 'react';
import { FiX, FiEye, FiEyeOff, FiChevronUp, FiChevronDown } from 'react-icons/fi';
import { saveLeagueSettings } from '@/utils/userPreferences';

const LeagueOrderModal = ({ isOpen, onClose, leagues, hiddenLeagueIds: initialHiddenLeagueIds, onUpdate }) => {
    const [orderedLeagues, setOrderedLeagues] = useState([]);
    const [hiddenLeagueIds, setHiddenLeagueIds] = useState([]);

    useEffect(() => {
        if (isOpen) {
            setOrderedLeagues([...leagues]);
            setHiddenLeagueIds(initialHiddenLeagueIds || []);
        }
    }, [isOpen, leagues, initialHiddenLeagueIds]);

    const moveUp = (index) => {
        if (index === 0) return;
        const newList = [...orderedLeagues];
        [newList[index - 1], newList[index]] = [newList[index], newList[index - 1]];
        setOrderedLeagues(newList);
    };

    const moveDown = (index) => {
        if (index === orderedLeagues.length - 1) return;
        const newList = [...orderedLeagues];
        [newList[index + 1], newList[index]] = [newList[index], newList[index + 1]];
        setOrderedLeagues(newList);
    };

    const toggleHidden = (leagueId) => {
        setHiddenLeagueIds((prev) =>
            prev.includes(leagueId)
                ? prev.filter((id) => id !== leagueId)
                : [...prev, leagueId]
        );
    };

    const handleSave = async () => {
        try {
            // 백엔드 LeagueDto의 @JsonProperty("id")가 leagueId에 매핑되어 있으므로, 프론트에서는 id를 사용해야 함
            const leagueIds = orderedLeagues.map(l => l.id);
            // 비로그인이면 이 브라우저에만, 로그인이면 서버(계정)에 저장되어 기기 간 동기화된다.
            await saveLeagueSettings(leagueIds, hiddenLeagueIds);
            onUpdate(orderedLeagues, hiddenLeagueIds); // 부모 컴포넌트에 변경된 목록/숨김 상태 전달
            onClose();
        } catch (error) {
            console.error("리그 순서 저장 실패:", error);
            alert("리그 순서 저장에 실패했습니다.");
        }
    };

    if (!isOpen) return null;

    return (
        <>
            <div className="modal-overlay" onClick={onClose}></div>
            <div className="league-order-modal">
                <div className="modal-header">
                    <h3>리그 순서 설정</h3>
                    <button onClick={onClose} className="close-btn" aria-label="닫기"><FiX /></button>
                </div>
                
                <div className="modal-body">
                    <ul className="league-order-list">
                        {orderedLeagues.map((league, index) => {
                            const isHidden = hiddenLeagueIds.includes(league.id);
                            return (
                                <li
                                    key={league.id}
                                    className={`league-order-item${isHidden ? ' league-hidden' : ''}`}
                                >
                                    {/* eslint-disable-next-line @next/next/no-img-element */}
                                    <img src={league.image} alt={league.name} className="league-icon" />
                                    <span className="league-name">{league.name}</span>
                                    <div className="order-controls">
                                        <button
                                            onClick={() => toggleHidden(league.id)}
                                            className={`control-btn hide-toggle-btn${isHidden ? ' is-hidden' : ''}`}
                                            title={isHidden ? '리그 표시하기' : '리그 숨기기'}
                                            aria-label={isHidden ? '리그 표시하기' : '리그 숨기기'}
                                        >
                                            {isHidden ? <FiEyeOff /> : <FiEye />}
                                        </button>
                                        <button
                                            onClick={() => moveUp(index)}
                                            disabled={index === 0}
                                            className="control-btn"
                                            aria-label="위로 이동"
                                        >
                                            <FiChevronUp />
                                        </button>
                                        <button
                                            onClick={() => moveDown(index)}
                                            disabled={index === orderedLeagues.length - 1}
                                            className="control-btn"
                                            aria-label="아래로 이동"
                                        >
                                            <FiChevronDown />
                                        </button>
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                </div>

                <div className="modal-footer">
                    <button onClick={onClose} className="cancel-btn">취소</button>
                    <button onClick={handleSave} className="save-btn">저장</button>
                </div>
            </div>
        </>
    );
};

export default LeagueOrderModal;
