import LoginForm from "@/components/auth/LoginForm.js";
import Link from 'next/link';

export default function LoginPage() {
    return (
        <div className="login-container">
            <h2>로그인</h2>
            <LoginForm />
            <p>
                계정이 없나요? <Link href="/auth/signup">회원가입</Link>
            </p>
        </div>
    );
}
