import format from 'date-fns/format';
import ko from 'date-fns/locale/ko';

export const formats = {
    dateFormat: 'd',
    dayFormat: (date) => format(date, 'M/d (EEE)', { locale: ko }),
    agendaDateFormat: 'M-d',
    agendaTimeFormat: 'HH:mm',
    agendaHeaderFormat: 'yyyy-MM-d',
    monthHeaderFormat: 'yyyy년 M월',
    dayHeaderFormat: 'yyyy-MM-d',
};
