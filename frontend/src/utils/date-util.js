export function getDateRange(view, date) {
    let year, month, day;
    if (view === 'month') {
        year = date.getFullYear();
        // month = date.getMonth() + 1;
    } else if (view === 'week') {
        const base = new Date(date);
        const dayOfWeek = base.getDay(); // 0=Sun
        const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
        const monday = new Date(base);
        monday.setDate(base.getDate() + diffToMonday);

        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);

        const formatDate = (d) => {
            const y = d.getFullYear();
            const m = String(d.getMonth() + 1).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${dd}`;
        };

        return {
            startDate: formatDate(monday),
            endDate: formatDate(sunday),
        };
    } else {
        year = date.getFullYear();
        month = date.getMonth() + 1;
        day = date.getDate();
    }

    if (!year) return null; // 연도 없으면 검색 안 함 or 기본값 처리

    let startDate, endDate;

    if (year && !month) {
        // 연도만
        startDate = `${year}-01-01`;
        endDate = `${year}-12-31`;
    } else if (year && month && !day) {
        // 연도 + 월
        const paddedMonth = String(month).padStart(2, '0');
        const lastDay = new Date(year, month, 0).getDate();
        startDate = `${year}-${paddedMonth}-01`;
        endDate = `${year}-${paddedMonth}-${String(lastDay).padStart(2, '0')}`;
    } else if (year && month && day) {
        // 연도 + 월 + 일
        const paddedMonth = String(month).padStart(2, '0');
        const paddedDay = String(day).padStart(2, '0');
        startDate = `${year}-${paddedMonth}-${paddedDay}`;
        endDate = startDate;
    } else {
        return null; // 조건이 불완전하면 null 리턴
    }

    return { startDate, endDate };
}
