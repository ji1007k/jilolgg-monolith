"use client";

// src/context/CalendarContext.js
import React, {createContext, useContext, useEffect, useState} from 'react';

const CalendarContext = createContext({
    selectedLeague: null,   // 현재 선택된 리그
    selectedTeam: null,     // 현재 선택된 팀
    favoriteTeamIds: [],
    selectedDate: [],
    setSelectedLeague: () => {},
    setSelectedTeam: () => {},
    setFavoriteTeamIds: () => {},
    setSelectedDate: () => {}
});

export const useCalendar = () => useContext(CalendarContext);

export const CalandarProvider = ({ children }) => {
    const [selectedLeague, setSelectedLeague] = useState(null);
    const [selectedTeam, setSelectedTeam] = useState(null);
    const [favoriteTeamIds, setFavoriteTeamIds] = useState([]);
    const [selectedDate, setSelectedDate] = useState(new Date());

    useEffect(() => {
        // console.log('selectedTeam 변경됨:', selectedTeam);
    }, [selectedTeam]);

    useEffect(() => {
        // console.log('favoriteTeamIds 변경됨:', favoriteTeamIds);
    }, [favoriteTeamIds]);

    return (
        <CalendarContext.Provider value={{
            selectedLeague, setSelectedLeague,
            selectedTeam, setSelectedTeam,
            favoriteTeamIds, setFavoriteTeamIds,
            selectedDate, setSelectedDate
        }}>
            {children}
        </CalendarContext.Provider>
    );
};
