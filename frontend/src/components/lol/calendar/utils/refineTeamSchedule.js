/**
 * [유틸 함수] 소문자 시작
 * @param schedules
 * @param view
 * @returns {*|*[]}
 */
export const refineTeamSchedule = (schedules, view) => {
    if (!schedules) return [];

    return view === 'month'
        ? schedules
            .filter(s => s.participants.length > 0)
            .map(s => {
                const participants = [...s.participants];
                if (participants.length === 1) participants.push({ ...participants[0] });
                return {
                    ...s,
                    title: [participants[0].team.code, participants[1].team.code].join(' vs '),
                    start: new Date(s.startTime),
                    end: new Date(s.startTime),
                    allDay: true,
                };
            })
        : schedules
            .filter(s => s.participants.length > 0)
            .flatMap(s => {
                const participants = [...s.participants];
                if (participants.length === 1) participants.push({ ...participants[0] });
                const startTime = new Date(s.startTime);
                const event = {
                    ...s,
                    title: [participants[0].team.code, participants[1].team.code].join(' vs '),
                    start: startTime,
                    end: new Date(startTime.getTime() + 60 * 60 * 1000),
                };
                return [ { ...event, allDay: true }, { ...event, allDay: false } ];
            });
};
