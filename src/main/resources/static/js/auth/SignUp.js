
export default class Singup {
    CONTAINER;

    constructor() {
        this.CONTAINER = document.getElementById('loginArea');

        this.initEvent();
    }

    initEvent() {
        this.CONTAINER.querySelector(".signup-btn").addEventListener('click', async () => {
            await this.signup();
        });
    }

    // =========================================
    async signup() {
        const iptEmail = this.CONTAINER.querySelector("#username");
        const iptPwd = this.CONTAINER.querySelector("#password");

        const response = await fetch("/auth/signup", {
            method: 'POST',
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: iptEmail?.value,
                password: iptPwd?.value,
                name: iptEmail?.value,
                authority: 'SCOPE_ADMIN'
            })
        });

        if (!response.ok) {
            alert("회원가입 실패");
        }

        // 로그인 페이지로 이동
        const loginPageUrl = await response.text();
        window.location.href = loginPageUrl;
    }

}