"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext.js";
import TokenExpiration from "@/components/auth/TokenExpiration";
import { logout as apiLogout } from "@/utils/api.js";
import Link from "next/link";

export default function UserInfo({ username }) {
    const [dropdownActive, setDropdownActive] = useState(false);
    const { logout } = useAuth();  // AuthContext에서 logout 함수 가져오기

    const toggleDropdown = () => {
        setDropdownActive((prev) => !prev);
    };

    const hideDropdown = (event) => {
        if (!event.target.closest("#dropdown") && !event.target.closest(".username")) {
            setDropdownActive(false);
        }
    };

    // 페이지 클릭 시 드롭다운 숨기기
    useEffect(() => {
        window.addEventListener("click", hideDropdown);
        return () => {
            window.removeEventListener("click", hideDropdown);
        };
    }, []);

    const handleLogout = async (e) => {
        e.preventDefault(); // a 태그 기본 동작을 방지 (페이지 이동 방지)

        await apiLogout();

        logout();  // 로그아웃 처리
    };

    return (
        <span id="username-area">
            <span className="username username-clickable" onClick={toggleDropdown}>
                {username}
            </span>
            <TokenExpiration />

            {dropdownActive && (
                <div id="dropdown" className="dropdown-content">
                    {username === 'admin' ? (
                        <Link href="/admin">관리자 메뉴</Link>
                    ) : (
                        <Link href="/users/mypage">마이페이지</Link>
                    )}
                    <a onClick={handleLogout} className="logout-btn">
                        로그아웃
                    </a>
                </div>
            )}
        </span>
    );
}
