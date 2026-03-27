import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { logout as apiLogout } from "@/utils/api.js";

export default function TokenExpiration() {
    const { expirationTime, refreshToken, logout, username } = useAuth();
    const [timeLeft, setTimeLeft] = useState(null);

    // 만료 시간 계산
    useEffect(() => {
        if (expirationTime) {
            const expirationDate = expirationTime;  // 문자열을 Date 객체로 변환
            const interval = setInterval(() => {
                const timeRemaining = expirationDate - new Date();
                if (timeRemaining <= 0) {
                    if (username === 'jikim') {
                        refreshToken(); // 토큰 자동 갱신
                    } else {
                        setTimeLeft(0);  // 토큰 만료
                        handleLogout();
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

    const handleLogout = async (e) => {
        logout();
        await apiLogout();
    }

    return (
        <span id="tokenExpirationTimeArea">
            <span id="tokenExpirationTime">
                { timeLeft }
            </span>
            <button className="refresh-token-btn" onClick={refreshToken}>갱신</button>
        </span>
    );
}
