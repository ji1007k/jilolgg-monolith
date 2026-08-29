import React, { useEffect, useRef, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { logout as apiLogout } from "@/utils/api.js";

export default function TokenExpiration() {
    const { expirationTime, refreshToken, logout, username } = useAuth();
    const [timeLeft, setTimeLeft] = useState(null);

    const handleLogout = async () => {
        logout();
        await apiLogout();
    }

    // 인터벌 콜백은 항상 최신 값을 참조해야 하지만, expirationTime이 바뀔 때만
    // 인터벌을 새로 만들고 싶어서(불필요한 재생성 방지) ref로 최신 값을 들고 있는다.
    const latestRef = useRef({ refreshToken, username, handleLogout });
    latestRef.current = { refreshToken, username, handleLogout };

    // 만료 시간 계산
    useEffect(() => {
        if (expirationTime) {
            const expirationDate = expirationTime;  // 문자열을 Date 객체로 변환
            const interval = setInterval(() => {
                const timeRemaining = expirationDate - new Date();
                if (timeRemaining <= 0) {
                    if (latestRef.current.username === 'jikim') {
                        latestRef.current.refreshToken(); // 토큰 자동 갱신
                    } else {
                        setTimeLeft(0);  // 토큰 만료
                        latestRef.current.handleLogout();
                    }
                } else {
                    const minutes = Math.floor(timeRemaining / 60000);
                    const seconds = Math.floor((timeRemaining % 60000) / 1000);
                    setTimeLeft(`${minutes}m ${seconds}s`);
                }
            }, 1000);

            return () => clearInterval(interval); // 컴포넌트 언마운트 시 인터벌 정리
        }
    }, [expirationTime]);

    return (
        <span id="tokenExpirationTimeArea">
            <span id="tokenExpirationTime">
                { timeLeft }
            </span>
            <button className="refresh-token-btn" onClick={refreshToken}>갱신</button>
        </span>
    );
}
