# Swagger API 운영 가이드

기준 코드: 현재 `main` 브랜치 (`2026-03-28`)

## 1) 인증 흐름 (Swagger에서 테스트할 때)
1. `GET /csrf`로 CSRF 토큰/쿠키 발급
2. `GET /auth/login` (Basic Auth)으로 로그인 -> `access_token`, `refresh_token` 쿠키 발급
3. 보호 API 호출 시:
- Bearer 토큰 또는 쿠키 인증 필요
- `POST/PUT/DELETE`는 `X-XSRF-TOKEN` 필요
- Swagger에서 CSRF 우회 테스트 시 `X-From-Swagger: skip` 사용 가능

## 2) 기능별 API 그룹

### AUTH
- `POST /auth/signup`: 회원가입
- `GET /auth/login`: 로그인 (Basic Auth)
- `GET /auth/logout`: 로그아웃
- `POST /auth/token/refresh`: 토큰 갱신

### LOL 조회 API (읽기)
- `GET /lol/leagues`
- `GET /lol/tournaments?leagueId=&year=`
- `GET /lol/teams?leagueId=&slugs=`
- `GET /lol/teams/{slug}`
- `GET /lol/matches?leagueId=&startDate=&endDate=`
- `GET /lol/matches/team/{name}`
- `GET /lol/standings/{tournamentId}`
- `POST /lol/matchhistory`

### LOL 동기화/관리 API (쓰기/운영)
- `PUT /lol/leagues/orders`
- `POST /lol/leagues/sync` (ADMIN)
- `POST /lol/teams/sync` (ADMIN)
- `POST /lol/matches/sync` (ADMIN)
- `GET /lol/batch/run-match-job?year=` (ADMIN, 테스트성 엔드포인트)
- `GET /lol/tournaments/sync` (현재 코드상 ADMIN 제한 주석 처리됨)

### 즐겨찾기
- `POST /lol/favorites/{teamId}`
- `GET /lol/favorites`
- `DELETE /lol/favorites/{teamId}`

### 알림(FCM)
- `POST /notification/token`
- `POST /notification/alarm`
- `GET /notification/alarm?matchIds=a,b,c`
- `POST /notification/test`
- `GET /notification/test`

### 게시글
- `POST /posts` (큐 등록)
- `POST /posts/non-batch`
- `POST /posts/batch`
- `GET /posts/{id}`
- `GET /posts?keyword=&sort=`
- `PUT /posts/{id}`
- `DELETE /posts/{id}`

## 3) 운영 권장 노출 정책
- 유지: AUTH, LOL 조회/즐겨찾기/알림, 게시글
- 운영 분리 권장:
- `[TEST]` 태그 API (`/lol/batch/**`, `/csrf`, `/token/generate`)는 운영 Swagger에서 숨기거나 별도 프로필로 분리
- `/lol/tournaments/sync`는 ADMIN 제한 복구 권장

## 4) 현재 코드 기준 주의사항
- `SecurityConfig`와 Controller `@PreAuthorize`가 이중으로 권한을 제어함
- 일부 API는 `authenticated()` + `@PreAuthorize('SCOPE_ADMIN')` 조합이므로 Swagger 테스트 시 권한 클레임 확인 필요
- CSRF 우회 헤더 `X-From-Swagger: skip`는 운영용 문서에서 주의 문구 필요

## 5) Admin Manual Match APIs (2026-03-29)
- `PUT /admin/manual-matches/{matchId}`
  - Create or update a match with `leagueId`, `tournamentId`, `startTime`, `state`, `blockName`, `strategyType`, `gameCount`, `teamIds`.
  - Rewrites `match_teams` for the match using provided `teamIds`.
  - Optional lock fields: `lockStartTime`, `lockBlockName` (also updates manual override).
- `PUT /admin/match-overrides/{matchId}`
  - Upsert manual override for `startTime` and `blockName`.
- `GET /admin/match-overrides/{matchId}`
  - Read current override.
- `DELETE /admin/match-overrides/{matchId}`
  - Delete override.
