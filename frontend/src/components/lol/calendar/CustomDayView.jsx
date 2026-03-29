import { addDays, format } from "date-fns";
import ko from "date-fns/locale/ko";
import { ScheduleDayListView } from "@/components/lol/calendar/ScheduleListView";

function CustomDayView(props) {
    return <ScheduleDayListView {...props} />;
}

CustomDayView.range = (date) => [date];

CustomDayView.navigate = (date, action) => {
    switch (action) {
        case "PREV":
            return addDays(date, -1);
        case "NEXT":
            return addDays(date, 1);
        default:
            return date;
    }
};

CustomDayView.title = (date) => format(date, "yyyy.MM.dd (EEE)", { locale: ko });

export default CustomDayView;
