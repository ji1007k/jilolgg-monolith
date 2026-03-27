import {useEffect, useRef, useState} from 'react';
import {fetchFavoriteTeam, getMatchesByLeagueIdAndDate} from '@utils/api-lol';
import {useAuth} from '@/context/AuthContext';
import {useCalendar} from '@/context/CalendarContext.js';
import {refineTeamSchedule} from '@/components/lol/calendar/utils/refineTeamSchedule';
import {getDateRange} from "@utils/date-util.js";

/**
 * [훅(Hook)] 소문자 시작 (use prefix)
 */
export const useCalendarLogic = () => {
    const { userId } = useAuth();
    const {
        selectedLeague, setSelectedLeague,
        selectedTeam,
        favoriteTeamIds, setFavoriteTeamIds,
        selectedDate, setSelectedDate
    } = useCalendar();

    const [currentView, setCurrentView] = useState('month');
    const [rawSchedules, setRawSchedules] = useState([]);
    const [refinedSchedules, setRefinedSchedules] = useState([]);
    const [leagues, setLeagues] = useState([]);
    const [popupOpen, setPopupOpen] = useState(false);
    const [popupMatches, setPopupMatches] = useState([]);
    const [popupDate, setPopupDate] = useState(selectedDate || new Date());
    const prevYearRef = useRef(null);
    const prevLeagueRef = useRef(null);

    // 리그 불러오기
    useEffect(() => {
        const fetchLeagues = async () => {
            try {
                const res = await fetch(`/api/lol/leagues`);
                const data = await res.json();
                setLeagues(data);
                if (!selectedLeague && data.length > 0) {
                    setSelectedLeague(data[0]);
                }
            } catch (e) {
                console.error('리그 로딩 실패', e);
            }
        };
        fetchLeagues();
    }, []);

    // 일정 불러오기
    useEffect(() => {
        const fetchSchedule = async () => {
            if (!selectedLeague || !selectedDate) return;

            const year = selectedDate.getFullYear();

            // 연도 또는 리그 선택값에 변경사항이 없는 경우 패스
            if (prevYearRef.current === year && prevLeagueRef.current === selectedLeague) return;

            // 연도 변경 → 새로 불러오기
            prevYearRef.current = year;
            prevLeagueRef.current = selectedLeague;

            if (userId) {
                const data = await fetchFavoriteTeam();
                setFavoriteTeamIds(data.map((team) => team.teamId));
            }

            const { startDate, endDate } = getDateRange(currentView, selectedDate);
            if (startDate && endDate) {
                const matches = await getMatchesByLeagueIdAndDate(selectedLeague?.id, startDate, endDate);
                setRawSchedules(matches);
            } else {
                // 검색 조건 불충분 처리
            }
        };
        fetchSchedule();
    }, [userId, selectedLeague, selectedDate, currentView]);

    useEffect(() => {
        setRefinedSchedules(refineTeamSchedule(rawSchedules, currentView));
    }, [rawSchedules, currentView, selectedTeam]);

    return {
        leagues,
        setLeagues, // 👈 추가됨
        currentView,
        setCurrentView,
        refinedSchedules,
        popupMatches,
        setPopupMatches,
        popupOpen,
        setPopupOpen,
        popupDate,
        setPopupDate
    };
};
