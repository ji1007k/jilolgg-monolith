'use client';

import React, { useState } from 'react';
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import '@/styles/tailwind/lol/calendar.css';
import '@/styles/css/lol-calendar.css';

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

function MyCalendar({ events }) {
    const [isLoading, setIsLoading] = useState(true);
    const {
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

    const handlers = useSwipeable({
        onSwipedLeft: () => {
            if (currentView === 'month') setSelectedDate((prev) => addMonths(prev, 1));
            else if (currentView === 'week') setSelectedDate((prev) => addDays(prev, 7));
            else if (currentView === 'day') setSelectedDate((prev) => addDays(prev, 1));
        },
        onSwipedRight: () => {
            if (currentView === 'month') setSelectedDate((prev) => subMonths(prev, 1));
            else if (currentView === 'week') setSelectedDate((prev) => subDays(prev, 7));
            else if (currentView === 'day') setSelectedDate((prev) => subDays(prev, 1));
        },
        preventScrollOnSwipe: true,
        trackMouse: true,
    });

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

    return (
        <div className="calendar-container">
            <div className="calendar-wrapper" {...handlers}>
                <Calendar
                    localizer={localizer}
                    formats={formats}
                    events={events || refinedSchedules}
                    startAccessor="start"
                    endAccessor="end"
                    defaultView="month"
                    views={{
                        month: true,
                        week: CustomWeekView,
                        day: CustomDayView,
                    }}
                    date={selectedDate}
                    onNavigate={setSelectedDate}
                    onView={setCurrentView}
                    eventPropGetter={(event) => eventPropGetter(event, selectedTeam, favoriteTeamIds)}
                    components={{
                        toolbar: CustomToolbar,
                        event: CalendarEvent,
                        eventWrapper: CustomEventWrapper,
                        month: {
                            dateHeader: ({ date, label }) => (
                                <div style={{ color: date.getDay() === 0 ? 'red' : undefined }}>
                                    {label}
                                </div>
                            ),
                        },
                        header: ({ date, label }) => (
                            <div style={{ color: date.getDay() === 0 ? 'red' : 'inherit' }}>
                                {label}
                            </div>
                        ),
                    }}
                    selectable
                    longPressThreshold={100}
                    onSelectSlot={async (slotInfo) => {
                        if (!slotInfo?.start) return;
                        if (currentView !== 'month') return;
                        fetchMatchesForDate(slotInfo.start);
                    }}
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

            <LeagueAndTeamSelector leagues={leagues} setLeagues={setLeagues} />
        </div>
    );
}

export default MyCalendar;
