# JILoL.gg

> 외부 API 동기화 파이프라인의 성능/정합성/운영 안정성을 개선한 백엔드 중심 프로젝트

- 서비스: [https://jilolgg.up.railway.app/jikimi](https://jilolgg.up.railway.app/jikimi)
- 저장소: [https://github.com/ji1007k/jilolgg-monolith](https://github.com/ji1007k/jilolgg-monolith)

## 프로젝트 목표
- LoL Esports 외부 API 기반 경기 일정을 안정적으로 동기화하고, 운영자 수동 수정과 충돌 없이 조회 정합성을 유지하는 것.

## 문제와 해결
1. 동기화 데이터가 늘어날수록 처리 시간이 길어짐  
- 문제: 리그/시즌/경기 수가 증가할수록 단일 처리 방식의 동기화 시간이 증가해 반영 지연 발생  
- 해결: Spring Batch 파티셔닝 기반 병렬 처리 적용

2. 수동 실행과 스케줄러 실행의 충돌  
- 문제: 동시 실행 시 중복 갱신/실패 응답/락 경합 가능  
- 해결: Redisson 글로벌 분산 락으로 수동/배치 실행 단일화

3. 조회 성능과 정합성을 동시에 만족해야 함  
- 문제: GET 트래픽은 높고 변경 빈도는 상대적으로 낮아 DB 부담이 큼. 반대로 변경 직후 stale 데이터는 허용 불가  
- 해결: TTL 기반 Redis 캐싱 + 동기화/수정/오버라이드/삭제 시 캐시 무효화

4. 개인 프로젝트 규모 대비 운영 복잡도가 과도함  
- 문제: FE/BE 분리 운영 + 별도 프록시/배포 관리가 프로젝트 규모에 비해 과해, 기능 개선보다 운영 유지에 시간을 많이 소모  
- 해결: 모놀리스 단일 배포 경로로 전환(Next.js 정적 산출물 + Spring Boot)해 운영 부담을 낮추고 기능 개선에 집중

5. 수동 등록 경기와 외부 API 경기가 중복 노출됨  
- 문제: 같은 경기라도 식별자 불일치로 2건 조회 가능  
- 해결: `match_external_mapping`으로 연결 정보를 관리하고, 조회 응답에서만 dedupe(표시 계층 병합) 적용  
- 원칙: 외부 동기화 `match_id`(외부 ID)는 치환하지 않고 원본 유지

## 결과
- 동기화 처리 시간 개선(히스토리 벤치마크): `92.5s -> 4.7s` (약 95% 단축)
- 수동/배치 동시 실행 충돌 제어
- 캐시 적용으로 반복 조회 시 DB 읽기 감소, 변경 이벤트에서는 정합성 유지
- FE/BE 분리 운영 대비 릴리즈/운영 동선 단순화 + 동일 출처 구성으로 CORS/쿠키 이슈 대응 부담 감소

## 기술 선택과 트레이드오프
- Spring Batch: 대량 동기화 작업(Job/Step/재시도/파티셔닝) 표준화 용이  
트레이드오프: 설정/운영 복잡도 증가
- Redisson 분산 락: 다중 인스턴스에서도 동시 실행 제어 가능  
트레이드오프: Redis 의존성 증가, 락 범위/TTL 설계 민감
- Redis 캐시: 조회 성능/DB 부하 개선에 효과적  
트레이드오프: 무효화 누락 시 stale 데이터 위험
- Railway: 개인 프로젝트 기준으로 서버/배포 운영 부담을 줄이고 빠르게 배포하기 용이  
트레이드오프: 사용량(CPU/메모리/트래픽) 증가 시 비용 상승폭이 커질 수 있음

## 기술 스택
- Backend: Java 17, Spring Boot 3, Spring Security, Spring Batch, Spring Data JPA
- Frontend: Next.js 15, React 19
- Data: PostgreSQL, Redis, Redisson
- Infra: Railway, Docker, GitHub Actions, Firebase Admin SDK

## 문서
- [docs/README.md](docs/README.md)
- [docs/erd.md](docs/erd.md)
- [docs/swagger-api-guide.md](docs/swagger-api-guide.md)
- [docs/report/optimization/summary.md](docs/report/optimization/summary.md)

## 로컬 실행
```bash
# 백엔드 실행
./gradlew bootRun -Dspring.profiles.active=dev

# 백엔드 빌드
./gradlew build

# 프론트 정적 산출물 빌드/복사
./gradlew copyFrontend

# 프론트 산출물 포함 빌드
./gradlew build -PwithFrontend
```

## 참고
- 성능 수치는 측정 시점/운영 환경에 따라 달라질 수 있습니다.
- 과거 운영 기록/원본 리포트는 `docs/archive/`에 보관합니다.
