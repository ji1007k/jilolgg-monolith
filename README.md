
# JIKIM.GG

> League of Legends(LoL) e스포츠 경기 일정 정보를 제공하는 웹 서비스  
> [서비스 바로가기](https://ec2-54-180-118-74.ap-northeast-2.compute.amazonaws.com/jikimi)

---

## 1. 프로젝트 개요

JIKIM.GG는 LoL Esports API를 활용해 리그, 팀, 경기 정보를 수집하고 실시간으로 제공하는 웹 기반 e스포츠 정보 서비스입니다.  
또한, JWT 인증, 실시간 채팅, 팀 즐겨찾기 등 사용자 편의 기능을 웹 및 모바일 환경에서 제공합니다.  
실시간 데이터 처리, 인증/보안 등 다양한 백엔드 기술 학습 목적을 포함합니다.

---

## 2. 주요 기능
다음과 같은 주요 기능을 제공합니다. (기능 상세 [여기](./docs/features.md)에서 확인)
- OAuth2 + JWT 기반 로그인/로그아웃/토큰 갱신
- 경기 정보 자동 연동 및 캐싱 처리 (API → DB → Redis)
- 경기 일정 동기화 (Spring Batch + WebFlux 기반 병렬 수집)
- 실시간 채팅 기능 (WebSocket + Redis Pub/Sub)
- Swagger 기반 API 문서 자동 생성

---

## 3. 기술 스택

### Backend
- Java 17, Spring Boot 3 (Spring MVC)
- Spring Security (OAuth2 + JWT)
- Spring Batch 5, Redisson (분산 락)
- JPA (Hibernate), PostgreSQL (AWS RDS)
- Redis (Pub/Sub, 캐시)
- WebClient (비동기 외부 API 호출)
- JUnit5, Mockito, MockMvc
- Logback

### Frontend
- Next.js (CSR + SSR)
- Express.js (API 프록시 미들웨어)

### Infra / DevOps
- AWS EC2 (Ubuntu)
- Docker, Nginx (HTTPS 리버스 프록시)
- GitHub Actions (CI/CD)
- GitHub OIDC + AWS IAM (보안 배포)

### 기타
- OpenAPI (Swagger)
- Postman (API 테스트)

---

## 문서 바로가기
- [시스템 아키텍처](./docs/architecture.md)
- [Infra 구성 및 배포](./docs/infra.md)
- [개발 과정 및 한계](./docs/development.md)
- [개선 방향](./docs/improvements.md)