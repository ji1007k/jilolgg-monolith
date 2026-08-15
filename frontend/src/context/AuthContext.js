"use client";

// src/context/AuthContext.js
import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { refreshToken as refreshTokenApi, login as apiLogin, fetchCsrfToken } from "@/utils/api";
import {useRouter} from "next/navigation.js"; // API 로직 분리된 곳에서 import
import { requestForToken } from "@/utils/firebase";
import { fetchFavoriteTeam } from "@/utils/api-lol";
import {
    hasLocalPreferences,
    readLocalPreferences,
    clearLocalPreferences,
    mergeLocalPreferencesIntoAccount,
} from "@/utils/userPreferences";
import MergePreferencesModal from "@components/auth/MergePreferencesModal.jsx";

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
    const [mergePromptOpen, setMergePromptOpen] = useState(false);
    const [mergeBusy, setMergeBusy] = useState(false);
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

        // 비로그인 상태에서 이 브라우저에 저장해둔 설정 처리.
        // localStorage에 userId를 넣은 뒤여야 userPreferences가 로그인 상태로 판단한다.
        reconcileLocalPreferences();
    };

    /**
     * 로컬 설정과 계정 설정을 맞춘다.
     * - 로컬에 아무것도 없으면 할 일 없음
     * - 계정이 비어 있으면 충돌이 아니므로 묻지 않고 그대로 올린다
     * - 양쪽 다 있고 다르면 사용자에게 묻는다
     */
    const reconcileLocalPreferences = async () => {
        if (!hasLocalPreferences()) return;

        try {
            const { favoriteTeamIds: localFavorites, leagueOrder: localOrder } = readLocalPreferences();
            const serverFavorites = (await fetchFavoriteTeam()).map((team) => team.teamId);

            // 계정 즐겨찾기가 비어 있고 로컬 리그 순서만 있는 경우도 "충돌 없음"으로 본다.
            // 리그 순서는 서버 조회 없이 덮어써도 잃을 것이 없다(기본 순서일 뿐).
            const favoritesConflict =
                localFavorites.length > 0 &&
                serverFavorites.length > 0 &&
                localFavorites.some((id) => !serverFavorites.includes(id));

            const orderConflict = localOrder.length > 0 && serverFavorites.length > 0;

            if (!favoritesConflict && !orderConflict) {
                await mergeLocalPreferencesIntoAccount();
                return;
            }

            setMergePromptOpen(true);
        } catch (e) {
            // 병합에 실패해도 로그인 자체는 성공한 상태다. 로컬 설정을 남겨두고 넘어간다.
            console.error("로컬 설정 확인 실패", e);
        }
    };

    const handleMergePreferences = async () => {
        setMergeBusy(true);
        try {
            await mergeLocalPreferencesIntoAccount();
        } finally {
            setMergeBusy(false);
            setMergePromptOpen(false);
            // 병합 결과를 화면에 반영하려면 다시 읽어야 한다.
            window.location.reload();
        }
    };

    const handleKeepAccountPreferences = () => {
        clearLocalPreferences();
        setMergePromptOpen(false);
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

    // 로그인 요청이 진행 중인지 표시. Ctrl+Alt+A는 keydown이라 키를 누르고 있으면
    // 자동 반복으로 여러 번 호출되고, 그러면 같은 사용자의 로그인 요청이 동시에 나가
    // 서버에서 refresh token 제약 위반(409)이 날 수 있다.
    const devLoginInFlight = useRef(false);

    const devLogin = async (username = 'admin', password = 'admin') => {
        if (devLoginInFlight.current) return;
        devLoginInFlight.current = true;

        try {
            const result = await apiLogin(username, password);
            if (result.success) {
                login(result.userId, username, result.expirationTime);
                router.push(result.mainPageUrl);
            }
        } catch (err) {
            console.error("관리자 로그인 실패", err);
        } finally {
            devLoginInFlight.current = false;
        }
    };

    return (
        <AuthContext.Provider value={{
            userId, username, expirationTime,
            login, logout, refreshToken, devLogin }}>
            {children}
            <MergePreferencesModal
                isOpen={mergePromptOpen}
                busy={mergeBusy}
                onMerge={handleMergePreferences}
                onKeepAccount={handleKeepAccountPreferences}
            />
        </AuthContext.Provider>
    );
};
