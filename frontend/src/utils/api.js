// utils/api.js
export async function baseFetch(url, options = {}) {
    options.credentials = options.credentials || 'include';
    options.headers = options.headers || {};
    
    let response = await fetch(url, options);

    // 401 Unauthorized 발생 시 토큰 갱신 시도
    if (response.status === 401 && !url.includes('/api/auth/token/refresh')) {
        try {
            const refreshRes = await refreshToken();
            if (refreshRes) {
                // 갱신 성공 후 원래 요청 재시도
                response = await fetch(url, options);
            }
        } catch (err) {
            console.error("Token refresh failed:", err);
            // 갱신 실패 시 로그아웃 처리 등 추가 로직 가능
        }
    }

    return response;
}

export async function login(username, password) {
    const usrInfo = `${username}:${password}`;
    const base64Encoded = btoa(usrInfo);

    try {
        const response = await fetch("/api/auth/login", {
            method: "GET",
            credentials: 'include',
            headers: {
                "Authorization": `Basic ${base64Encoded}`,
                "Content-Type": "application/json",
            },
        });

        if (!response.ok) {
            return {
                success: false,
                errorMessage: "로그인 실패: 사용자명 또는 비밀번호를 확인하세요.",
            };
        }

        const data = await response.json();
        return {
            success: true,
            mainPageUrl: data.mainPageUrl,
            expirationTime: data.expirationTime,
            userId: data.userId,
            username: username,
        };
    } catch (err) {
        console.error("로그인 오류:", err);
        return {
            success: false,
            errorMessage: "서버 오류가 발생했습니다.",
        };
    }
}

export async function signup(username, password) {
    const response = await fetch("/api/auth/signup", {
        method: 'POST',
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            email: username,
            password: password,
            name: username,
            authority: 'SCOPE_USER'
        }),
    });

    if (!response.ok) {
        throw new Error("회원가입 실패");
    }

    return await response.text();
}

export async function logout() {
    const response = await fetch("/api/auth/logout", { method: "GET" });
    if (!response.ok) {
        throw new Error("로그아웃 실패");
    }
}

export async function refreshToken() {
    const response = await fetch("/api/auth/token/refresh", {
        method: "POST",
        credentials: 'include',
    });

    if (!response.ok) {
        return null;
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return null;
}

export async function fetchCsrfToken() {
    try {
        await fetch("/api/csrf", {
            method: "GET",
            credentials: "include"
        });
    } catch (e) {
        console.error("CSRF 토큰 발급 실패", e);
    }
}
