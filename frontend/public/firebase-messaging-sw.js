importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

// TODO: 파이어베이스 콘솔 -> 프로젝트 설정 -> 일반 탭 하단의 "웹 앱" 추가 후 나오는 설정값으로 아래를 교체해주세요!
const firebaseConfig = {
  apiKey: "AIzaSyCN0UyvJ6MrS1uz6De49asCYG0pTp0rAac",
  authDomain: "jilolgg.firebaseapp.com",
  projectId: "jilolgg",
  storageBucket: "jilolgg.firebasestorage.app",
  messagingSenderId: "662277282827",
  appId: "1:662277282827:web:7559485c758964c1cb3b0e",
  measurementId: "G-DQ9G4BN878"
};

// 파이어베이스 앱 초기화
firebase.initializeApp(firebaseConfig);

// 백그라운드 메시지 수신 처리기
const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] 백그라운드 메시지 수신 ', payload);

    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/favicon.ico' // 알림 아이콘
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});
