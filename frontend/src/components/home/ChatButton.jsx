import { useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function ChatButton() {
    const router = useRouter();
    const { devLogin } = useAuth();

    const tapCount = useRef(0);
    const lastTapTime = useRef(0);
    const clickTimeout = useRef(null);

    const CLICK_DELAY = 1000; // 단일 클릭 동작 지연
    const TAP_INTERVAL = 500; // 더블/트리플 탭 최대 간격

    // 단일 클릭 실행 -> 로그인 페이지로 이동
    const handleSingleClick = () => {
        router.push("/auth/login");
    };

    // 더블 클릭 실행 -> 관리자 로그인 (토큰 만료 ㅇ)
    const handleDoubleClick = () => {
        devLogin(); // 기본 admin/admin
    };

    // 트리플 클릭 실행 -> 관리자 로그인 (토큰 만료 x)
    const handleTripleClick = () => {
        const restPwd = prompt("pwd 입력:", "");
        if (!restPwd) return; // 취소 시 종료
        devLogin("jikim", "jikim" + restPwd); // 개발용 계정
    };

    const handleClick = () => {
        // 기존 타이머 제거
        if (clickTimeout.current) {
            clearTimeout(clickTimeout.current);
            clickTimeout.current = null;
        }

        // 클릭 후 지연 처리 (페이지 이동 지연)
        clickTimeout.current = setTimeout(() => {
            handleSingleClick();
            clickTimeout.current = null;
        }, CLICK_DELAY);
    };

    const handleDoubleClickWrapper = () => {
        // 단일 클릭 예약 취소 (페이지 이동 취소)
        if (clickTimeout.current) {
            clearTimeout(clickTimeout.current);
            clickTimeout.current = null;
        }
        handleDoubleClick();
    };

    const handleTouchStart = () => {
        const now = Date.now();

        if (now - lastTapTime.current < TAP_INTERVAL) {
            tapCount.current += 1;
        } else {
            tapCount.current = 1;
        }

        lastTapTime.current = now;

        if (tapCount.current === 2) {
            // 더블탭
            if (clickTimeout.current) {
                clearTimeout(clickTimeout.current);
                clickTimeout.current = null;
            }
            handleDoubleClick();
        } else if (tapCount.current === 3) {
            // 트리플탭
            if (clickTimeout.current) {
                clearTimeout(clickTimeout.current);
                clickTimeout.current = null;
            }
            handleTripleClick();
            tapCount.current = 0; // 초기화
        }
    };

    return (
        <button
            onClick={handleClick}
            onDoubleClick={handleDoubleClickWrapper}
            onTouchStart={handleTouchStart}
            className="chat-toggle-button"
            title="로그인 필요"
        >
            🔒
        </button>
    );
}
