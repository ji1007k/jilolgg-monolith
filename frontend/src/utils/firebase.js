import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";

// TODO: public/firebase-messaging-sw.js 에 넣은 값과 동일한 설정값을 .env.local에 넣거나 여기에 직접 입력하세요.
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

export const requestForToken = async () => {
    try {
        if (!messaging) return null;

        // 브라우저 알림 권한 요청
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') {
            console.log('푸시 알림 권한이 거부되었습니다.');
            return null;
        }

        // VAPID 키를 사용하여 토큰 발급 (아까 알려주신 VAPID 공개 키)
        const currentToken = await getToken(messaging, {
            vapidKey: "BHUnEfyGE5qXmTyjxovUR4hECldIs4pp_Bo9geA0FrkoOM0-O18eeowkZ8tW6xuGUxfD2apK3ew5CJgQ-iDvjAQ"
        });

        if (currentToken) {
            console.log('발급된 FCM 토큰:', currentToken);
            // 백엔드로 토큰 전송 (방금 백엔드에 구현한 컨트롤러 호출)
            await fetch('/api/notification/token', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: 1,  // 로그인한 유저 ID를 넣어주세요.
                    token: currentToken,
                    deviceInfo: navigator.userAgent
                })
            });
            return currentToken;
        } else {
            console.log('접근 가능한 토큰을 가져올 수 없습니다. 브라우저 설정을 확인하세요.');
            return null;
        }
    } catch (err) {
        console.error('토큰 발급 중 오류 발생:', err);
        return null;
    }
};

export const onMessageListener = () =>
    new Promise((resolve) => {
        if (!messaging) return;
        onMessage(messaging, (payload) => {
            resolve(payload);
        });
    });
