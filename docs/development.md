[← 이전 페이지로 돌아가기](../README.md)

## 문제 해결 경험

1. **JWT 무상태 인증 구현**
- **JWT 저장 방식**: httpOnly 쿠키 사용으로 XSS 방어
- **CSRF 방어**: SameSite 쿠키 설정으로 cross-site 요청 차단
- **CORS 환경에서 쿠키 저장 이슈 해결**:
   - 개발환경: SameSite=None + Secure 설정에도 embedded context(fetch 요청 등)에서 cross-origin 쿠키 저장 실패 (브라우저 보안 정책 강화로 원인 추정) → 프론트엔드 프록시 미들웨어에서 백엔드 쿠키를 중계하여 same-origin 응답으로 변환 (25.07.28)
   - 배포환경: SameSite=Lax + Nginx 리버스 프록시로 same-origin 구성 (도메인 통합)
---

2. **데이터 갱신 및 처리 최적화**
- LoL Esports API 응답 구조 복잡, 검색 조건 제한적 → 서버에서 정제 후 DB 저장 방식으로 전환
- 스케줄러 도입으로 실시간성 개선
  - 수동 갱신 시 Redisson 분산 락 적용 → 중복 실행 방지
- Spring Batch + 파티셔닝 적용 → 병렬 처리로 데이터 양이 많아질수록 갱신 소요시간 단축
  - 처리 시간 95% 단축 (92.5초 → 4.7초)

---

3. **데이터 조회 성능 최적화**
- 반복 API 호출 최소화를 위해 Redis 캐시 도입
- 데이터 특성에 따라 변동성이 낮은 데이터일수록 긴 TTL 적용

---

4. **(이력) WebSocket + Redis Pub/Sub 기능 운영 후 FCM 푸시로 전환**
- 과거에는 WebSocket + Redis Pub/Sub 기반 실시간 채팅 기능을 운영
- 현재는 운영 복잡도/리소스 절감을 위해 WebSocket을 제거하고 FCM 푸시 중심으로 전환
- 현재 상태는 README의 `개선된 통합 시스템 아키텍처 (Current)` 기준으로 관리

---

5. **테스트 작성 시 CSRF 토큰 검증 문제와 해결**
- 스프링 시큐리티 기본 설정에서는 CSRF 토큰 검증 시 세션 유지가 필요해 무상태 JWT 인증 구조와 충돌하는 문제 발생
- 무상태 인증을 유지하기 위해 CSRF 토큰도 쿠키에 저장하고, 클라이언트가 쿠키에서 토큰을 읽어 요청 헤더에 넣어 전송하도록 변경
- 서버는 쿠키와 헤더의 토큰 일치 여부만 검사하여 세션 없이 무상태 CSRF 검증을 구현함

---

6. **배포 방식 개선**
- 초기 SSH 수동 배포의 번거로움과 Nginx access.log 다수의 해킹봇 연결 시도를 확인한 후 배포 방식 변경
- GitHub Actions 기반 CI/CD 배포 자동화 (SSH 접속 방식)
- AWS Systems Manager (SSM) 도입
- Nginx에서 HTTPS 포트만 허용, 루트 접근 제한, 의심 요청 차단 설정
- 최종 배포는 GitHub OIDC + SSM 접속 방식으로 전환 (SSH는 특정 IP만 허용)

---

7. **EC2 프리티어 사양 한계 대응**
- 단일 인스턴스에 프론트 및 백엔드 배포 + Nginx 리버스 프록시 + Redis 운영중 OOM, 접속 문제 발생
- 안정화를 위한 노력
  - PostgreSQL DB를 Docker → EC2 설치 → AWS RDS로 전환
  - Redis, JVM, 도커 컨테이너 메모리 제한 + 스왑 메모리 활성화
  - 싱글 코어에 맞춰 SerialGC 사용
  - 데이터 갱신 후 영속성 컨텍스트 초기화로 메모리 정리
  - JDK 도구(jps, jstat 등)를 활용해 GC 동작 상태와 메모리 사용 현황 모니터링

---
