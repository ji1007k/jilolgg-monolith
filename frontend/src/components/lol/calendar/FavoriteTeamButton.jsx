import { useEffect, useState } from 'react';
import { FaStar, FaRegStar } from 'react-icons/fa';
import { useCalendar } from "@/context/CalendarContext.js";
import { apiAddFavoriteTeam, apiRemoveFavoriteTeam } from "@utils/api-lol.js";
import { useAuth } from "@/context/AuthContext.js";  // userId가 있는 곳

const FavoriteTeamButton = ({ teamId, name, slug, image }) => {
    const { userId } = useAuth(); // userId 가져오기
    const { selectedTeam, setSelectedTeam, favoriteTeamIds, setFavoriteTeamIds } = useCalendar(); // ⬅️ context에서 함수 가져오기
    const [hovered, setHovered] = useState(false); // 버튼 개별 상태
    const [isFavorited, setIsFavorited] = useState(favoriteTeamIds.includes(teamId));

    useEffect(() => {
        setIsFavorited(favoriteTeamIds.includes(teamId));
    }, [favoriteTeamIds, teamId]);

    // 즐겨찾기 토글 핸들러
    const handleFavoriteToggle = async () => {
        if (!userId) {
            console.warn('사용자가 로그인하지 않았습니다.');
            return;  // 인증되지 않으면 즐겨찾기 기능을 수행하지 않음
        }

        const isAlreadyFavorited = favoriteTeamIds.includes(teamId);

        // csrf 토큰 발급
        await fetch('/api/csrf', { method: 'GET', credentials: 'include' });

        try {
            if (isAlreadyFavorited) {
                await apiRemoveFavoriteTeam(teamId); // 즐겨찾기 해제
            } else {
                await apiAddFavoriteTeam(teamId);    // 즐겨찾기 추가
            }

            setIsFavorited(!isFavorited);

            // UI 상태 업데이트
            setFavoriteTeamIds((prevFavorites) => {
                return isAlreadyFavorited
                    ? prevFavorites.filter((tId) => tId !== teamId)
                    : [teamId, ...prevFavorites];
            });
        } catch (error) {
            console.error("즐겨찾기 토글 실패:", error);
        }
    };

    // 팀 선택 버튼 (해당 팀 경기 일정 하이라이트)
    const handleTeamBtnClick = () => {
        setSelectedTeam({ teamId, name, slug }); // 선택한 팀 정보 저장
    };

    return (
        <div className="relative inline-block">
            {/* ⭐ 별 아이콘 버튼으로 만들어 클릭 가능하게 */}
            {userId && ( // 로그인한 사용자일 때만 즐겨찾기 버튼 표시
                <button
                    className="star-wrapper"
                    onClick={(e) => {
                        e.stopPropagation(); // 부모 버튼 클릭 방지
                        handleFavoriteToggle();  // 즐겨찾기 토글
                    }}
                    title={isFavorited ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                >
                    <i className={`star-icon ${isFavorited ? 'active' : 'inactive'} ${hovered ? 'hover' : ''}`}>
                        {isFavorited ? <FaStar /> : <FaRegStar />}
                    </i>
                </button>
            )}

            {/* ⭕ 동그란 팀 버튼 (로그인 여부와 관계없이 사용 가능) */}
            <button
                title={name}
                onClick={handleTeamBtnClick}
                onMouseEnter={() => setHovered(true)}
                onMouseLeave={() => setHovered(false)}
                className={`team-button ${isFavorited ? 'favorited' : ''} 
                    ${selectedTeam?.teamId === teamId ? 'selected' : ''} 
                    ${hovered ? 'hovered' : ''}`
                }
            >
                <img
                    src={image}
                    alt={name}
                    className="team-image"
                />
            </button>
        </div>
    );
};

export default FavoriteTeamButton;
