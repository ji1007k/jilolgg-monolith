"use client";

import { useState } from "react";
import { signup as signupApi } from "@/utils/api"; // API 로직 분리된 곳에서 import
import { useRouter } from "next/navigation";

export default function SignupForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const router = useRouter();

    const handleSignup = async () => {
        if (!username || !password) {
            return;
        }

        const loginPageUrl = await signupApi(username, password); // login 함수 호출
        router.push(loginPageUrl); // 성공 시 로그인페이지으로 이동
    };

    return (
        <div>
            {/*{error && <p className="error-message">{error}</p>}*/}
            <div className="login-form-group">
                <label htmlFor="username">Username:</label>
                <input
                    type="text"
                    id="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
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
                    required
                    placeholder="Enter your password"
                />
            </div>
            <button className="signup-btn" onClick={handleSignup}>
                Sign Up
            </button>
        </div>
    );
}
