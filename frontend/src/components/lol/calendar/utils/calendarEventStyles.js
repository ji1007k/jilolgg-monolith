/**
 * [스타일 유틸] 대문자 or 소문자 (혼재되어 있음)
 * @param event
 * @returns {{style: {borderRadius: string, padding: string}}}
 */
// utils/eventPropGetter.js
export const eventPropGetter = (event, selectedTeam, favoriteTeamIds) => {
    const Ids = event.participants?.map(p => p.team.teamId);
    const isFavorite = Ids.some(id => favoriteTeamIds?.includes(id));
    const isSelected = selectedTeam && Ids.includes(selectedTeam.teamId);
    const isUnstarted = new Date(event.start) > new Date();

    let style = {
        borderRadius: '6px',
        padding: '2px 6px',
    };

    if (isFavorite && isSelected) {
        Object.assign(style, {
            backgroundColor: '#f4511e',
            border: '2px solid #ffd54f',
            color: '#fffaf0',
            fontWeight: '600',
            boxShadow: '0 0 0 2px #ffeb3b66',
        });
    } else if (isFavorite) {
        Object.assign(style, {
            backgroundColor: '#f4511e',
            border: '1px solid #d84315',
            color: '#fffaf0',
            fontWeight: '600',
        });
    } else if (isSelected) {
        Object.assign(style, {
            backgroundColor: '#fffde7',
            border: '2px dashed #1976d2',
            color: '#0d47a1',
            fontWeight: '500',
        });
    } else {
        if (isUnstarted) {
            Object.assign(style, {
                backgroundColor: '#e3f2fd',
                border: '1px dashed #64b5f6',
                color: '#1e88e5',
                fontStyle: 'italic',
            });
        } else {
            Object.assign(style, {
                backgroundColor: '#f0f2f5',
                border: '1px solid #cfd8dc',
                color: '#37474f',
            });
        }
    }

    return { style };
};
