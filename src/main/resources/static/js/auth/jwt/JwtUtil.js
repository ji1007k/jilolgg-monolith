/**
 * JWT 토큰 관리
 */
let _expirationTime = null;

// 토큰 발급 (세션쿠키에 저장)
export async function getTokenWithSessionCookie(data) {
    const utf8Encoded = String.fromCharCode(...new TextEncoder().encode(data));

    const response = await fetch("/token/generate/sc", {
        method: "POST",
        headers: {
            "Authorization": `Basic ${btoa(utf8Encoded)}`,
            "Content-Type": "application/json" // 필요에 따라 설정
        }
    });

    if (!response.ok) {
        alert("로그인 실패");
        throw new Error(await response.text());
    }

    const result = await response.json();

    // 토큰 만료 시간 초기화
    _expirationTime = new Date(result.expirationTime);
    console.log('Token Expiration Time:', _expirationTime);

    return result;
}

// 액세스 토큰 갱신 요청
export async function refreshToken() {
    try {
        // 리프레시 토큰으로 새로운 액세스 토큰을 갱신
        const response = await fetch('/auth/token/refresh', {
            method: 'POST',
            credentials: 'include', // 쿠키 포함
        });

        if (!response.ok) {
            throw new Error('Failed to refresh token');
        }

        // 새로 발급한 토큰 만료 시간 출력
        const result = await response.json();
        console.log('Token Expiration Time:', result.expirationTime);
        _expirationTime = new Date(result.expirationTime); // 예시로, 실제 토큰 생성 시 받아온 만료 시간 사용

        displayExpirationTime();  // 새로 갱신된 만료 시간 표시
    } catch (error) {
        console.error('Error refreshing token:', error);
    }
}

// 액세스 토큰이 발급될 때 만료 시간을 받아서 화면에 표시하도록 설정
export function displayExpirationTime(expirationTimeStr) {
    if (expirationTimeStr) {
        _expirationTime = new Date(expirationTimeStr);
    }

    // 토큰 생성 후 받아온 만료 시간을 이용하여 남은 시간을 표시
    const currentTime = new Date();
    const timeLeft = _expirationTime - currentTime;

    const timeArea = document.querySelector('#tokenExpirationTime');
    if (timeLeft <= 0) {
        timeArea.textContent = "Token Expired";
    } else {
        const minutesLeft = Math.floor(timeLeft / 60000);
        const secondsLeft = Math.floor((timeLeft % 60000) / 1000);
        timeArea.textContent = `Time Left: ${minutesLeft}m ${secondsLeft}s`;
    }

    // 1초마다 남은 시간 갱신
    setInterval(displayExpirationTime, 1000);
}