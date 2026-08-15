import { useEffect, useState } from 'react';
import { getMatchesByLeagueIdAndDate } from '@utils/api-lol';
import {
    getFavoriteTeamIds,
    getLocalLeagueOrder,
    applyLeagueOrder,
} from '@utils/userPreferences';
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

                // 로그인 사용자는 서버가 이미 정렬해서 내려준다.
                // 비로그인 사용자는 이 브라우저에 저장된 순서를 여기서 적용한다.
                const ordered = applyLeagueOrder(data, getLocalLeagueOrder());

                setLeagues(ordered);
                if (!selectedLeague && ordered.length > 0) {
                    setSelectedLeague(ordered[0]);
                }
            } catch (e) {
                // eslint-disable-next-line no-console
                console.error('리그 로딩 실패', e);
            }
        };

        fetchLeagues();
        // userId가 바뀌면(로그인/로그아웃) 적용할 순서의 출처가 달라지므로 다시 불러온다.
    }, [userId, setSelectedLeague]);

    // 즐겨찾기는 리그/날짜와 무관하므로 별도 effect로 둔다.
    // (기존에는 일정 조회 안에 있어서 날짜를 넘길 때마다 같이 다시 불렀다)
    useEffect(() => {
        const loadFavorites = async () => {
            try {
                setFavoriteTeamIds(await getFavoriteTeamIds());
            } catch (e) {
                // eslint-disable-next-line no-console
                console.error('즐겨찾기 로딩 실패', e);
            }
        };

        loadFavorites();
    }, [userId, setFavoriteTeamIds]);

    useEffect(() => {
        const fetchSchedule = async () => {
            if (!selectedLeague || !selectedDate) return;

            const { startDate, endDate } = getDateRange(currentView, selectedDate) || {};
            if (!startDate || !endDate) return;

            const leagueId = selectedLeague?.id || selectedLeague?.leagueId;
            if (!leagueId) return;

            const matches = await getMatchesByLeagueIdAndDate(leagueId, startDate, endDate);
            setRawSchedules(matches);
        };

        fetchSchedule();
    }, [selectedLeague, selectedDate, currentView]);

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
