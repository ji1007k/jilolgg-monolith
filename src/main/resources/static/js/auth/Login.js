// import * as JwtUtil from './jwt/JwtUtil.js';

export default class Login {
    CONTAINER;

    constructor() {
        this.CONTAINER = document.getElementById('loginArea');

        this.initEvent();
    }

    initEvent() {
        this.CONTAINER.querySelector(".login-btn").addEventListener('click', async () => {
            await this.loginWithSessionCookie();
        });
    }

    // =========================================
    // 방법1) 토큰 발급 후 서버 요청 시 세션쿠키에 저장된 토큰 자동으로 포함
    async loginWithSessionCookie() {
        const id = this.CONTAINER.querySelector("#username").value;
        const pwd = this.CONTAINER.querySelector("#password").value;


        const loginInfo = `${id}:${pwd}`;
        const utf8Encoded = String.fromCharCode(...new TextEncoder().encode(loginInfo));
        const response = await fetch("/auth/login", {
            method: "POST",
            headers: {
                "Authorization": `Basic ${btoa(utf8Encoded)}`,
                "Content-Type": "application/json"
            }
        });


        if (!response.ok) {
            alert("로그인 실패");
            throw new Error(await response.text());
        }

        const result = await response.json();

        setTimeout(() => {
            // 로그인 후 메인페이지로 이동
            location.href = result.mainPageUrl;
        }, 2000);
    }


    async testLoginWithSessionCookie() {
        const response = await fetch("/mypage/admin", {
            method: "GET",
            // (CORS) 환경에서 다른 도메인으로 요청을 보낼 때 사용되며, credentials: "include"를 설정하면 클라이언트는 쿠키나 인증 정보를 포함하여 요청
            // 자동으로 쿠키에 jwt 포함하기 위한 설정(브라우저 환경에서).
            // 없으면 서버 filter를 통해 쿠키에서 token을 못찾아 인증 실패함
            credentials: "include",
            headers: {
                // 쿠키에 token이 자동으로 포함되어 Authorization 헤더를 별도로 설정할 필요 없음
                // "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        });

        console.log(await response.text());
    }


    // ====================================================
    // 방법2) 토큰 발급 후 header에 Authorization 및 토큰 직접 추가
    async login() {
        const id = this.CONTAINER.querySelector('#username').value;
        const pwd = this.CONTAINER.querySelector('#password').value;

        await testAdminScope(await getToken(`${id}:${pwd}`))

    }

    async getToken(data) {
        const utf8Encoded = String.fromCharCode(...new TextEncoder().encode(data));

        const response = await fetch("/token", {
            method: "POST",
            headers: {
                "Authorization": `Basic ${btoa(utf8Encoded)}`,
                "Content-Type": "application/json" // 필요에 따라 설정
            }
        });

        const token = await response.text();
        console.log(token);

        return token;
    }

    async testAdminScope(token) {
        const response = await fetch("/admin-page", {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        });

        console.log(await response.text());
    }

}