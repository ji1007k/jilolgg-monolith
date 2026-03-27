"use client";

import {useAuth} from "@/context/AuthContext.js"; // useAuth 훅을 사용
import UserInfo from "@/components/user/UserInfo.js"; // UserInfo 컴포넌트
import LoginLink from "@/components/auth/LoginLink.js"; // LoginLink 컴포넌트
import Link from 'next/link';
import {usePathname} from "next/navigation.js";
import {useEffect, useState} from "react";

export default function Header() {
    const { username, expirationTime } = useAuth();  // AuthContext에서 값 가져오기
    const pathname = usePathname();
    const isMainPage = pathname === "/";
    const [activeSection, setActiveSection] = useState('');

    const handleClick = (sectionId) => {
        setActiveSection(sectionId);
    };

    useEffect(() => {
        const sections = document.querySelectorAll('div.section');

        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting && activeSection !== entry.target.id) {
                        setActiveSection(entry.target.id);
                    }
                });
            },
            { rootMargin: '-50% 0px -50% 0px', threshold: 0 } // 중앙 기준
        );

        sections.forEach((section) => observer.observe(section));

        return () => {
            sections.forEach((section) => observer.unobserve(section));
        };
    }, []);


    return (
        <header>
            <div className="header-container">
                {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
                <div>
                    {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
                    <Link href="/" className="main-link">JILoL.gg</Link>
                    {/*<a> 태그는 브라우저의 기본 HTML 동작을 따르기 때문에, Next.js가 제공하는 라우팅 기능 (next/link)을 우회함
                        -> basePath 적용 안됨*/}
                    <a href="/api/swagger-ui/index.html" className="api-docs-link">API Docs</a>
                </div>
                <div className="user-info">
                    {username ? (
                        <UserInfo username={username} expirationTime={expirationTime} />
                    ) : (
                        <LoginLink />
                    )}
                </div>
            </div>
            {/*메인페이지인 경우에만 nav 표시*/}
            { isMainPage &&
                <div className="nav-container">
                    <nav>
                        <ul>
                            <li className={activeSection === 'section1' ? 'active' : ''}>
                                <a href="#section1" onClick={() => handleClick('section1')}>일정</a>
                            </li>
                            <li className={activeSection === 'section2' ? 'active' : ''}>
                                <a href="#section2" onClick={() => handleClick('section2')}>순위</a>
                            </li>
                        </ul>
                    </nav>
                </div>
            }

        </header>
    );
}
