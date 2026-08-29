import { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import { getMatchesByLeagueIdAndDate } from '@utils/api-lol';
import {
    getFavoriteTeamIds,
    getLocalLeagueOrder,
    applyLeagueOrder,
    getHiddenLeagueIds,
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
    const [hiddenLeagueIds, setHiddenLeagueIds] = useState([]);
    const [popupOpen, setPopupOpen] = useState(false);
    const [popupMatches, setPopupMatches] = useState([]);
    const [popupDate, setPopupDate] = useState(selectedDate || new Date());

    // 리그 목록을 불러올 때 "선택된 리그가 없으면 첫 리그로 채운다"는 판단에만 최신 값이 필요하고,
    // selectedLeague 변경 자체로 목록을 다시 불러오고 싶지는 않아 ref로 최신 값만 참조한다.
    const selectedLeagueRef = useRef(selectedLeague);
    selectedLeagueRef.current = selectedLeague;

    useEffect(() => {
        const fetchLeagues = async () => {
            try {
                const res = await fetch('/api/lol/leagues');
                const data = await res.json();

                // 로그인 사용자는 서버가 이미 정렬해서 내려준다.
                // 비로그인 사용자는 이 브라우저에 저장된 순서를 여기서 적용한다.
                const ordered = applyLeagueOrder(data, getLocalLeagueOrder());
                // 숨김: 로그인 사용자는 서버가 내려준 hidden 필드에서, 비로그인 사용자는 이 브라우저에서 읽는다.
                const hidden = getHiddenLeagueIds(ordered);

                setLeagues(ordered);
                setHiddenLeagueIds(hidden);

                const visible = ordered.filter((league) => !hidden.includes(league.id));
                if (!selectedLeagueRef.current && visible.length > 0) {
                    setSelectedLeague(visible[0]);
                }
            } catch (e) {
                console.error('리그 로딩 실패', e);
            }
        };

        fetchLeagues();
        // userId가 바뀌면(로그인/로그아웃) 적용할 순서의 출처가 달라지므로 다시 불러온다.
    }, [userId, setSelectedLeague]);

    const visibleLeagues = useMemo(
        () => leagues.filter((league) => !hiddenLeagueIds.includes(league.id)),
        [leagues, hiddenLeagueIds]
    );

    /**
     * 리그 순서/숨김 설정 모달 저장 시 호출된다.
     * 선택 중인 리그가 방금 숨겨졌다면 보이는 리그가 하나도 없는 화면이 되므로
     * 보이는 리그 중 첫 번째로 대체한다.
     */
    const updateLeagueSettings = useCallback((newLeagues, newHiddenLeagueIds) => {
        setLeagues(newLeagues);
        setHiddenLeagueIds(newHiddenLeagueIds);

        const isSelectedNowHidden = selectedLeague && newHiddenLeagueIds.includes(selectedLeague.id);
        if (isSelectedNowHidden) {
            const nextVisible = newLeagues.find((league) => !newHiddenLeagueIds.includes(league.id));
            if (nextVisible) setSelectedLeague(nextVisible);
        }
    }, [selectedLeague, setSelectedLeague]);

    // 즐겨찾기는 리그/날짜와 무관하므로 별도 effect로 둔다.
    // (기존에는 일정 조회 안에 있어서 날짜를 넘길 때마다 같이 다시 불렀다)
    useEffect(() => {
        const loadFavorites = async () => {
            try {
                setFavoriteTeamIds(await getFavoriteTeamIds());
            } catch (e) {
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
        visibleLeagues,
        hiddenLeagueIds,
        updateLeagueSettings,
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
