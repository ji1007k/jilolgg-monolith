import { addDays, endOfWeek, format, startOfWeek } from "date-fns";
import ko from "date-fns/locale/ko";
import { ScheduleWeekListView } from "@/components/lol/calendar/ScheduleListView";

function CustomWeekView(props) {
    return <ScheduleWeekListView {...props} />;
}

CustomWeekView.range = (date) => {
    const start = startOfWeek(date, { weekStartsOn: 1 });
    return Array.from({ length: 7 }, (_, i) => addDays(start, i));
};

CustomWeekView.navigate = (date, action) => {
    switch (action) {
        case "PREV":
            return addDays(date, -7);
        case "NEXT":
            return addDays(date, 7);
        default:
            return date;
    }
};

CustomWeekView.title = (date) => {
    const start = startOfWeek(date, { weekStartsOn: 1 });
    const end = endOfWeek(date, { weekStartsOn: 1 });
    return `${format(start, "yyyy.MM.dd", { locale: ko })} - ${format(end, "yyyy.MM.dd", { locale: ko })}`;
};

export default CustomWeekView;
