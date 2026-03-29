"use client";

import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/context/AuthContext";

const BEST_OF_OPTIONS = [
    { label: "BO1", value: 1 },
    { label: "BO3", value: 3 },
    { label: "BO5", value: 5 },
];

const getLeagueId = (league) => league?.leagueId ?? league?.id ?? "";
const getTournamentId = (tournament) => tournament?.tournamentId ?? tournament?.id ?? "";
const getTeamId = (team) => team?.teamId ?? team?.id ?? "";

export default function AdminPage() {
    const { username } = useAuth();
    const [activeAdminTab, setActiveAdminTab] = useState("sync");
    const [msg, setMsg] = useState("");
    const [pushCountdown, setPushCountdown] = useState(0);
    const [isPushSending, setIsPushSending] = useState(false);

    const [matchId, setMatchId] = useState("");
    const [overrideStartTime, setOverrideStartTime] = useState("");
    const [overrideBlockName, setOverrideBlockName] = useState("");
    const [lockStartTime, setLockStartTime] = useState(true);
    const [lockBlockName, setLockBlockName] = useState(true);

    const [manualLeagueId, setManualLeagueId] = useState("");
    const [manualTournamentId, setManualTournamentId] = useState("");
    const [manualStartTime, setManualStartTime] = useState("");
    const [manualBlockName, setManualBlockName] = useState("");
    const [manualState, setManualState] = useState("unstarted");
    const [manualBestOf, setManualBestOf] = useState(3);
    const [manualTeamA, setManualTeamA] = useState("");
    const [manualTeamB, setManualTeamB] = useState("");
    const [manualLockStartTime, setManualLockStartTime] = useState(false);
    const [manualLockBlockName, setManualLockBlockName] = useState(false);
    const [externalMatchId, setExternalMatchId] = useState("");
    const [externalLinkStatus, setExternalLinkStatus] = useState(null);
    const [externalCandidates, setExternalCandidates] = useState([]);

    const [leagues, setLeagues] = useState([]);
    const [tournaments, setTournaments] = useState([]);
    const [teams, setTeams] = useState([]);
    const [loadingLeagues, setLoadingLeagues] = useState(false);
    const [loadingTournaments, setLoadingTournaments] = useState(false);
    const [loadingTeams, setLoadingTeams] = useState(false);

    const selectedYear = useMemo(() => {
        if (!manualStartTime) return new Date().getFullYear();
        const year = Number(manualStartTime.slice(0, 4));
        return Number.isNaN(year) ? new Date().getFullYear() : year;
    }, [manualStartTime]);

    const getCookie = (name) => {
        if (typeof document === "undefined") return null;
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(";").shift();
        return null;
    };

    const ensureCsrfToken = async () => {
        let csrf = getCookie("XSRF-TOKEN");
        if (csrf) return csrf;

        await fetch("/api/csrf", { method: "GET", credentials: "include" });
        csrf = getCookie("XSRF-TOKEN");
        return csrf;
    };

    const toLocalDateTimeString = (value) => {
        if (!value) return null;
        const normalized = value.trim().replace(" ", "T");
        if (normalized.length === 16) return `${normalized}:00`;
        return normalized.slice(0, 19);
    };

    const fromLocalDateTimeString = (value) => {
        if (!value) return "";
        const normalized = value.trim().replace(" ", "T");
        return normalized.slice(0, 16);
    };

    useEffect(() => {
        const fetchLeagues = async () => {
            setLoadingLeagues(true);
            try {
                const res = await fetch("/api/lol/leagues", { credentials: "include" });
                if (!res.ok) {
                    setLeagues([]);
                    setMsg(`리그 목록 조회 실패 (${res.status})`);
                    return;
                }

                const data = await res.json().catch(() => []);
                const list = Array.isArray(data) ? data : [];
                setLeagues(list);
                if (!manualLeagueId && list.length > 0) {
                    setManualLeagueId(getLeagueId(list[0]));
                }
            } catch (error) {
                setLeagues([]);
                setMsg(`리그 목록 조회 오류: ${error.message}`);
            } finally {
                setLoadingLeagues(false);
            }
        };

        fetchLeagues();
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        if (!manualLeagueId) {
            setTournaments([]);
            setTeams([]);
            setManualTournamentId("");
            setManualTeamA("");
            setManualTeamB("");
            return;
        }

        // League changed: reset dependent fields first.
        setManualTournamentId("");
        setManualTeamA("");
        setManualTeamB("");

        const fetchDependentData = async () => {
            setLoadingTournaments(true);
            setLoadingTeams(true);

            try {
                const [tournamentRes, teamRes] = await Promise.all([
                    fetch(`/api/lol/tournaments?leagueId=${encodeURIComponent(manualLeagueId)}&year=${selectedYear}`, {
                        credentials: "include",
                    }),
                    fetch(`/api/lol/teams?leagueId=${encodeURIComponent(manualLeagueId)}`, {
                        credentials: "include",
                    }),
                ]);

                if (tournamentRes.ok) {
                    const tournamentData = await tournamentRes.json().catch(() => []);
                    const tournamentList = Array.isArray(tournamentData) ? tournamentData : [];
                    setTournaments(tournamentList);
                } else {
                    setTournaments([]);
                }

                if (teamRes.ok) {
                    const teamData = await teamRes.json().catch(() => []);
                    const teamList = Array.isArray(teamData) ? teamData : [];
                    setTeams(teamList);
                } else {
                    setTeams([]);
                }
            } catch (error) {
                setTournaments([]);
                setTeams([]);
                setMsg(`토너먼트/팀 조회 오류: ${error.message}`);
            } finally {
                setLoadingTournaments(false);
                setLoadingTeams(false);
            }
        };

        fetchDependentData();
    }, [manualLeagueId, selectedYear]);

    const syncApi = async (url, method) => {
        setMsg(`요청 중... (${url})`);
        try {
            const headers = { "X-From-Swagger": "skip" };
            const res = await fetch(url, { method, headers, credentials: "include" });
            if (!res.ok) {
                const errorText = await res.text().catch(() => "");
                setMsg(`요청 실패 (${res.status}): ${errorText || res.statusText}`);
                return;
            }

            const text = await res.text();
            setMsg(`처리 완료: ${text.substring(0, 300)}`);
        } catch (err) {
            setMsg(`네트워크 오류: ${err.message}`);
        }
    };

    const handleUpsertManualMatch = async () => {
        if (!matchId.trim()) {
            setMsg("matchId는 필수입니다.");
            return;
        }
        if (!manualLeagueId) {
            setMsg("리그를 선택하세요.");
            return;
        }
        if (!manualTournamentId) {
            setMsg("토너먼트를 선택하세요.");
            return;
        }
        if (!manualStartTime) {
            setMsg("startTime은 필수입니다.");
            return;
        }
        if (!manualTeamA || !manualTeamB) {
            setMsg("대진 팀 2개를 선택하세요.");
            return;
        }
        if (manualTeamA === manualTeamB) {
            setMsg("대진 팀은 서로 달라야 합니다.");
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(`/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                },
                credentials: "include",
                body: JSON.stringify({
                    leagueId: manualLeagueId,
                    tournamentId: manualTournamentId,
                    startTime: toLocalDateTimeString(manualStartTime),
                    blockName: manualBlockName.trim() || null,
                    bestOf: manualBestOf,
                    state: manualState.trim() || "unstarted",
                    teamIds: [manualTeamA, manualTeamB],
                    lockStartTime: manualLockStartTime,
                    lockBlockName: manualLockBlockName,
                }),
            });

            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                setMsg(`수동 일정 저장 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }
            setMsg(`수동 일정 저장 완료: ${JSON.stringify(data)}`);
        } catch (error) {
            setMsg(`수동 일정 저장 오류: ${error.message}`);
        }
    };

    const handleDeleteManualMatch = async () => {
        if (!matchId.trim()) {
            setMsg("matchId는 필수입니다.");
            return;
        }

        if (!window.confirm(`원본 경기(matchId=${matchId.trim()})를 삭제할까요?`)) {
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(`/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}`, {
                method: "DELETE",
                headers: {
                    ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                },
                credentials: "include",
            });

            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                setMsg(`원본 경기 삭제 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }

            setMsg(`원본 경기 삭제 완료: matchId=${matchId.trim()}`);
        } catch (error) {
            setMsg(`원본 경기 삭제 오류: ${error.message}`);
        }
    };

    const handleUpsertOverride = async () => {
        if (!matchId.trim()) {
            setMsg("matchId를 입력하세요.");
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(`/api/admin/match-overrides/${encodeURIComponent(matchId.trim())}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                },
                credentials: "include",
                body: JSON.stringify({
                    startTime: toLocalDateTimeString(overrideStartTime),
                    blockName: overrideBlockName,
                    lockStartTime,
                    lockBlockName,
                    applyImmediately: true,
                }),
            });

            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                setMsg(`오버라이드 저장 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }
            setMsg(`오버라이드 저장 완료: ${JSON.stringify(data)}`);
        } catch (error) {
            setMsg(`오버라이드 저장 오류: ${error.message}`);
        }
    };

    const handleLinkExternalMatch = async () => {
        if (!matchId.trim()) {
            setMsg("기준 matchId를 입력하세요.");
            return;
        }
        if (!externalMatchId.trim()) {
            setMsg("externalMatchId를 입력하세요.");
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(`/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}/external-link`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                },
                credentials: "include",
                body: JSON.stringify({
                    externalMatchId: externalMatchId.trim(),
                }),
            });

            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                setMsg(`외부 경기 연결 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }
            setExternalLinkStatus(data);
            await handleGetExternalLinkCandidates(false);
            setMsg(`외부 경기 연결 완료: ${JSON.stringify(data)}`);
        } catch (error) {
            setMsg(`외부 경기 연결 오류: ${error.message}`);
        }
    };

    const handleUnlinkExternalMatch = async () => {
        if (!matchId.trim()) {
            setMsg("기준 matchId를 입력하세요.");
            return;
        }
        if (!externalMatchId.trim()) {
            setMsg("externalMatchId를 입력하세요.");
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(
                `/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}/external-link?externalMatchId=${encodeURIComponent(externalMatchId.trim())}`,
                {
                    method: "DELETE",
                    headers: {
                        ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                    },
                    credentials: "include",
                }
            );

            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                setMsg(`외부 경기 연결 해제 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }

            setExternalLinkStatus(null);
            setMsg(`외부 경기 연결 해제 완료: ${externalMatchId.trim()}`);
        } catch (error) {
            setMsg(`외부 경기 연결 해제 오류: ${error.message}`);
        }
    };

    const handleGetExternalLinkStatus = async () => {
        if (!matchId.trim()) {
            setMsg("기준 matchId를 입력하세요.");
            return;
        }

        try {
            const res = await fetch(`/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}/external-link`, {
                method: "GET",
                credentials: "include",
            });

            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                setExternalLinkStatus(null);
                setMsg(`외부 경기 연결 상태 조회 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }

            setExternalLinkStatus(data);
            setExternalMatchId(data.externalMatchId || "");
            setMsg(`외부 경기 연결 상태 조회 완료: ${JSON.stringify(data)}`);
        } catch (error) {
            setMsg(`외부 경기 연결 상태 조회 오류: ${error.message}`);
        }
    };

    const handleGetExternalLinkCandidates = async (showMessage = true) => {
        if (!matchId.trim()) {
            setMsg("기준 matchId를 입력하세요.");
            return;
        }

        try {
            const res = await fetch(`/api/admin/manual-matches/${encodeURIComponent(matchId.trim())}/external-link/candidates`, {
                method: "GET",
                credentials: "include",
            });

            const data = await res.json().catch(() => []);
            if (!res.ok) {
                setExternalCandidates([]);
                setMsg(`외부 경기 후보 조회 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }

            const candidates = Array.isArray(data) ? data : [];
            setExternalCandidates(candidates);
            if (showMessage) {
                setMsg(`외부 경기 후보 조회 완료: ${candidates.length}건`);
            }
        } catch (error) {
            setMsg(`외부 경기 후보 조회 오류: ${error.message}`);
        }
    };

    const handleGetOverride = async () => {
        if (!matchId.trim()) {
            setMsg("matchId를 입력하세요.");
            return;
        }

        try {
            const res = await fetch(`/api/admin/match-overrides/${encodeURIComponent(matchId.trim())}`, {
                method: "GET",
                credentials: "include",
            });

            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                setMsg(`오버라이드 조회 실패 (${res.status}): ${data.message || res.statusText}`);
                return;
            }

            setOverrideStartTime(fromLocalDateTimeString(data.startTime));
            setOverrideBlockName(data.blockName || "");
            setLockStartTime(Boolean(data.lockStartTime));
            setLockBlockName(Boolean(data.lockBlockName));
            setMsg(`오버라이드 조회 완료: ${JSON.stringify(data)}`);
        } catch (error) {
            setMsg(`오버라이드 조회 오류: ${error.message}`);
        }
    };

    const handleDeleteOverride = async () => {
        if (!matchId.trim()) {
            setMsg("matchId를 입력하세요.");
            return;
        }

        try {
            const csrf = await ensureCsrfToken();
            const res = await fetch(`/api/admin/match-overrides/${encodeURIComponent(matchId.trim())}`, {
                method: "DELETE",
                headers: {
                    ...(csrf ? { "X-XSRF-TOKEN": csrf } : {}),
                },
                credentials: "include",
            });

            if (!res.ok) {
                const errorText = await res.text().catch(() => "");
                setMsg(`오버라이드 삭제 실패 (${res.status}): ${errorText || res.statusText}`);
                return;
            }
            setMsg(`오버라이드 삭제 완료: matchId=${matchId}`);
        } catch (error) {
            setMsg(`오버라이드 삭제 오류: ${error.message}`);
        }
    };

    const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

    const sendDelayedTestPush = async () => {
        if (isPushSending) return;

        setIsPushSending(true);
        setMsg("5초 후 테스트 푸시를 발송합니다.");
        try {
            for (let sec = 5; sec >= 1; sec -= 1) {
                setPushCountdown(sec);
                await sleep(1000);
            }
            setPushCountdown(0);

            const response = await fetch(
                "/api/notification/test?title=%EA%B4%80%EB%A6%AC%EC%9E%90%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EC%95%8C%EB%A6%BC&body=5%EC%B4%88%20%EC%A7%80%EC%97%B0%20%ED%91%B8%EC%8B%9C%20%ED%85%8C%EC%8A%A4%ED%8A%B8",
                { method: "GET", credentials: "include" }
            );

            const data = await response.json().catch(() => ({}));
            if (!response.ok || data.success === false) {
                setMsg(`푸시 발송 실패: ${data.message || `${response.status} ${response.statusText}`}`);
                return;
            }
            setMsg(`테스트 푸시 발송 완료 (성공 토큰 수: ${data.sentCount})`);
        } catch (error) {
            setMsg(`푸시 요청 오류: ${error.message}`);
        } finally {
            setPushCountdown(0);
            setIsPushSending(false);
        }
    };

    if (!username) {
        return (
            <div style={{ padding: "100px", textAlign: "center" }}>
                <h2>로그인이 필요합니다.</h2>
                <p>관리자 계정으로 로그인 후 접근하세요.</p>
            </div>
        );
    }

    return (
        <div style={{ maxWidth: "980px", margin: "50px auto", padding: "30px", background: "#1e1e2f", color: "white", borderRadius: "10px" }}>
            <h1 style={{ marginBottom: "10px" }}>관리자 동기화 콘솔</h1>
            <p style={{ color: "#aaa", marginBottom: "30px" }}>
                동기화 실행, 수동 일정 저장, 오버라이드 설정, 테스트 푸시를 한 화면에서 관리합니다.
            </p>

            <div className="nav-container" style={{ marginBottom: "14px" }}>
                <nav>
                    <ul>
                        <li className={activeAdminTab === "sync" ? "active" : ""}>
                            <a href="#admin-sync" onClick={(e) => { e.preventDefault(); setActiveAdminTab("sync"); }}>동기화</a>
                        </li>
                        <li className={activeAdminTab === "manual" ? "active" : ""}>
                            <a href="#admin-manual" onClick={(e) => { e.preventDefault(); setActiveAdminTab("manual"); }}>수동 일정</a>
                        </li>
                        <li className={activeAdminTab === "override" ? "active" : ""}>
                            <a href="#admin-override" onClick={(e) => { e.preventDefault(); setActiveAdminTab("override"); }}>오버라이드</a>
                        </li>
                        <li className={activeAdminTab === "push" ? "active" : ""}>
                            <a href="#admin-push" onClick={(e) => { e.preventDefault(); setActiveAdminTab("push"); }}>푸시 테스트</a>
                        </li>
                    </ul>
                </nav>
            </div>

            {activeAdminTab === "sync" && <section id="admin-sync" style={sectionStyle}>
                <h2 style={sectionTitleStyle}>데이터 동기화</h2>
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    <button onClick={() => syncApi("/api/lol/leagues/sync", "POST")} style={btnStyle}>1. 리그 동기화</button>
                    <button onClick={() => syncApi("/api/lol/tournaments/sync", "GET")} style={btnStyle}>2. 토너먼트 동기화</button>
                    <button onClick={() => syncApi("/api/lol/teams/sync", "POST")} style={btnStyle}>3. 팀 동기화</button>
                    <button onClick={() => syncApi(`/api/lol/matches/sync?year=${new Date().getFullYear()}`, "POST")} style={btnStyle}>
                        4. 올해 경기 일정 동기화
                    </button>
                </div>
            </section>}

            {activeAdminTab === "manual" && <section id="admin-manual" style={sectionStyle}>
                <h2 style={sectionTitleStyle}>수동 일정 저장 (필수값 기반)</h2>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    <input value={matchId} onChange={(e) => setMatchId(e.target.value)} placeholder="matchId (필수)" style={inputStyle} />

                    <select value={manualLeagueId} onChange={(e) => setManualLeagueId(e.target.value)} style={inputStyle} disabled={loadingLeagues}>
                        <option value="">리그 선택</option>
                        {leagues.map((league) => {
                            const id = getLeagueId(league);
                            return (
                                <option key={id} value={id}>
                                    {league.name} ({id})
                                </option>
                            );
                        })}
                    </select>

                    <select
                        value={manualTournamentId}
                        onChange={(e) => setManualTournamentId(e.target.value)}
                        style={inputStyle}
                        disabled={!manualLeagueId || loadingTournaments}
                    >
                        <option value="">토너먼트 선택 (필수)</option>
                        {tournaments.map((tournament) => {
                            const id = getTournamentId(tournament);
                            return (
                                <option key={id} value={id}>
                                    {tournament.slug || id} ({id})
                                </option>
                            );
                        })}
                    </select>

                    <input type="datetime-local" value={manualStartTime} onChange={(e) => setManualStartTime(e.target.value)} style={inputStyle} />
                    <input value={manualBlockName} onChange={(e) => setManualBlockName(e.target.value)} placeholder="blockName (선택)" style={inputStyle} />
                    <input value={manualState} onChange={(e) => setManualState(e.target.value)} placeholder="state (기본 unstarted)" style={inputStyle} />

                    <select value={manualBestOf} onChange={(e) => setManualBestOf(Number(e.target.value))} style={inputStyle}>
                        {BEST_OF_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>

                    <select
                        value={manualTeamA}
                        onChange={(e) => setManualTeamA(e.target.value)}
                        style={inputStyle}
                        disabled={!manualLeagueId || loadingTeams}
                    >
                        <option value="">팀 A 선택 (필수)</option>
                        {teams.map((team) => {
                            const id = getTeamId(team);
                            return (
                                <option key={id} value={id}>
                                    {team.name} ({team.code})
                                </option>
                            );
                        })}
                    </select>

                    <select
                        value={manualTeamB}
                        onChange={(e) => setManualTeamB(e.target.value)}
                        style={inputStyle}
                        disabled={!manualLeagueId || loadingTeams}
                    >
                        <option value="">팀 B 선택 (필수)</option>
                        {teams.map((team) => {
                            const id = getTeamId(team);
                            return (
                                <option key={id} value={id}>
                                    {team.name} ({team.code})
                                </option>
                            );
                        })}
                    </select>
                </div>

                <div style={{ marginTop: "12px", display: "flex", gap: "14px", flexWrap: "wrap" }}>
                    <label><input type="checkbox" checked={manualLockStartTime} onChange={(e) => setManualLockStartTime(e.target.checked)} /> startTime lock</label>
                    <label><input type="checkbox" checked={manualLockBlockName} onChange={(e) => setManualLockBlockName(e.target.checked)} /> blockName lock</label>
                </div>

                <div style={{ marginTop: "12px", display: "flex", gap: "10px", flexWrap: "wrap" }}>
                    <button onClick={handleUpsertManualMatch} style={btnStyle}>수동 일정 저장/수정</button>
                    <button onClick={handleDeleteManualMatch} style={dangerBtnStyle}>원본 경기 삭제</button>
                </div>

                <div style={{ marginTop: "20px", paddingTop: "14px", borderTop: "1px solid #3a3a55" }}>
                    <h3 style={{ margin: "0 0 10px 0", fontSize: "16px" }}>외부 경기 연결/해제</h3>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                        <input
                            value={matchId}
                            onChange={(e) => setMatchId(e.target.value)}
                            placeholder="기준 matchId (내부 경기)"
                            style={inputStyle}
                        />
                        <input
                            value={externalMatchId}
                            onChange={(e) => setExternalMatchId(e.target.value)}
                            placeholder="externalMatchId (외부 API 경기 ID)"
                            style={inputStyle}
                        />
                    </div>
                    <div style={{ marginTop: "12px", display: "flex", gap: "10px", flexWrap: "wrap" }}>
                        <button onClick={handleLinkExternalMatch} style={btnStyle}>외부 경기 연결</button>
                        <button onClick={handleUnlinkExternalMatch} style={dangerBtnStyle}>외부 경기 연결 해제</button>
                        <button onClick={handleGetExternalLinkStatus} style={btnStyle}>연결 상태 조회</button>
                        <button onClick={() => handleGetExternalLinkCandidates(true)} style={btnStyle}>후보 목록 조회</button>
                    </div>

                    {externalLinkStatus && (
                        <div style={{ marginTop: "10px", color: "#b8e1ff", fontSize: "13px" }}>
                            현재 연결: {externalLinkStatus.externalMatchId} ({externalLinkStatus.provider})
                        </div>
                    )}

                    {externalCandidates.length > 0 && (
                        <div style={{ marginTop: "14px", overflowX: "auto" }}>
                            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
                                <thead>
                                    <tr>
                                        <th style={tableThStyle}>externalMatchId</th>
                                        <th style={tableThStyle}>startTime</th>
                                        <th style={tableThStyle}>teams</th>
                                        <th style={tableThStyle}>block/strategy</th>
                                        <th style={tableThStyle}>score</th>
                                        <th style={tableThStyle}>액션</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {externalCandidates.map((candidate) => (
                                        <tr key={candidate.externalMatchId}>
                                            <td style={tableTdStyle}>{candidate.externalMatchId}</td>
                                            <td style={tableTdStyle}>{candidate.startTime || "-"}</td>
                                            <td style={tableTdStyle}>
                                                {(candidate.teamNames || []).join(" vs ") || "-"}
                                            </td>
                                            <td style={tableTdStyle}>
                                                {(candidate.blockName || "-")} / {(candidate.strategy || "-")}
                                            </td>
                                            <td style={tableTdStyle}>{candidate.score}</td>
                                            <td style={tableTdStyle}>
                                                <button
                                                    type="button"
                                                    style={smallBtnStyle}
                                                    onClick={() => setExternalMatchId(candidate.externalMatchId)}
                                                >
                                                    선택
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </section>}

            {activeAdminTab === "override" && <section id="admin-override" style={sectionStyle}>
                <h2 style={sectionTitleStyle}>수동 오버라이드 (기존 매치)</h2>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    <input value={matchId} onChange={(e) => setMatchId(e.target.value)} placeholder="matchId (필수)" style={inputStyle} />
                    <input type="datetime-local" value={overrideStartTime} onChange={(e) => setOverrideStartTime(e.target.value)} style={inputStyle} />
                    <input
                        value={overrideBlockName}
                        onChange={(e) => setOverrideBlockName(e.target.value)}
                        placeholder="blockName (선택)"
                        style={{ ...inputStyle, gridColumn: "1 / span 2" }}
                    />
                </div>

                <div style={{ marginTop: "12px", display: "flex", gap: "14px", flexWrap: "wrap" }}>
                    <label><input type="checkbox" checked={lockStartTime} onChange={(e) => setLockStartTime(e.target.checked)} /> startTime lock</label>
                    <label><input type="checkbox" checked={lockBlockName} onChange={(e) => setLockBlockName(e.target.checked)} /> blockName lock</label>
                    <label><input type="checkbox" checked readOnly /> apply immediately</label>
                </div>

                <div style={{ marginTop: "12px", display: "flex", gap: "10px", flexWrap: "wrap" }}>
                    <button onClick={handleGetOverride} style={btnStyle}>오버라이드 조회</button>
                    <button onClick={handleUpsertOverride} style={btnStyle}>오버라이드 저장/수정</button>
                    <button onClick={handleDeleteOverride} style={dangerBtnStyle}>오버라이드 삭제</button>
                </div>
            </section>}

            {activeAdminTab === "push" && <section id="admin-push" style={sectionStyle}>
                <h2 style={sectionTitleStyle}>푸시 테스트</h2>
                <button onClick={sendDelayedTestPush} style={btnStyle} disabled={isPushSending}>
                    {isPushSending ? `5초 후 푸시 발송 중... (${pushCountdown || 0}초)` : "5초 후 테스트 푸시 발송"}
                </button>
            </section>}

            <div style={{ marginTop: "26px", padding: "18px", background: "#f8f9fa", borderRadius: "8px", color: "#333", minHeight: "90px" }}>
                <strong>실시간 처리 결과</strong>
                <div style={{ marginTop: "10px", fontSize: "14px", whiteSpace: "pre-wrap" }}>{msg || "대기 중..."}</div>
            </div>
        </div>
    );
}

const sectionStyle = {
    marginBottom: "18px",
    padding: "16px",
    background: "#2a2a40",
    borderRadius: "8px",
};

const sectionTitleStyle = {
    margin: "0 0 12px 0",
    fontSize: "18px",
};

const inputStyle = {
    padding: "10px 12px",
    borderRadius: "6px",
    border: "1px solid #4e4e6c",
    background: "#1c1c2f",
    color: "white",
};

const btnStyle = {
    padding: "12px 16px",
    background: "linear-gradient(90deg, #0070f3, #00d4ff)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    fontSize: "15px",
    fontWeight: "bold",
    cursor: "pointer",
};

const dangerBtnStyle = {
    ...btnStyle,
    background: "linear-gradient(90deg, #d62828, #f77f00)",
};

const smallBtnStyle = {
    padding: "6px 10px",
    background: "#1f88ff",
    color: "#fff",
    border: "none",
    borderRadius: "6px",
    cursor: "pointer",
};

const tableThStyle = {
    textAlign: "left",
    borderBottom: "1px solid #4e4e6c",
    padding: "8px",
};

const tableTdStyle = {
    borderBottom: "1px solid #383855",
    padding: "8px",
};
