"use client";

import { useState } from "react";
import { useAuth } from "@/context/AuthContext";

export default function AdminPage() {
    const { username } = useAuth();
    const [msg, setMsg] = useState("");

    const syncApi = async (url, method) => {
        setMsg(`요청 중... (${url}) 데이터 양이 많아 수십 초 대기할 수 있습니다.`);
        try {
            // X-From-Swagger 헤더를 넘겨서 CSRF 필터 무시 (SwaggerConfig에서 지정한 SecurityRequirement 연동)
            const headers = {
                "X-From-Swagger": "skip"
            };

            const res = await fetch(url, {
                method: method,
                headers: headers,
                credentials: "include"
            });

            if (res.ok) {
                const text = await res.text();
                setMsg(`✅ 처리 완료: ${text.substring(0, 200)}`);
            } else {
                setMsg(`❌ 요청 에러 (${res.status}): ${res.statusText}`);
            }
        } catch (err) {
            setMsg(`❌ 네트워크 에러: ${err.message}`);
        }
    };

    if (!username) {
        return (
            <div style={{ padding: '100px', textAlign: 'center' }}>
                <h2>로그인이 필요합니다</h2>
                <p>단축키 <strong>Ctrl + Alt + A</strong> 를 눌러 관리자 계정으로 로그인한 뒤 새로고침 하세요.</p>
            </div>
        );
    }

    return (
        <div style={{ maxWidth: "800px", margin: "50px auto", padding: "30px", background: "#1e1e2f", color: "white", borderRadius: "10px" }}>
            <h1 style={{ marginBottom: "10px" }}>🛠️ 관리자 데이터 동기화 컨트롤 패널</h1>
            <p style={{ color: "#aaa", marginBottom: "30px" }}>
                Riot Esports API 서버로부터 실시간 데이터를 긁어와 로컬 DB를 채웁니다.<br />
                ※ 반드시 위에서부터 <strong>순서대로(1 ➔ 2 ➔ 3 ➔ 4)</strong> 클릭해야 관계(FK)가 깨지지 않습니다.
            </p>

            <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
                <button onClick={() => syncApi('/api/lol/leagues/sync', 'POST')} style={btnStyle}>
                    1. 리그 동기화 (League)
                </button>
                <button onClick={() => syncApi('/api/lol/tournaments/sync', 'GET')} style={btnStyle}>
                    2. 토너먼트 동기화 (Tournament)
                </button>
                <button onClick={() => syncApi('/api/lol/teams/sync', 'POST')} style={btnStyle}>
                    3. 팀 동기화 (Team)
                </button>
                <button onClick={() => syncApi(`/api/lol/matches/sync?year=${new Date().getFullYear()}`, 'POST')} style={btnStyle}>
                    4. 금년도 경기일정 동기화 (Match {new Date().getFullYear()}년 기준)
                </button>
            </div>

            <div style={{ marginTop: "40px", padding: "20px", background: "#f8f9fa", borderRadius: "8px", color: '#333', minHeight: '80px' }}>
                <strong>실시간 처리 결과:</strong> <br /><br />
                <span style={{ fontSize: '15px' }}>{msg || "대기 중..."}</span>
            </div>
        </div>
    );
}

const btnStyle = {
    padding: "18px 25px",
    background: "linear-gradient(90deg, #0070f3, #00d4ff)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    fontSize: "18px",
    fontWeight: "bold",
    cursor: "pointer",
    textAlign: "left",
    boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
    transition: "transform 0.1s"
};
