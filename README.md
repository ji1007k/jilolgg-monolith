
# JILoL.gg

> LoL e스포츠 경기 일정 정보를 제공하는 웹 서비스  
> [서비스 바로가기](https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/jikimi)

---

## 1. 프로젝트 개요

LoL Esports API를 활용하여 리그, 팀, 경기 정보를 수집하고 제공하는 웹 서비스입니다. 
JWT 인증, 실시간 채팅, 관련 정보 동기화 배치 처리 등의 기능을 구현하며 
백엔드 중심의 기술 학습과 클라우드 환경 운영 경험을 목적으로 한 프로젝트입니다.

---

## 2. 주요 기능
다음과 같은 주요 기능을 제공합니다.
- JWT 기반 로그인/로그아웃/토큰 갱신
- 경기 정보 자동 연동 및 캐싱 처리 (API → DB → Redis)
- 경기 일정 동기화 (Spring Batch + 파티셔닝)
- 실시간 채팅 (WebSocket + Redis Pub/Sub)
- Swagger 기반 API 문서화

---

## 3. 기술 스택

### Backend
- Java 17, Spring Boot 3 (MVC, Security, Batch), JPA (Hibernate), PostgreSQL, H2, Redis, MapStruct, WebClient, Redisson
### Frontend
- Next.js, Express.js
### Infra
- AWS EC2/RDS, Docker, Nginx, GitHub Actions CI/CD
### Test
- JUnit5, Mockito, Swagger

---

## 4. 구현 내용

### JWT 기반 인증 시스템
- Spring Security를 활용한 무상태 JWT 인증 구현
- Access Token(1시간) + Refresh Token(7일) 분리 관리
- httpOnly 쿠키 저장으로 XSS 방어, 쿠키-헤더 토큰 일치 검증으로 CSRF 방어

### Spring Batch 데이터 동기화
- LoL Esports API 연동한 경기 정보 자동 수집
- 리그별 파티셔닝을 통한 병렬 처리로 성능 개선
- Redisson 분산 락으로 동시성 제어 (향후 다중 인스턴스 확장 대비)

### 실시간 채팅 시스템
- WebSocket 기반 양방향 통신 구현
- Redis Pub/Sub으로 메시지 중계 (향후 다중 인스턴스 확장 대비)
- 30초 주기 Ping 메시지 전송으로 연결 안정성 확보

### 캐싱 및 성능 최적화
- Spring Cache + Redis: 경기 일정, 팀 정보 캐싱으로 조회 성능 개선
- 데이터 동기화 후 자동 캐시 무효화

---

## 5. 문서 바로가기
- [시스템 아키텍처](./docs/architecture.md) - Mermaid 다이어그램 시각화
- [인프라 구성](./docs/infra.md) - GitHub Actions + GHCR + AWS SSM 기반 CI/CD
- [문제 해결 경험](./docs/development.md)
  - JWT 무상태 인증 + CSRF 방어 구현
  - 분산락과 배치 파티셔닝으로 성능 최적화
  - WebSocket + Redis Pub/Sub 실시간 처리
  - EC2 프리티어에서 메모리 한계 대응
- [향후 개선 계획](./docs/improvements.md)
