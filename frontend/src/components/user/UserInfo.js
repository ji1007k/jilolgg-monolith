"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext.js";
import TokenExpiration from "@/components/auth/TokenExpiration";
import { logout as apiLogout } from "@/utils/api.js";

export default function UserInfo({ username }) {
    const [dropdownActive, setDropdownActive] = useState(false);
    const { logout } = useAuth();
    const router = useRouter();

    const toggleDropdown = () => {
        setDropdownActive((prev) => !prev);
    };

    const hideDropdown = (event) => {
        if (!event.target.closest("#dropdown") && !event.target.closest(".username")) {
            setDropdownActive(false);
        }
    };

    useEffect(() => {
        window.addEventListener("click", hideDropdown);
        return () => {
            window.removeEventListener("click", hideDropdown);
        };
    }, []);

    const handleLogout = async (e) => {
        e.preventDefault();
        await apiLogout();
        logout();
    };

    const handleMenuClick = (e, targetPath) => {
        e.preventDefault();
        e.stopPropagation();
        setDropdownActive(false);
        router.push(targetPath);
    };

    return (
        <span id="username-area">
            <span className="username username-clickable" onClick={toggleDropdown}>
                {username}
            </span>
            <TokenExpiration />

            {dropdownActive && (
                <div id="dropdown" className="dropdown-content">
                    {username === "admin" ? (
                        <a href="/admin" onClick={(e) => handleMenuClick(e, "/admin")}>관리자 메뉴</a>
                    ) : (
                        <a href="/users/mypage" onClick={(e) => handleMenuClick(e, "/users/mypage")}>마이페이지</a>
                    )}
                    <a onClick={handleLogout} className="logout-btn">
                        로그아웃
                    </a>
                </div>
            )}
        </span>
    );
}
