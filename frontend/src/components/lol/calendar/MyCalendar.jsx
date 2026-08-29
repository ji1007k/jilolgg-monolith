'use client';

import React, { useState } from 'react';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import '@/styles/tailwind/lol/calendar.css';
import '@/styles/css/lol-calendar.css';
import '@/styles/css/responsive.css';

import { addDays, addMonths, format, getDay, parse, startOfWeek, subDays, subMonths } from 'date-fns';
import ko from 'date-fns/locale/ko';
import { useSwipeable } from 'react-swipeable';

import CustomToolbar from '@components/lol/calendar/CustomToolbar';
import CustomEventWrapper from '@components/lol/calendar/CustomEventWrapper';
import CustomDayView from '@components/lol/calendar/CustomDayView';
import CustomWeekView from '@components/lol/calendar/CustomWeekView';
import { useCalendarLogic } from '@/components/lol/calendar/hooks/useCalendarLogic';
import { eventPropGetter } from '@/components/lol/calendar/utils/calendarEventStyles';
import { formats } from '@/components/lol/calendar/config/formats';
import LeagueAndTeamSelector from '@components/lol/calendar/LeagueAndTeamSelector';
import { useCalendar } from '@/context/CalendarContext.js';
import { getMatchesByLeagueIdAndDate } from '@utils/api-lol.js';
import { getDateRange } from '@utils/date-util.js';
import MatchListPopup from '@components/lol/calendar/MatchListPopup.jsx';

const localizer = dateFnsLocalizer({
    format,
    parse,
    startOfWeek: () => startOfWeek(new Date(), { weekStartsOn: 0 }),
    getDay,
    locales: { ko },
});

/**
 * 달력 한 벌. 데스크톱/모바일이 서로 다른 기본 뷰로 각각 렌더된다(CSS로 하나만 보임).
 * 뷰 상태를 자기가 들고 있어야 스와이프가 실제 화면과 맞는다.
 */
function CalendarPane({ defaultView, events, calendarProps, onShift, onViewChange }) {
    const [view, setView] = useState(defaultView);
    // onSelectDay는 Calendar가 아는 prop이 아니므로 분리해서 넘기지 않는다.
    const { onSelectDay, ...restProps } = calendarProps;

    const handlers = useSwipeable({
        onSwipedLeft: () => onShift(view, 1),
        onSwipedRight: () => onShift(view, -1),
        preventScrollOnSwipe: true,
        trackMouse: true,
    });

    const handleView = (nextView) => {
        setView(nextView);
        onViewChange(nextView);
    };

    return (
        <div className="calendar-wrapper" {...handlers}>
            <Calendar
                {...restProps}
                events={events}
                view={view}
                onView={handleView}
                onSelectSlot={(slotInfo) => {
                    if (!slotInfo?.start) return;
                    // 일자별 팝업은 월간 화면에서만 연다. 주간 화면은 목록 자체가 상세다.
                    if (view !== 'month') return;
                    onSelectDay(slotInfo.start);
                }}
            />
        </div>
    );
}

function MyCalendar({ events }) {
    const [isLoading, setIsLoading] = useState(true);
    const {
        leagues,
        visibleLeagues,
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
    } = useCalendarLogic();

    const { selectedLeague, selectedTeam, favoriteTeamIds, selectedDate, setSelectedDate } = useCalendar();

    function CalendarEvent({ event }) {
        const isInProgress = event.state === 'inProgress';
        return (
            <div className="flex items-center">
                {isInProgress && <span className="live-badge" />}
                <span>{event.title}</span>
            </div>
        );
    }

    /**
     * 스와이프로 이동할 거리는 "지금 보이는 뷰"에 따라 달라진다.
     * 데스크톱/모바일 달력이 currentView 상태를 공유하는데 모바일은 주간으로 시작하면서도
     * onView가 불리지 않아 currentView가 'month'로 남았고, 그 결과 주간 화면에서
     * 좌우로 밀면 한 달씩 건너뛰었다. 각 달력이 자기 뷰를 들고 그 값으로 계산하게 한다.
     */
    const shiftDate = (view, direction) => {
        setSelectedDate((prev) => {
            if (view === 'month') return direction > 0 ? addMonths(prev, 1) : subMonths(prev, 1);
            if (view === 'week') return direction > 0 ? addDays(prev, 7) : subDays(prev, 7);
            return direction > 0 ? addDays(prev, 1) : subDays(prev, 1);
        });
    };

    const fetchMatchesForDate = async (date) => {
        const { startDate, endDate } = getDateRange('day', date);
        setPopupDate(date);
        setIsLoading(true);
        setPopupOpen(true);

        try {
            const response = await getMatchesByLeagueIdAndDate(selectedLeague?.id, startDate, endDate);
            setPopupMatches(response || []);
        } catch (err) {
            // eslint-disable-next-line no-console
            console.error('Error fetching matches:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const handlePrevDate = async () => {
        const newDate = new Date(popupDate);
        newDate.setDate(newDate.getDate() - 1);
        await fetchMatchesForDate(newDate);
    };

    const handleNextDate = async () => {
        const newDate = new Date(popupDate);
        newDate.setDate(newDate.getDate() + 1);
        await fetchMatchesForDate(newDate);
    };

    // 데스크톱/모바일 달력이 공유하는 설정. 다른 건 기본 뷰뿐이다.
    const calendarProps = {
        localizer,
        formats,
        startAccessor: 'start',
        endAccessor: 'end',
        views: { month: true, week: CustomWeekView },
        date: selectedDate,
        onNavigate: setSelectedDate,
        eventPropGetter: (event) => eventPropGetter(event, selectedTeam, favoriteTeamIds),
        components: {
            toolbar: CustomToolbar,
            event: CalendarEvent,
            eventWrapper: CustomEventWrapper,
            month: {
                dateHeader: ({ date, label }) => (
                    <div style={{ color: date.getDay() === 0 ? 'red' : undefined }}>{label}</div>
                ),
            },
            header: ({ date, label }) => (
                <div style={{ color: date.getDay() === 0 ? 'red' : 'inherit' }}>{label}</div>
            ),
        },
        selectable: true,
        longPressThreshold: 100,
        onSelectDay: fetchMatchesForDate,
    };

    return (
        <div className="calendar-container">
            <div className="desktop-calendar">
                <CalendarPane
                    defaultView="month"
                    events={events || refinedSchedules}
                    calendarProps={calendarProps}
                    onShift={shiftDate}
                    onViewChange={setCurrentView}
                />
            </div>

            <div className="mobile-schedule-list">
                <CalendarPane
                    defaultView="week"
                    events={events || refinedSchedules}
                    calendarProps={calendarProps}
                    onShift={shiftDate}
                    onViewChange={setCurrentView}
                />
            </div>

            {popupOpen && (
                <MatchListPopup
                    open={popupOpen}
                    onClose={() => setPopupOpen(false)}
                    matches={popupMatches}
                    date={popupDate}
                    isLoading={isLoading}
                    onPrevDate={handlePrevDate}
                    onNextDate={handleNextDate}
                />
            )}

            <LeagueAndTeamSelector
                leagues={leagues}
                visibleLeagues={visibleLeagues}
                onUpdateLeagueSettings={updateLeagueSettings}
            />
        </div>
    );
}

export default MyCalendar;
