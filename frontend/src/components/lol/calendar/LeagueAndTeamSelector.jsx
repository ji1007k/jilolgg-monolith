import {useEffect, useState} from "react";
import {useCalendar} from "@/context/CalendarContext.js";
import {useAuth} from "@/context/AuthContext.js";
import FavoriteTeamButton from "@components/lol/calendar/FavoriteTeamButton.jsx";
import LeagueDropdown from "@components/lol/calendar/LeagueDropdown.jsx";
import LeagueOrderModal from "@components/lol/calendar/LeagueOrderModal.jsx";

const LeagueAndTeamSelector = ({ leagues, setLeagues }) => {
    const [rawTeams, setRawTeams] = useState([]); // 👈 fetch 결과만 보관
    const [teams, setTeams] = useState([]);       // 👈 정렬된 최종 데이터
    const [isOrderModalOpen, setIsOrderModalOpen] = useState(false);

    const {selectedLeague, setSelectedLeague, favoriteTeamIds} = useCalendar();
    const {userId} = useAuth();


    // 리그 변경 시 팀 fetch
    useEffect(() => {
        const fetchTeams = async () => {
            try {
                if (!selectedLeague) return;
                const res = await fetch(`/api/lol/teams?leagueId=${selectedLeague.id}`);
                const data = await res.json();
                setRawTeams(data); // fetch만 담당
            } catch (e) {
                console.error("팀 정보를 불러오는 데 실패했습니다.", e);
            }
        };

        if (selectedLeague) {
            fetchTeams();
        }
    }, [selectedLeague]);

    // 🎯 즐겨찾기 or rawTeams 변경 시 정렬
    useEffect(() => {
        if (!rawTeams.length) return;

        const sortedData = [...rawTeams].sort((a, b) => {
            const aIndex = favoriteTeamIds.indexOf(a.teamId);
            const bIndex = favoriteTeamIds.indexOf(b.teamId);

            if (aIndex !== -1 && bIndex !== -1) return aIndex - bIndex;
            if (aIndex !== -1) return -1;
            if (bIndex !== -1) return 1;
            return 0;
        });

        setTeams(sortedData);
    }, [favoriteTeamIds, rawTeams]);

    // ✅ 즐겨찾기 팀 분리
    const favoriteTeams = teams.filter(team =>
        favoriteTeamIds.includes(team.teamId)
    );
    const nonFavoriteTeams = teams.filter(
        team => !favoriteTeamIds.includes(team.teamId)
    );

    const handleLeagueUpdate = (newLeagues) => {
        if (setLeagues) {
            setLeagues(newLeagues);
        }
    };

    return (
        <div className="team-selector-wrapper">
            <div className="league-selector-container" style={{ display: 'flex', alignItems: 'center' }}>
                <LeagueDropdown
                    leagues={leagues}
                    selectedLeague={selectedLeague}
                    onChange={setSelectedLeague}
                    onOpenSettings={userId ? () => setIsOrderModalOpen(true) : null} // 👈 설정 버튼 핸들러 전달
                />
            </div>

            {userId && (
                <div
                    className={`favorite-teams-section scroll-hidden${favoriteTeams.length === 0 ? ' empty' : ''}`}
                >
                    <div className="team-btn-container">
                        {favoriteTeams.map(team => (
                            <FavoriteTeamButton key={team.teamId} {...team} />
                        ))}
                    </div>
                </div>
            )}

            <div
                className={`nonfavorite-teams-section scroll-hidden${nonFavoriteTeams.length === 0 ? ' empty' : ''}`}
            >
                <div className="team-btn-container">
                    {nonFavoriteTeams.map(team => (
                        <FavoriteTeamButton key={team.teamId} {...team} />
                    ))}
                </div>
            </div>

            <LeagueOrderModal
                isOpen={isOrderModalOpen}
                onClose={() => setIsOrderModalOpen(false)}
                leagues={leagues}
                onUpdate={handleLeagueUpdate}
            />
        </div>
    );
};

export default LeagueAndTeamSelector;
