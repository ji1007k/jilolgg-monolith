"use client";

import { useTheme } from "@/context/ThemeContext.js";

export default function ThemeToggle() {
    const { toggleTheme } = useTheme();

    return (
        <button
            type="button"
            className="theme-toggle-btn"
            onClick={toggleTheme}
            aria-label="다크모드 전환"
        >
            <span className="theme-icon theme-icon-light" aria-hidden="true">☀️</span>
            <span className="theme-icon theme-icon-dark" aria-hidden="true">🌙</span>
        </button>
    );
}
