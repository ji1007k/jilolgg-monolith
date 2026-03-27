"use client";

import { useRef } from "react";
import {useRouter} from "next/navigation";
import { useAuth } from "@/context/AuthContext.js";

export default function LoginLink() {
    const router = useRouter();
    const { devLogin } = useAuth();

    const clickCount = useRef(0);
    const lastClickTime = useRef(0);
    const clickTimer = useRef(null);
    const CLICK_INTERVAL = 450;

    const goToLoginPage = () => {

        // React의 클라이언트 사이드 네비게이션을 사용해 페이지를 변경
        // 전체 페이지 새로고침 없이, SPA(Single Page Application) 방식으로 이동
        router.push("/auth/login");
    };

    const handleDoubleClickLogin = async () => {
        await devLogin(); // 기본 admin/admin
    };

    const handleTripleClickLogin = async () => {
        const restPwd = prompt("pwd 입력:", "");
        if (!restPwd) return;
        await devLogin("jikim", "jikim" + restPwd); // 개발용 계정
    };

    const handleLoginClick = (e) => {
        e.preventDefault();
        const now = Date.now();

        if (now - lastClickTime.current < CLICK_INTERVAL) {
            clickCount.current += 1;
        } else {
            clickCount.current = 1;
        }

        lastClickTime.current = now;

        if (clickTimer.current) {
            clearTimeout(clickTimer.current);
        }

        clickTimer.current = setTimeout(async () => {
            if (clickCount.current === 1) {
                goToLoginPage();
            } else if (clickCount.current === 2) {
                await handleDoubleClickLogin();
            } else if (clickCount.current >= 3) {
                await handleTripleClickLogin();
            }
            clickCount.current = 0;
            clickTimer.current = null;
        }, CLICK_INTERVAL);
    };

    return (
        <a onClick={handleLoginClick} className="login-btn">
            로그인
        </a>
    );
}
