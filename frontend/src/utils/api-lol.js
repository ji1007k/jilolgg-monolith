// 쿠키에서 특정 이름을 가진 값을 가져오는 함수
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null; // 쿠키가 없으면 null을 반환
}


export async function fetchFavoriteTeam() {
    const response = await fetch('/api/lol/favorites', {
        method: "GET",
        credentials: "include",
    });

    if (!response.ok) {
        throw new Error(await response.text());
    }

    return await response.json();
}

export async function apiAddFavoriteTeam(teamId) {
    const csrfToken = getCookie("XSRF-TOKEN");
    const response = await fetch(`/api/lol/favorites/${teamId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken,
        },
        credentials: "include",
    });

    if (!response.ok) {
        throw new Error(await response.text());
    }
}

export async function apiRemoveFavoriteTeam(teamId) {
    const csrfToken = getCookie("XSRF-TOKEN");
    const response = await fetch(`/api/lol/favorites/${teamId}`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken,
        },
        credentials: "include",
    });

    if (!response.ok) {
        throw new Error(await response.text());
    }
}

export async function getAllSchedules() {
    const response = await fetch('/api/lol/matches', {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        throw new Error('전체 경기 일정 조회 실패');
    }

    return await response.json();
}

// 첫 진입 시 해당년도 전체 경기일정 조회
/*
export async function getMatchesByLeagueIdAndYear(leagueId, year) {
    const response = await fetch(`/api/lol/matches?leagueId=${leagueId}&year=${year}`, {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        throw new Error('경기 일정 조회 실패');
    }

    return await response.json();
}
*/

// 달력에서 선택한 날짜 기준으로 데이터 조회
export async function getMatchesByLeagueIdAndDate(leagueId, startDate, endDate) {
    const response = await fetch(`/api/lol/matches?leagueId=${leagueId}&startDate=${startDate}&endDate=${endDate}`, {
        method: 'GET',
        credentials: 'include'
    });

    if (!response.ok) {
        throw new Error('경기 일정 조회 실패');
    }

    return await response.json();
}

export async function getFavoritTeamSchedule(favoriteTeamCode) {
    const response = await fetch(`/api/lol/matches/team/${favoriteTeamCode}`, {
        method: 'GET',
    });

    if (!response.ok) {
        throw new Error('즐겨찾는 팀 일정 조회 실패');
    }

    return await response.json();
}

export async function apiFetchTournaments(leagueId, year) {
    const response = await fetch(`/api/lol/tournaments?leagueId=${leagueId}&year=${year}`, {
        method: 'GET',
    })

    if (!response.ok) {
        throw new Error('토너먼트 조회 실패');
    }

    return await response.json();
}

export async function apiFetchStandings(tournamentId) {
    const response = await fetch(`/api/lol/standings/${tournamentId}`, {
        method: 'GET',
    })

    if (!response.ok) {
        throw new Error('순위 조회 실패');
    }

    return await response.json();
}

export async function apiGetMatchHistory(matchIds) {
    const response = await fetch(`/api/lol/matchhistory`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(matchIds),
    });

    if (!response.ok) {
        throw new Error('경기 전적 조회 실패');
    }

    return await response.json();
}

export async function apiUpdateLeagueOrders(leagueIds) {
    const csrfToken = getCookie("XSRF-TOKEN");
    const response = await fetch(`/api/lol/leagues/orders`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken,
        },
        body: JSON.stringify(leagueIds),
        credentials: "include",
    });

    if (!response.ok) {
        throw new Error(await response.text());
    }
}
