[← 이전 페이지로 돌아가기](../README.md)

## 시스템 아키텍처 다이어그램 

```
사용자
  │
  ▼
[Nginx Reverse Proxy]  ← HTTPS 트래픽 수신 및 프록시
  │
  ▼
[Express 서버 (Frontend Gateway)]
  ├─ /api        → Backend API 프록시
  ├─ /chat       → WebSocket 연결
  └─ Next.js     → CSR / SSR 라우팅 처리
      │
      ▼
[Next.js (CSR/SSR 클라이언트)]
  └─ 로그인, 일정 조회, 채팅 인터페이스

─────────────────────────────────────────────

[Spring Boot Backend]
  ├─ REST API 서버
  ├─ JWT / OAuth2 인증 처리
  ├─ LOL Esports API 데이터 연동
  ├─ Redis Pub/Sub 채팅 처리
  ├─ 배치 스케줄러 (Spring Batch + WebFlux)
  └─ Redisson 분산 락 처리

─────────────────────────────────────────────

[Redis 서버]
  ├─ 경기 일정 캐싱
  └─ 실시간 채팅 메시지 송수신 (Pub/Sub)

[PostgreSQL (AWS RDS)]
  └─ 리그/경기/유저/순위 정보 저장소

─────────────────────────────────────────────

[AWS EC2 + Docker + GitHub Actions]
  ├─ 프론트/백엔드 컨테이너 실행
  ├─ Nginx 리버스 프록시 구성
  ├─ GitHub Actions 통한 CI/CD 자동 배포
  └─ GitHub OIDC + AWS IAM으로 보안 강화

```