import React from 'react';
import {FaCalendarAlt, FaCalendarDay, FaChevronLeft, FaChevronRight} from 'react-icons/fa';
import format from 'date-fns/format';

const years = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - 5 + i);
const months = Array.from({ length: 12 }, (_, i) => i + 1);

const CustomToolbar = (toolbar) => {
    const goToBack = () => toolbar.onNavigate('PREV');
    const goToNext = () => toolbar.onNavigate('NEXT');
    const goToToday = () => toolbar.onNavigate('TODAY');
    const goToView = (view) => toolbar.onView(view);

    const handleYearChange = (e) => {
        const newDate = new Date(toolbar.date);
        newDate.setFullYear(parseInt(e.target.value));
        toolbar.onNavigate('DATE', newDate);
    };

    const handleMonthChange = (e) => {
        const newDate = new Date(toolbar.date);
        newDate.setMonth(parseInt(e.target.value) - 1); // 0-indexed
        toolbar.onNavigate('DATE', newDate);
    };

    return (
        <div className="custom-toolbar">
            <div className="label-area">
                <button onClick={goToBack}><FaChevronLeft /></button>
                <div className="date-selectors">
                    {toolbar.view === 'month' && (
                        <>
                            <select value={toolbar.date.getFullYear()} onChange={handleYearChange}>
                                {years.map((year) => (
                                    <option key={year} value={year}>{year}년</option>
                                ))}
                            </select>
                            <select value={toolbar.date.getMonth() + 1} onChange={handleMonthChange}>
                                {months.map((month) => (
                                    <option key={month} value={month}>{month}월</option>
                                ))}
                            </select>
                        </>
                    )}

                    {(toolbar.view === 'week' || toolbar.view === 'day') && (
                        <div>
                            <input
                                type="date"
                                value={format(toolbar.date, 'yyyy-MM-dd')}
                                onChange={(e) => toolbar.onNavigate('DATE', new Date(e.target.value))}
                            />
                        </div>
                    )}
                </div>
                <button onClick={goToNext}><FaChevronRight /></button>
            </div>
            <div className="toolbar-controls">
                <button title="오늘 날짜로 이동" onClick={goToToday}>Today</button>
                <button title="월간" onClick={() => goToView('month')}><FaCalendarAlt /></button>
                {/*<button title="주간" onClick={() => goToView('week')}><FaCalendarWeek /></button>*/}
                <button title="일간" onClick={() => goToView('day')}><FaCalendarDay /></button>
            </div>
        </div>
    );
};

export default CustomToolbar;