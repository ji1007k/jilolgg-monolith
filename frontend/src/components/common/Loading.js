// components/Loading.js
import React from "react";
import "@/styles/css/loading.css"; // 스피너 스타일 따로 분리 (아래에 추가됨)

const Loading = ({ message = "로딩 중..." }) => {
    return (
        <div className="loading-wrapper">
            <div className="spinner" />
            <p>{message}</p>
        </div>
    );
};

export default Loading;
