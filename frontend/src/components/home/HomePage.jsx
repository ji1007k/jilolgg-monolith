import "@/styles/css/chat.css";
import "@/styles/css/standings.css";

import { useAuth } from "@/context/AuthContext";
import { CalandarProvider } from "@/context/CalendarContext.js";
import SectionCalendar from "@/components/home/SectionCalendar";
import SectionStandings from "@components/home/SectionStandings";
import ChatButton from "@components/home/ChatButton";
import Chat from "@components/home/Chat.jsx";

export default function HomePage() {
    const { userId } = useAuth();

    return (
        <main className="home-container">
            <CalandarProvider>
                <div className="section" id="section1">
                    <SectionCalendar />
                </div>

                <div className="section" id="section2">
                    <SectionStandings />
                </div>
            </CalandarProvider>

            {/* {userId ? <Chat /> : <ChatButton />} */}
        </main>
    );
}
