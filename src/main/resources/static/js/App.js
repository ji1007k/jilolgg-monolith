import * as JwtUtil from './auth/jwt/JwtUtil.js';

export default class App {
    constructor() {
    }

    init() {
        this.initHeader();
    }

    initHeader() {
        const signedIn = document.querySelector(".username-clickable") !== null;
        if (signedIn) {
            const expirationTime = document.getElementById('tokenExpirationTime').textContent;
            JwtUtil.displayExpirationTime(expirationTime);
        }

        this.initHeaderEvent();
    }

    initHeaderEvent() {

        const signedIn = document.querySelector(".username-clickable") !== null;
        if (!signedIn) return;

        // 로그인한 경우에만 초기화
        const refreshTokenBtn = document.querySelector(".refresh-token-btn");
        refreshTokenBtn.addEventListener('click', async () => {
            await JwtUtil.refreshToken();
        });

        // 사용자 이름 클릭 시 드롭다운 메뉴 show
        const toggleDropdownBtn = document.querySelector(".username");
        toggleDropdownBtn.addEventListener('click', async () => {
            const dropdown = document.getElementById('dropdown');
            dropdown.classList.toggle('active');
        });

        // 페이지 클릭 시 드롭다운 숨기기
        window.onclick = function(event) {
            if (!event.target.matches('.username') && !event.target.matches('.username *')) {
                const dropdown = document.getElementById('dropdown');
                dropdown.classList.remove('active');
            }
        }
    }



}