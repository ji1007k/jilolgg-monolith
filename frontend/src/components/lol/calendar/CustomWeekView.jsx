// TODO 커스텀 주간 뷰
const days = ['월', '화', '수', '목', '금', '토', '일'];

const CustomVerticalWeekView = ({ events, date }) => {
    const weekStart = startOfWeek(date, { weekStartsOn: 1 }); // 월요일 시작

    return (
        <div style={{ display: 'flex', flexDirection: 'column' }}>
            {days.map((day, index) => {
                const currentDate = addDays(weekStart, index);
                const dayEvents = events.filter(event =>
                    isSameDay(new Date(event.start), currentDate)
                );

                return (
                    <div key={index} style={{ borderBottom: '1px solid #ccc', padding: '10px' }}>
                        <strong>{day} ({format(currentDate, 'MM/dd', { locale: ko })})</strong>
                        <ul>
                            {dayEvents.map(event => (
                                <li key={event.matchId}>{event.title}</li>
                            ))}
                        </ul>
                    </div>
                );
            })}
        </div>
    );
};

CustomVerticalWeekView.title = () => '주간 (세로)';