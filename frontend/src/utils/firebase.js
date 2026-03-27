import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";

// public/firebase-messaging-sw.js 에 넣은 값과 동일한 설정값을 .env.local에 넣거나 여기에 직접 입력.
const firebaseConfig = {
    apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY || "AIzaSyCN0UyvJ6MrS1uz6De49asCYG0pTp0rAac",
    authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN || "jilolgg.firebaseapp.com",
    projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID || "jilolgg",
    storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET || "jilolgg.firebasestorage.app",
    messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || "662277282827",
    appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID || "1:662277282827:web:7559485c758964c1cb3b0e",
    measurementId: process.env.NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID || "G-DQ9G4BN878"
};

// 앱 캐싱 및 중복 초기화 방지
let app;
let messaging;

if (typeof window !== "undefined") {
    app = initializeApp(firebaseConfig);
    messaging = getMessaging(app);
}

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

async function getMessagingServiceWorkerRegistration() {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
        return null;
    }

    const basePath = process.env.NEXT_PUBLIC_BASE_PATH || "";
    const swPath = `${basePath}/firebase-messaging-sw.js`;
    const scope = `${basePath || "/"}/`;

    return await navigator.serviceWorker.register(swPath, { scope });
}

export const requestForToken = async () => {
    try {
        if (!messaging) return null;

        if (typeof Notification === "undefined") {
            return { ok: false, reason: "not_supported" };
        }

        // 이미 허용된 상태면 재요청하지 않음
        let permission = Notification.permission;
        if (permission !== "granted") {
            permission = await Notification.requestPermission();
        }

        if (permission !== 'granted') {
            console.log('푸시 알림 권한이 거부되었습니다.');
            return { ok: false, reason: "permission_denied" };
        }

        const serviceWorkerRegistration = await getMessagingServiceWorkerRegistration();

        // VAPID 키를 사용하여 토큰 발급
        const currentToken = await getToken(messaging, {
            vapidKey: "BPTZTB0ocSsogYDPwqhisxTDCxfLLH-CEsaQ8T05StLEpAOaZl6mTsaDE2CgP9G-Em-BwxSIDsX7XvbpkWUD9bU",
            serviceWorkerRegistration: serviceWorkerRegistration || undefined,
        });

        if (currentToken) {
            console.log('발급된 FCM 토큰:', currentToken);
            const csrfToken = await ensureCsrfToken();

            // 백엔드로 토큰 전송
            await fetch('/api/notification/token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
                },
                credentials: "include",
                body: JSON.stringify({
                    token: currentToken,
                    deviceInfo: navigator.userAgent
                })
            });
            return { ok: true, token: currentToken };
        } else {
            console.log('접근 가능한 토큰을 가져올 수 없습니다. 브라우저 설정을 확인하세요.');
            return { ok: false, reason: "token_unavailable" };
        }
    } catch (err) {
        console.error('토큰 발급 중 오류 발생:', err);
        return { ok: false, reason: "token_error", error: err };
    }
};

export const onMessageListener = () =>
    new Promise((resolve) => {
        if (!messaging) return;
        onMessage(messaging, (payload) => {
            resolve(payload);
        });
    });
