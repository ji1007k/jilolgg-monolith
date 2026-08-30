import { useEffect, useState } from 'react';
import { FiStar } from 'react-icons/fi';
import { useCalendar } from "@/context/CalendarContext.js";
import { addFavoriteTeam, removeFavoriteTeam } from "@utils/userPreferences.js";

const FavoriteTeamButton = ({ teamId, name, slug, image }) => {
    // 로그인 여부는 신경 쓰지 않는다. userPreferences가 비로그인이면 localStorage로,
    // 로그인이면 서버로 알아서 보낸다.
    const { selectedTeam, setSelectedTeam, favoriteTeamIds, setFavoriteTeamIds } = useCalendar(); // ⬅️ context에서 함수 가져오기
    const [hovered, setHovered] = useState(false); // 버튼 개별 상태
    const [isFavorited, setIsFavorited] = useState(favoriteTeamIds.includes(teamId));

    useEffect(() => {
        setIsFavorited(favoriteTeamIds.includes(teamId));
    }, [favoriteTeamIds, teamId]);

    // 즐겨찾기 토글 핸들러
    const handleFavoriteToggle = async () => {
        const isAlreadyFavorited = favoriteTeamIds.includes(teamId);

        try {
            if (isAlreadyFavorited) {
                await removeFavoriteTeam(teamId); // 즐겨찾기 해제
            } else {
                await addFavoriteTeam(teamId);    // 즐겨찾기 추가
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
            {/* ⭐ 별 아이콘 버튼. 로그인 없이도 쓸 수 있다(비로그인은 이 브라우저에만 저장) */}
            <button
                className="star-wrapper"
                onClick={(e) => {
                    e.stopPropagation(); // 부모 버튼 클릭 방지
                    handleFavoriteToggle();  // 즐겨찾기 토글
                }}
                title={isFavorited ? '즐겨찾기 해제' : '즐겨찾기 추가'}
                aria-label={isFavorited ? '즐겨찾기 해제' : '즐겨찾기 추가'}
            >
                <i className={`star-icon ${isFavorited ? 'active' : 'inactive'} ${hovered ? 'hover' : ''}`}>
                    <FiStar fill={isFavorited ? 'currentColor' : 'none'} />
                </i>
            </button>

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
