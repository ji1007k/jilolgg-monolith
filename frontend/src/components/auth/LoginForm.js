"use client";

import { useState } from "react";
import { login as apiLogin } from "@/utils/api.js"; // 로그인 API 호출 분리
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext.js";  // useAuth 훅을 사용

export default function LoginForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const router = useRouter();
    const { login } = useAuth();  // AuthContext에서 login 함수 가져오기

    const handleLogin = async () => {
        setError(null);

        if (!username.trim().length) {
            alert("username 입력하기")
            return;
        }

        if (!password.trim().length) {
            alert("password 입력하기")
            return;
        }

        try {
            const result = await apiLogin(username, password); // login API 호출
            if (result.success) {
                // 로그인 성공 시 받은 데이터로 Context 상태 업데이트
                login(result.userId, username, result.expirationTime);

                // 로그인 성공 후 받은 URL로 리디렉션
                router.push(result.mainPageUrl);
            } else {
                setError("로그인 실패: 사용자명 또는 비밀번호를 확인하세요.");
            }
        } catch (err) {
            setError("서버 오류가 발생했습니다.");
        }
    };

    const handleEnterPress = (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        }
    }

    return (
        <div>
            {error && <p className="error-message">{error}</p>}
            <div className="login-form-group">
                <label htmlFor="username">Username:</label>
                <input
                    type="text"
                    id="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    onKeyDown={handleEnterPress}
                    required
                    placeholder="Enter your username"
                />
            </div>
            <div className="login-form-group">
                <label htmlFor="password">Password:</label>
                <input
                    type="password"
                    id="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyDown={handleEnterPress}
                    required
                    placeholder="Enter your password"
                />
            </div>
            <button className="login-btn" onClick={handleLogin}>
                로그인
            </button>
        </div>
    );
}
