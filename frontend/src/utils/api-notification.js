function getCookie(name) {
    if (typeof document === "undefined") return null;

    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(";").shift();
    return null;
}

async function ensureCsrfToken() {
    let csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) return csrfToken;

    await fetch("/api/csrf", {
        method: "GET",
        credentials: "include",
    });

    csrfToken = getCookie("XSRF-TOKEN");
    return csrfToken;
}

export async function apiGetAlarmStatus(matchIds) {
    if (!Array.isArray(matchIds) || matchIds.length === 0) {
        return [];
    }

    const query = encodeURIComponent(matchIds.join(","));
    const response = await fetch(`/api/notification/alarm?matchIds=${query}`, {
        method: "GET",
        credentials: "include",
    });

    if (!response.ok) {
        throw new Error("알림 설정 상태 조회에 실패했습니다.");
    }

    const data = await response.json();
    return data.enabledMatchIds || [];
}

export async function apiToggleMatchAlarm(matchId) {
    const csrfToken = await ensureCsrfToken();
    const response = await fetch("/api/notification/alarm", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {}),
        },
        credentials: "include",
        body: JSON.stringify({ matchId }),
    });

    if (!response.ok) {
        throw new Error("알림 설정 변경에 실패했습니다.");
    }

    return await response.json();
}
