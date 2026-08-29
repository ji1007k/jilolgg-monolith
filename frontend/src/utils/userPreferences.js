/**
 * 사용자 취향 설정(즐겨찾기 팀, 리그 노출 순서) 저장소.
 *
 * 즐겨찾기와 리그 순서는 본질적으로 "이 브라우저의 취향"이라 로그인 없이도 쓸 수 있어야 한다.
 * 서버에 둘 이유는 로그인 사용자의 기기 간 동기화뿐이므로 저장 위치를 로그인 여부로 가른다.
 *
 *   비로그인 -> localStorage (서버를 아예 호출하지 않는다)
 *   로그인   -> 기존 API (utils/api-lol.js)
 *
 * 호출부가 이 분기를 신경 쓰지 않도록 여기 한 곳에 가둔다.
 * 새 컴포넌트에서도 api-lol.js의 즐겨찾기/순서 함수를 직접 부르지 말고 이 모듈을 쓸 것.
 */

import {
    fetchFavoriteTeam,
    apiAddFavoriteTeam,
    apiRemoveFavoriteTeam,
    apiUpdateLeagueOrders,
} from './api-lol';

const FAVORITE_TEAMS_KEY = 'jilolgg.favoriteTeamIds';
const LEAGUE_ORDER_KEY = 'jilolgg.leagueOrder';
const HIDDEN_LEAGUES_KEY = 'jilolgg.hiddenLeagueIds';
const DEVICE_ID_KEY = 'jilolgg.deviceId';

/** 서버가 비로그인 구독 주체를 식별하는 헤더 이름. NotificationController와 맞춰야 한다. */
export const DEVICE_ID_HEADER = 'X-Device-Id';

/**
 * 이 브라우저의 기기 식별자. 없으면 만들어 저장한다.
 *
 * 즐겨찾기·리그 순서와 달리 경기 알림은 서버가 "어느 기기가 어느 경기를 구독했는지"
 * 알아야 하므로, 로그인 없이 알림을 받으려면 서버에 보낼 식별자가 필요하다.
 * 사용자를 특정하는 값이 아니라 이 브라우저를 가리키는 임의의 값이다.
 */
