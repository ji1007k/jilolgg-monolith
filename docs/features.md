[← 이전 페이지로 돌아가기](../README.md)

## 상세 기능 목록

### 사용자 인증
- Access/Refresh 토큰 기반 인증
- CSRF와 XSS 공격을 방지하기 위해 Secure 및 httpOnly 속성을 적용한 쿠키 사용

### 경기 정보 시스템
- 리그/토너먼트/팀/경기 정보 연동 (LoL Esports API)
- 스케줄링 기반 데이터 동기화
- 리그별 경기 일정 정보 제공
- 팀 즐겨찾기 기능
- 순위표 조회 (승패, 득실점, 경기 전적)

### 실시간 채팅
- WebSocket 기반 실시간 메시징
- Redis Pub/Sub 구조 적용
- JWT 인증 기반 채팅방 입장 제어
  웹소켓 핸드셰이크 시 JWT 인증 정보 확인 및 사용

### API 문서화
- Swagger(OpenAPI) 기반 API 문서 제공
