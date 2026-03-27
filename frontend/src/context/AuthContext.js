"use client";

// src/context/AuthContext.js
import React, { createContext, useContext, useState, useEffect } from 'react';
import { refreshToken as refreshTokenApi, login as apiLogin, fetchCsrfToken } from "@/utils/api";
import {useRouter} from "next/navigation.js"; // API 로직 분리된 곳에서 import
import { requestForToken } from "@/utils/firebase";

// TODO userid, username 객체로 합치기
// 기본 값 설정
const AuthContext = createContext({
    userId: null,
    username: null,
    expirationTime: null,
    login: () => {},
    logout: () => {},
    refreshToken: () => {},
    devLogin: () => {},
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [userId, setUserId] = useState(null);
    const [username, setUsername] = useState(null);
    const [expirationTime, setExpirationTime] = useState(null);
    const router = useRouter();

    // FIXME 서버에서 설정한 토큰 만료시간 사용하도록 수정
    // 액세스 토큰 갱신 요청
    const refreshToken = async () => {
        const result = await refreshTokenApi();
        console.log('Token Expiration Time:', result.expirationTime);
        // setExpirationTime(new Date(result.expirationTime)); // 만료 시간 업데이트
        const newExpriationDate = new Date(Date.now() + 30 * 60 * 1000);
        setExpirationTime(newExpriationDate); // 토큰 유효시간 30분 연장
        localStorage.setItem("expirationTime", newExpriationDate.toISOString());  // 문자열로 저장
    };

    // FIXME 서버에서 설정한 토큰 만료시간 사용하도록 수정
    // 로그인 처리 함수
    const login = (userId, username, expirationTimeStr) => {
        // const expirationDate = new Date(expirationTimeStr);  // 문자열을 Date 객체로 변환
        const expirationDate = new Date(Date.now() + 30 * 60 * 1000); // 유효시간 30분
        setUserId(userId);
        setUsername(username);
        setExpirationTime(expirationDate);  // ISO 형식의 문자열로 저장
        localStorage.setItem("userId", userId);
        localStorage.setItem("username", username);
        localStorage.setItem("expirationTime", expirationDate.toISOString());  // 문자열로 저장

        // 로그인 성공 시 FCM 권한 요청 및 백엔드 토큰 등록
        requestForToken();
    };


    // 로그아웃 처리 함수
    const logout = () => {
        setUserId(null);
        setUsername(null);
        setExpirationTime(null);
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        localStorage.removeItem('expirationTime');

        const basePath = process.env.NEXT_PUBLIC_BASE_PATH;
        window.location.href = `${basePath}/`; // 메인 페이지로 이동 (새로고침)
    };

    // 로그인 상태 초기화
    useEffect(() => {
        // CSRF 토큰 발급
        fetchCsrfToken();

        const storedUserId = localStorage.getItem('userId');
        const storedUsername = localStorage.getItem('username');
        const storedExpirationTime = localStorage.getItem('expirationTime');

        if (storedUserId && storedUsername && storedExpirationTime) {
            setUserId(storedUserId);
            setUsername(storedUsername);
            setExpirationTime(new Date(storedExpirationTime));
            
            // 이미 로그인된 유저가 페이지 새로고침 시에도 권한 요청/토큰 갱신
            requestForToken();
        }

        // TODO prod 환경에서는 로그인 못하도록
        // 개발용 이스터에그
        const handler = (e) => {
            if (e.ctrlKey && e.altKey && e.code === 'KeyA') {
                devLogin();
            }
        };
        
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, []);

    const devLogin = async (username = 'admin', password = 'admin') => {
        try {
            const result = await apiLogin(username, password);
            if (result.success) {
                login(result.userId, username, result.expirationTime);
                router.push(result.mainPageUrl);
            }
        } catch (err) {
            console.error("관리자 로그인 실패", err);
        }
    };

    return (
        <AuthContext.Provider value={{
            userId, username, expirationTime,
            login, logout, refreshToken, devLogin }}>
            {children}
        </AuthContext.Provider>
    );
};
