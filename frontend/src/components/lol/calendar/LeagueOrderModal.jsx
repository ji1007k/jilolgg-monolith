import { useState, useEffect } from 'react';
import { apiUpdateLeagueOrders } from '@/utils/api-lol';

const LeagueOrderModal = ({ isOpen, onClose, leagues, onUpdate }) => {
    const [orderedLeagues, setOrderedLeagues] = useState([]);

    useEffect(() => {
        if (isOpen) {
            setOrderedLeagues([...leagues]);
        }
    }, [isOpen, leagues]);

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

    const handleSave = async () => {
        try {
            // 백엔드 LeagueDto의 @JsonProperty("id")가 leagueId에 매핑되어 있으므로, 프론트에서는 id를 사용해야 함
            const leagueIds = orderedLeagues.map(l => l.id);
            await apiUpdateLeagueOrders(leagueIds);
            onUpdate(orderedLeagues); // 부모 컴포넌트에 변경된 목록 전달
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
                    <button onClick={onClose} className="close-btn">&times;</button>
                </div>
                
                <div className="modal-body">
                    <ul className="league-order-list">
                        {orderedLeagues.map((league, index) => (
                            <li key={league.id} className="league-order-item">
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img src={league.image} alt={league.name} className="league-icon" />
                                <span className="league-name">{league.name}</span>
                                <div className="order-controls">
                                    <button 
                                        onClick={() => moveUp(index)} 
                                        disabled={index === 0}
                                        className="control-btn"
                                    >
                                        ▲
                                    </button>
                                    <button 
                                        onClick={() => moveDown(index)} 
                                        disabled={index === orderedLeagues.length - 1}
                                        className="control-btn"
                                    >
                                        ▼
                                    </button>
                                </div>
                            </li>
                        ))}
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
