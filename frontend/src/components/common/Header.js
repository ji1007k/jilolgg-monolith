"use client";

import {useAuth} from "@/context/AuthContext.js"; // useAuth 훅을 사용
import UserInfo from "@/components/user/UserInfo.js"; // UserInfo 컴포넌트
import LoginLink from "@/components/auth/LoginLink.js"; // LoginLink 컴포넌트
import ThemeToggle from "@/components/common/ThemeToggle.js"; // 다크모드 토글 버튼
import Link from 'next/link';
import {usePathname} from "next/navigation.js";
import {useEffect, useRef, useState} from "react";

export default function Header() {
    const { username } = useAuth();  // AuthContext에서 값 가져오기
    const pathname = usePathname();
    const isMainPage = pathname === "/";
    const [activeSection, setActiveSection] = useState('');
    const headerRef = useRef(null);

    // 헤더 실제 높이를 CSS 변수로 반영한다. 모바일에서 로그인 상태/화면 폭에 따라
    // 헤더가 한 줄/두 줄로 바뀌면서 높이가 달라지는데, 하드코딩된 여백을 쓰면
    // 달력 상단(연/월 선택, Today 버튼)이 fixed 헤더 밑에 가려지는 문제가 있었다.
    useEffect(() => {
        const headerEl = headerRef.current;
        if (!headerEl) return;

        const updateHeaderHeight = () => {
            document.documentElement.style.setProperty('--header-height', `${headerEl.offsetHeight}px`);
        };

        updateHeaderHeight();

        const observer = new ResizeObserver(updateHeaderHeight);
        observer.observe(headerEl);

        return () => observer.disconnect();
    }, [isMainPage, username]);

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
        <header ref={headerRef}>
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
                    <ThemeToggle />
                    {username ? (
                        <UserInfo username={username} />
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
