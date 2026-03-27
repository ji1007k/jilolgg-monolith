"use client";

import {useRouter} from "next/navigation";

export default function LoginLink() {
    const router = useRouter();

    const goToLoginPage = (e) => {
        e.preventDefault();

        // React의 클라이언트 사이드 네비게이션을 사용해 페이지를 변경
        // 전체 페이지 새로고침 없이, SPA(Single Page Application) 방식으로 이동
        router.push("/auth/login");
    }

    return (
        <a onClick={goToLoginPage} className="login-btn">
            로그인
        </a>
    );
}
