import { useEffect, useState } from 'react';
import { fetchFavoriteTeam, getMatchesByLeagueIdAndDate } from '@utils/api-lol';
import { useAuth } from '@/context/AuthContext';
import { useCalendar } from '@/context/CalendarContext.js';
import { refineTeamSchedule } from '@/components/lol/calendar/utils/refineTeamSchedule';
import { getDateRange } from '@utils/date-util.js';

export const useCalendarLogic = () => {
    const { userId } = useAuth();
    const {
        selectedLeague,
        setSelectedLeague,
        selectedTeam,
        favoriteTeamIds,
        setFavoriteTeamIds,
        selectedDate,
    } = useCalendar();

    const [currentView, setCurrentView] = useState('month');
    const [rawSchedules, setRawSchedules] = useState([]);
    const [refinedSchedules, setRefinedSchedules] = useState([]);
    const [leagues, setLeagues] = useState([]);
    const [popupOpen, setPopupOpen] = useState(false);
    const [popupMatches, setPopupMatches] = useState([]);
    const [popupDate, setPopupDate] = useState(selectedDate || new Date());

    useEffect(() => {
        const fetchLeagues = async () => {
            try {
                const res = await fetch('/api/lol/leagues');
                const data = await res.json();
                setLeagues(data);
                if (!selectedLeague && data.length > 0) {
                    setSelectedLeague(data[0]);
                }
            } catch (e) {
                // eslint-disable-next-line no-console
                console.error('리그 로딩 실패', e);
            }
        };

        fetchLeagues();
    }, [setSelectedLeague]);

    useEffect(() => {
        const fetchSchedule = async () => {
            if (!selectedLeague || !selectedDate) return;

            const { startDate, endDate } = getDateRange(currentView, selectedDate) || {};
            if (!startDate || !endDate) return;

            const leagueId = selectedLeague?.id || selectedLeague?.leagueId;
            if (!leagueId) return;

            if (userId) {
                const favorites = await fetchFavoriteTeam();
                setFavoriteTeamIds(favorites.map((team) => team.teamId));
            }

            const matches = await getMatchesByLeagueIdAndDate(leagueId, startDate, endDate);
            setRawSchedules(matches);
        };

        fetchSchedule();
    }, [userId, selectedLeague, selectedDate, currentView, setFavoriteTeamIds]);

    useEffect(() => {
        setRefinedSchedules(refineTeamSchedule(rawSchedules, currentView));
    }, [rawSchedules, currentView, selectedTeam]);

    return {
        leagues,
        setLeagues,
        currentView,
        setCurrentView,
        refinedSchedules,
        popupMatches,
        setPopupMatches,
        popupOpen,
        setPopupOpen,
        popupDate,
        setPopupDate,
        favoriteTeamIds,
    };
};
