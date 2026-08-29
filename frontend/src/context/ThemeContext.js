"use client";

// src/context/ThemeContext.js
import { createContext, useCallback, useContext, useEffect, useState } from "react";

const THEME_STORAGE_KEY = "theme";

const ThemeContext = createContext({
    theme: "light",
    toggleTheme: () => {},
});

export const useTheme = () => useContext(ThemeContext);

export const ThemeProvider = ({ children }) => {
    const [theme, setThemeState] = useState("light");

    // layout.js의 블로킹 스크립트가 hydration 전에 <html data-theme>를 이미 세팅해 두므로,
    // 여기서는 그 값을 읽어와 React 상태와 맞추기만 한다(FOUC 방지는 스크립트가 담당).
    useEffect(() => {
        const stored = localStorage.getItem(THEME_STORAGE_KEY);
        const initial =
            stored === "dark" || stored === "light"
                ? stored
                : window.matchMedia("(prefers-color-scheme: dark)").matches
                    ? "dark"
                    : "light";

        setThemeState(initial);
        document.documentElement.setAttribute("data-theme", initial);
    }, []);

    const toggleTheme = useCallback(() => {
        setThemeState((prev) => {
            const next = prev === "dark" ? "light" : "dark";
            localStorage.setItem(THEME_STORAGE_KEY, next);
            document.documentElement.setAttribute("data-theme", next);
            return next;
        });
    }, []);

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    );
};
