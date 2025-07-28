
# JILoL.gg

> LoL e스포츠 경기 일정 정보를 제공하는 웹 서비스  
> [서비스 바로가기](https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/jikimi)

---

## 1. 프로젝트 개요

JILoL.gg는 LoL Esports API를 활용해 리그, 팀, 경기 정보를 수집하고 실시간으로 제공하는 웹 기반 e스포츠 정보 서비스입니다.  
또한, JWT 인증, 실시간 채팅, 팀 즐겨찾기 등 사용자 편의 기능을 웹 및 모바일 환경에서 제공합니다.  
실시간 데이터 처리, 인증/보안 등 다양한 백엔드 기술 학습 목적을 포함합니다.

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
- Java 17, Spring Boot 3.x (Spring MVC)
- Spring Security (JWT 기반 인증)
- Spring Batch, Redisson (분산 락)
- JPA (Hibernate), MapStruct (Entity-DTO 매핑)
- PostgreSQL (AWS RDS), Redis (Pub/Sub, 캐시)
- WebClient (비동기 외부 API)
- JUnit5, Mockito, Logback, Spring Actuator

### Frontend
- Next.js
- Express.js (API 프록시 미들웨어)

### Infra / DevOps
- AWS EC2 (Ubuntu)
- Docker, Nginx (HTTPS 리버스 프록시)
- GitHub Actions + AWS SSM 기반 CI/CD
- GitHub OIDC + AWS IAM (보안 배포)

### 기타
- OpenAPI (Swagger)
- Postman (API 테스트)

---

## 4. 기술적 특징

### 캐싱 및 성능 최적화
- Spring Cache + Redis: 경기 일정, 팀 정보 캐싱으로 조회 성능 개선
- 캐시 무효화: 데이터 동기화 후 자동 캐시 클리어

### 배치 처리 최적화
- 파티셔닝: 리그별 병렬 처리로 성능 향상
- 청크 기반 처리: 대용량 데이터 효율적 처리
- 분산 락: Redisson을 활용한 동시성 제어

### 보안 및 인증
- 무상태 JWT: Access Token (1시간) + Refresh Token (7일)
- 쿠키 보안: HttpOnly, Secure, SameSite 설정으로 XSS/CSRF 방어
- CORS 설정: 허용된 도메인만 접근 가능
- CSRF 보호: 상태 변경 API 요청 시 CSRF 토큰 검증
- 권한 기반 접근제어: USER, MANAGER, ADMIN 권한 분리

### 인프라 및 배포
- CI/CD 자동화: GitHub Actions + GHCR + AWS SSM 기반 배포
- 보안 배포: GitHub OIDC + IAM Role로 키 없는 안전한 배포
- 컨테이너화: Docker + Nginx 리버스 프록시 구성
- 메모리 최적화: EC2 프리티어 환경에서 JVM 튜닝 경험

---

## 5. 문서 바로가기
- [시스템 아키텍처](./docs/architecture.md) - Mermaid 다이어그램으로 시각화
- [인프라 구성](./docs/infra.md) - GitHub Actions + GHCR + AWS SSM 기반 CI/CD
- [개선 및 최적화 경험](./docs/development.md)
  - JWT 무상태 인증 + CSRF 방어 구현
  - 분산락과 배치 파티셔닝으로 성능 최적화
  - WebSocket + Redis Pub/Sub 실시간 처리
  - EC2 프리티어에서 메모리 한계 대응
- [향후 개선 계획](./docs/improvements.md)
