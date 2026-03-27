"use client"

import {useState, useEffect, useRef} from 'react';
import { useAuth } from "@/context/AuthContext.js";

const Chat = () => {
    const { userId } = useAuth();
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [isChatOpen, setIsChatOpen] = useState(false); // 채팅창 열림/닫힘 상태
    const messagesEndRef = useRef(null);  // 메시지 끝을 참조하는 ref
    const wsRef = useRef(null); // useRef: 값이 변경돼도 컴포넌트를 재렌더링x

    useEffect(() => {
        // 백엔드 구조 변경으로 인해 WebSocket 채팅 기능이 비활성화되었습니다.
        // 추후 다른 방식으로 채팅을 구현할 때까지 연결하지 않습니다.
    }, []);

    // 핑 메시지(비활성화)
    useEffect(() => {
    }, []);

    // 새 메시지가 추가될 때마다 스크롤을 맨 아래로
    useEffect(() => {
        // 메시지가 변경될 때마다 실행
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]); // messages가 변경될 때마다 실행

    const handleEnterPress = (e) => {
        if (e.key === 'Enter') {
            sendMessage();
        }
    }

    const sendMessage = () => {
        if (inputMessage && inputMessage.trim() !== "") {
            // 새 메시지 추가 (내 메시지라고 가정) 로컬 반응만 적용
            const data = { text: inputMessage, sender: userId, name: "Me", time: new Date().toLocaleTimeString('ko-KR', { hour: 'numeric', minute: 'numeric' }) };
            setMessages((prevMessages) => [...prevMessages, data]);
            setInputMessage('');         // 입력 필드 초기화
        }
    };

    return (
        <>
            {/* 채팅 버튼 */}
            <button className="chat-toggle-button" onClick={() => setIsChatOpen(!isChatOpen)}>
                {isChatOpen ? "✖" : "💬"}
            </button>

            {/* 채팅 창 */}
            {isChatOpen && (
                <div className="chat-container">
                    <h2>Chat</h2>
                    <div className="chat-box">
                        {messages.map((message, index) => (
                            <div
                                key={index}
                                className={`message ${message.sender === userId ? "sent" : "received"}`}
                            >
                                <div>{message.text}</div>
                                <div className="time">{message.time || '오전 10:07'}</div>
                            </div>
                        ))}
                        {/* 메시지 목록 끝에 ref를 연결하여 스크롤을 맨 아래로 */}
                        <div ref={messagesEndRef} />
                    </div>
                    <div className="chat-input">
                        <input
                            type="text"
                            value={inputMessage}
                            onChange={(e) => setInputMessage(e.target.value)}
                            onKeyDown={handleEnterPress}
                            placeholder="Type a message"
                        />
                        <button onClick={sendMessage}>Send</button>
                    </div>
                </div>
            )}
        </>
    );
};

export default Chat;
