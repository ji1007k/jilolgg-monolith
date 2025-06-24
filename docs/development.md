[← 이전 페이지로 돌아가기](../README.md)

## 개선 및 최적화 경험

1.  **JWT 기반 무상태 인증 구조** (CSRF 공격 방어를 위한 CORS + SameSite 조합 적용)
    - JWT를 httpOnly 쿠키에 저장해 XSS 공격에 대응하고, 자동 전송 구조 유지
    - CSRF 방어를 위해
      - SameSite=Lax 설정으로 외부 출처 쿠키 전송 제한
      - CORS에서 허용된 Origin만 허용하고 credentials=true 설정
    - 로컬 개발 시 프록시 미들웨어로 Origin을 백엔드와 일치시켜 SameSite 제한 우회
    - 배포 환경에서는 Nginx 리버스 프록시로 프론트·백엔드 도메인 통합, CORS 문제 해소 및 안정적 CSRF 방어 구현

---

2. **데이터 갱신 및 처리 최적화**
    - LOL Esports API 응답 구조 복잡, 검색 조건 제한 → 서버에서 정제 후 DB 저장 방식으로 전환
    - 스케줄러 도입으로 실시간성 개선
      - 수동 갱신 시 Redisson 분산 락 적용 → 중복 실행 방지로 데이터 정합성 확보
    - Spring Batch + 파티셔닝 적용 → 병렬 처리로 데이터 양이 많아질수록 갱신 소요시간 단축

---

3. **데이터 조회 성능 최적화**
  - 반복 API 호출 최소화 위해 Redis 캐시 도입 → 조회 속도 약 10배 향상 확인

---

4. **WebSocket + Redis Pub/Sub 기반 실시간 채팅 기능**
    - Redis Pub/Sub 구조 적용 → 다중 인스턴스 간 메시지 처리 안정화
    - 연결 유지 및 UX 개선을 위해 Ping/Pong 전략 적용 

---

5. **테스트 작성 시 CSRF 토큰 검증 문제와 해결**
    - 스프링 시큐리티 기본 설정에서는 CSRF 토큰 검증 시 세션 유지가 필요해 무상태 JWT 인증 구조와 충돌하는 문제 발생
    - 무상태 인증을 유지하기 위해 CSRF 토큰을 쿠키에 저장하고, 클라이언트가 쿠키에서 토큰을 읽어 요청 헤더에 넣어 전송하도록 변경
    - 서버는 쿠키와 헤더의 토큰 일치 여부만 검사하여 세션 없이 무상태 CSRF 검증을 구현함

---

6. **배포 방식 개선**
    - 초기: SSH 수동 배포
    - 이후: GitHub Actions 기반 CI/CD 구성 (SSH 접속 방식)
    - 보안 강화:
        - Nginx에서 HTTPS 포트만 허용, 루트 접근 제한, 의심 요청 차단 설정
        - AWS Systems Manager (SSM) 도입
        - 최종 배포는 GitHub OIDC + SSM 접속 방식으로 전환 (SSH는 본인 IP만 허용)

---

7. **EC2 프리티어 사양 한계 대응**
    - 단일 인스턴스에 프론트 및 백엔드 배포 + Nginx 리버스 프록시 + Redis 운영중 OOM, 접속 문제 발생
    - 안정화를 위한 노력
      - PostgreSQL DB를 Docker → EC2 설치 → AWS RDS로 전환
      - Redis, JVM, 도커 컨테이너 메모리 제한 + 스왑 메모리 활성화
      - 싱글 코어에 맞춰 SerialGC 사용
      - 데이터 갱신 후 영속성 컨텍스트 flush/clear
      - JDK 도구(jps, jstat 등)를 활용해 GC 동작 상태와 메모리 사용 현황 모니터링

---
