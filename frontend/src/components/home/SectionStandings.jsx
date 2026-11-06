import {useEffect, useRef, useState} from "react";
import Standings from "@components/lol/standings/Standings";
import Loading from "@components/common/Loading";
import { apiFetchTournaments } from "@utils/api-lol";
import {useCalendar} from "@/context/CalendarContext.js";

// slug만으로는 구분이 어려워 기간(MM.DD~MM.DD)을 덧붙인다. 날짜가 없으면 slug만 쓴다.
const shortDate = (d) => (typeof d === 'string' && d.length >= 10 ? d.slice(5, 10).replace('-', '.') : '');

const tournamentLabel = (t) => {
    const s = shortDate(t.startDate);
    const e = shortDate(t.endDate);
    return s && e ? `${t.slug} (${s}~${e})` : t.slug;
};

export default function SectionStandings() {
    const { selectedLeague, selectedDate } = useCalendar();
    const [tournaments, setTournaments] = useState([]);
    const [activeTournamentId, setActiveTournamentId] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const prevLeagueRef = useRef(null);

    useEffect(() => {
        const fetchTournaments = async () => {
            if (!selectedLeague) return;

            // 리그 선택값에 변경사항이 없으면 패스
            if (prevLeagueRef.current === selectedLeague) return;

            prevLeagueRef.current = selectedLeague;

            setIsLoading(true);
            const response = await apiFetchTournaments(selectedLeague.id, selectedDate.getFullYear());
            setTournaments(response);
            if (response.length > 0) {
                const ongoing = response.find(t => t.active);
                setActiveTournamentId(ongoing ? ongoing.id : response[0].id);
            }
            setIsLoading(false);
        };
        fetchTournaments();
    }, [selectedLeague, selectedDate]);


    return isLoading ? (
        <Loading message="토너먼트 불러오는 중..." />
    ) : (
        <div className="tournament-container">
            <div className="tournament-select-wrapper">
                <span>Standings</span>
                <select
                    className="tournament-select"
                    value={activeTournamentId}
                    onChange={(e) => setActiveTournamentId(e.target.value)}
                >
                    {tournaments.map(t => (
                        <option key={t.id} value={t.id}>
                            {tournamentLabel(t)}
                        </option>
                    ))}
                </select>
            </div>
            <Standings tournamentId={activeTournamentId} />
        </div>
    );
}