export function getDeviceId() {
    if (typeof window === 'undefined') return null;

    try {
        let deviceId = window.localStorage.getItem(DEVICE_ID_KEY);
        if (deviceId) return deviceId;

        deviceId = (window.crypto && typeof window.crypto.randomUUID === 'function')
            ? window.crypto.randomUUID()
            : `dev-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;

        window.localStorage.setItem(DEVICE_ID_KEY, deviceId);
        return deviceId;
    } catch (e) {
        // 시크릿 모드 등에서 저장이 막히면 알림 구독을 유지할 수 없다.
        console.warn('[userPreferences] 기기 식별자 생성 실패', e);
        return null;
    }
}

/** 알림 API 요청에 붙일 헤더. 로그인 상태면 서버가 계정을 쓰므로 굳이 보내지 않는다. */
export function deviceIdHeader() {
    if (isLoggedIn()) return {};

    const deviceId = getDeviceId();
    return deviceId ? { [DEVICE_ID_HEADER]: deviceId } : {};
}

/**
 * 로그인 여부 판별.
 * AuthContext가 로그인/로그아웃 시 localStorage의 userId를 쓰고 지우므로 같은 출처를 본다.
 * (별도 상태 주입 없이 일반 함수에서도 쓸 수 있게 하려는 의도)
 */
function isLoggedIn() {
    if (typeof window === 'undefined') return false;
    return Boolean(window.localStorage.getItem('userId'));
}

/** SSR 시점에는 localStorage가 없다. 반드시 마운트 이후에만 호출할 것. */
function readLocalArray(key) {
    if (typeof window === 'undefined') return [];

    try {
        const raw = window.localStorage.getItem(key);
        if (!raw) return [];

        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch (e) {
        // 손상된 값이 남아 있으면 앱 전체가 막히므로 버리고 진행한다.
        console.warn(`[userPreferences] ${key} 파싱 실패, 초기화합니다.`, e);
        window.localStorage.removeItem(key);
        return [];
    }
}

function writeLocalArray(key, value) {
    if (typeof window === 'undefined') return;

    try {
        window.localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
        // 시크릿 모드나 용량 초과. 기능만 안 될 뿐 앱은 계속 동작해야 한다.
        console.warn(`[userPreferences] ${key} 저장 실패`, e);
    }
}

/** 서버 쓰기 전에 CSRF 쿠키를 받아둔다. 로그인 경로에서만 필요하다. */
async function ensureCsrfToken() {
    await fetch('/api/csrf', { method: 'GET', credentials: 'include' });
}

// ── 즐겨찾기 팀 ──────────────────────────────────────────────

/** @returns {Promise<string[]>} teamId 배열. 배열 순서가 곧 노출 순서다. */
export async function getFavoriteTeamIds() {
    if (!isLoggedIn()) {
        return readLocalArray(FAVORITE_TEAMS_KEY);
    }

    const favorites = await fetchFavoriteTeam();
    return favorites.map((team) => team.teamId);
}

export async function addFavoriteTeam(teamId) {
    if (!isLoggedIn()) {
        const current = readLocalArray(FAVORITE_TEAMS_KEY);
        if (current.includes(teamId)) return;

        writeLocalArray(FAVORITE_TEAMS_KEY, [teamId, ...current]);
        return;
    }

    await ensureCsrfToken();
    await apiAddFavoriteTeam(teamId);
}

export async function removeFavoriteTeam(teamId) {
    if (!isLoggedIn()) {
        const current = readLocalArray(FAVORITE_TEAMS_KEY);
        writeLocalArray(FAVORITE_TEAMS_KEY, current.filter((id) => id !== teamId));
        return;
    }

    await ensureCsrfToken();
    await apiRemoveFavoriteTeam(teamId);
}

// ── 리그 노출 순서 ────────────────────────────────────────────

/**
 * 비로그인 사용자의 리그 순서.
 * 로그인 사용자는 서버가 이미 정렬해서 내려주므로(LeagueService.getAllLeagues) null을 반환한다.
 * @returns {string[]|null} leagueId 배열, 또는 클라이언트 정렬이 불필요하면 null
 */
export function getLocalLeagueOrder() {
    if (isLoggedIn()) return null;

    const order = readLocalArray(LEAGUE_ORDER_KEY);
    return order.length > 0 ? order : null;
}

export async function saveLeagueOrder(leagueIds) {
    if (!isLoggedIn()) {
        writeLocalArray(LEAGUE_ORDER_KEY, leagueIds);
        return;
    }

    await ensureCsrfToken();
    await apiUpdateLeagueOrders(leagueIds);
}

/**
 * 저장된 순서대로 리그 목록을 재정렬한다.
 * 순서 정보에 없는 리그(새로 생긴 리그 등)는 원래 순서를 유지한 채 뒤로 보낸다.
 */
export function applyLeagueOrder(leagues, order) {
    if (!order || order.length === 0) return leagues;

    const rank = new Map(order.map((leagueId, index) => [leagueId, index]));
    const rankOf = (league) => {
        const id = league?.id ?? league?.leagueId;
        return rank.has(id) ? rank.get(id) : Number.MAX_SAFE_INTEGER;
    };

    // sort는 제자리 정렬이므로 원본을 건드리지 않도록 복사한다.
    return [...leagues].sort((a, b) => rankOf(a) - rankOf(b));
}

// ── 숨긴 리그 ────────────────────────────────────────────────

/**
 * 숨긴 리그 ID 목록.
 * 서버에는 아직 숨김 필드가 없어(schema 변경 필요, 후속 작업) 로그인 여부와 무관하게
 * 항상 이 브라우저에만 저장한다. 계정 간 동기화는 되지 않는다.
 * @returns {string[]} leagueId 배열
 */
export function getHiddenLeagueIds() {
    return readLocalArray(HIDDEN_LEAGUES_KEY);
}

export function saveHiddenLeagueIds(leagueIds) {
    writeLocalArray(HIDDEN_LEAGUES_KEY, leagueIds);
}

// ── 로그인 시 병합 ────────────────────────────────────────────

/** 로컬에 저장해둔 설정이 있는지. 병합 여부 판단에 쓴다. */
export function hasLocalPreferences() {
    return (
        readLocalArray(FAVORITE_TEAMS_KEY).length > 0 ||
        readLocalArray(LEAGUE_ORDER_KEY).length > 0
    );
}

export function readLocalPreferences() {
    return {
        favoriteTeamIds: readLocalArray(FAVORITE_TEAMS_KEY),
        leagueOrder: readLocalArray(LEAGUE_ORDER_KEY),
    };
}

export function clearLocalPreferences() {
    if (typeof window === 'undefined') return;

    window.localStorage.removeItem(FAVORITE_TEAMS_KEY);
    window.localStorage.removeItem(LEAGUE_ORDER_KEY);
}

/**
 * 로컬 설정을 로그인한 계정으로 옮긴다.
 *
 * 즐겨찾기는 계정에 없는 것만 추가한다(합집합).
 * 리그 순서는 두 순서를 합칠 방법이 없으므로 로컬 순서로 덮어쓴다.
 *
 * 반드시 로그인이 끝난 뒤(localStorage에 userId가 들어간 뒤) 호출해야 한다.
 */
export async function mergeLocalPreferencesIntoAccount() {
    const { favoriteTeamIds, leagueOrder } = readLocalPreferences();

    if (favoriteTeamIds.length > 0) {
        let serverFavoriteIds = [];
        try {
            const favorites = await fetchFavoriteTeam();
            serverFavoriteIds = favorites.map((team) => team.teamId);
        } catch (e) {
            console.error('[userPreferences] 계정 즐겨찾기 조회 실패, 병합을 건너뜁니다.', e);
            return;
        }

        await ensureCsrfToken();

        // 화면에 보이던 순서를 유지하려면 뒤에서부터 넣어야 한다.
        // addFavoriteTeam이 새 항목을 앞에 붙이기 때문.
        const toAdd = favoriteTeamIds.filter((id) => !serverFavoriteIds.includes(id)).reverse();

        for (const teamId of toAdd) {
            try {
                await apiAddFavoriteTeam(teamId);
            } catch (e) {
                console.error(`[userPreferences] 즐겨찾기 병합 실패: ${teamId}`, e);
            }
        }
    }

    if (leagueOrder.length > 0) {
        try {
            await ensureCsrfToken();
            await apiUpdateLeagueOrders(leagueOrder);
        } catch (e) {
            console.error('[userPreferences] 리그 순서 병합 실패', e);
        }
    }

    clearLocalPreferences();
}
