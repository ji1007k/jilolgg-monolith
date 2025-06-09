[← 이전 페이지로 돌아가기](../README.md)

## 개발 과정에서의 고민과 한계

- **JWT 기반 무상태 인증 구조 (CSRF 공격 방어를 위해 Double Submit Cookie 패턴 적용)**
    - JWT 기반 인증 방식 도입. 토큰은 httpOnly 쿠키에 저장해 XSS 방지 및 자동 전송 구조 구성
    - Secure, SameSite=Lax, CORS 정책 등으로 CSRF 방어
    - 로컬 개발 환경에서는 프록시 미들웨어로 Origin 변경 → SameSite 제한 우회
  > Double Submit Cookie 패턴: JWT를 httpOnly 쿠키와 일반 쿠키 두 곳에 저장하고, 서버에서 이를 비교함으로써 요청 위변조를 방지

- **HTTP 클라이언트 변경 (RestTemplate → WebClient)**
    - 초기에는 RestTemplate 사용 → 이후 WebClient로 전환
    - 비동기 통신, 유지보수 용이성, WebFlux 호환성 확보

- **LOL Esports API 연동 이슈**
    - API 응답 구조 복잡 / 검색 조건 제한적 → 서버에서 정제 후 DB 저장 방식으로 전환
    - 스케줄러 도입으로 실시간성 개선
    - 수동 갱신 시 Redisson 기반 분산 락 적용 → 데이터 정합성 보장

- **데이터 연동 및 저장 구조 개선**
    - 반복 API 호출 최소화 → DB 저장 + Redis 캐시 도입 -> 조회 속도 약 10배 향상
    - Spring Batch + 파티셔닝 도입 → 전체 처리 시간 절반 수준으로 단축
    - EC2 프리티어 제약으로 병렬 배치는 로컬에서 테스트

- **WebSocket + Redis Pub/Sub 기반 채팅 기능**
    - Redis Pub/Sub 구조 적용 → 다중 인스턴스 간 메시지 처리 안정화
    - Ping/Pong 전략 적용 → 연결 유지 및 UX 개선

- **테스트 코드 작성**
    - 실제 운영 환경과 유사한 테스트 환경을 구성하여 신뢰성 있는 테스트를 목표로 함
    - 통합 테스트: `@SpringBootTest`, TestRestTemplate/WebClient 기반 end-to-end 흐름 검증
    - 단위 테스트: `@MockBean`, Mockito, `@WebMvcTest` 활용하여 외부 의존성을 제거하고 로직 중심 테스트 수행
    - 계층별 테스트 구분 (Controller / Service / Repository)
    - 일부 로직에서 TDD 방식 적용

- **배포 방식 개선**
    - 초기: SSH 수동 배포
    - 이후: GitHub Actions 기반 CI/CD 구성 (SSH 접속 방식)
    - 보안 강화:
        - Nginx에서 HTTPS 포트만 허용, 루트 접근 제한, 의심 요청 차단 설정
        - AWS Systems Manager (SSM) 도입
        - 최종 배포는 GitHub OIDC + SSM 접속 방식으로 전환 (SSH는 본인 IP만 허용)

- **Nginx 로드밸런싱 테스트**
    - EC2 프리티어 한계로 로컬 Docker 환경에 백엔드 2개 구성
    - Nginx 로드밸런싱 알고리즘 테스트 (round-robin, least_conn, ip_hash)

- **Swagger 인증 및 테스트 환경 이슈**
    - JWT 무상태 인증 환경에서 Swagger 테스트 시 CSRF 검증 오류 발생
    - 해결 방법:
        - Swagger 요청 시 특정 인증 헤더를 포함시키고, 해당 헤더 포함 요청에 대해 CSRF 검사 제외
        - RequestMatcher를 사용하여 특정 요청만 CSRF 무시
        - Swagger 문서에는 @ApiImplicitParam, @SecurityRequirement로 인증 정보 설정

- **EC2 프리티어 사양 한계 대응**
    - 프론트엔드와 백엔드를 분리하여 운영하며, Nginx를 통해 HTTPS 트래픽을 리버스 프록시.
    - Redis EC2 설치 및 메모리 제한
    - 1GB 메모리, 1 vCPU 환경 내에서의 OOM, 커널 강제 종료, SSH 접속 불가 등의 문제 발생
    - 대응 방안:
        - PostgreSQL Docker → EC2 직접 설치 → AWS RDS 전환
        - Redis 및 JVM 힙 메모리/컨테이너 메모리 제한 설정
        - 스왑 메모리 활성화
        - 데이터 동기화 후 영속성 컨텍스트 초기화로 메모리 사용 최적화
