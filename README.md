# JILoL.gg

> 외부 API 동기화 파이프라인을 성능/안정성 중심으로 개선한 백엔드 포트폴리오 프로젝트

- 서비스: [https://jilolgg.up.railway.app/jikimi](https://jilolgg.up.railway.app/jikimi)
- 저장소: [https://github.com/ji1007k/jilolgg-monolith](https://github.com/ji1007k/jilolgg-monolith)

---

## 프로젝트 개요
- LoL Esports 외부 API 기반 경기 일정/팀/리그 데이터를 수집하고 제공하는 서비스입니다.
- 단순 기능 추가보다 동기화 처리의 안정성, 중복 실행 방지, 캐시 일관성, 운영 편의성에 집중했습니다.

## 핵심 개선 포인트
1. 동기화 병렬 처리 최적화
- Spring Batch 파티셔닝 기반으로 대량 데이터 동기화 시간을 단축했습니다.

2. 중복 실행 방지
- Redisson 분산 락으로 수동/배치 동기화의 동시 실행을 제어했습니다.

3. 캐시 일관성 강화
- 동기화/수정/오버라이드 처리 후 캐시 무효화를 적용해 조회 정합성을 맞췄습니다.

4. 운영 단순화
- Railway 환경에 맞춰 운영 동선을 단순화하고, 문서/아카이브를 분리 정리했습니다.

---

## 기술 스택
- Backend: Java 17, Spring Boot 3, Spring Security, Spring Batch, Spring Data JPA
- Frontend: Next.js 15, React 19
- Data: PostgreSQL, Redis, Redisson
- Infra: Railway, Docker, GitHub Actions, Firebase Admin SDK

---

## 문서
- 문서 인덱스: `docs/README.md`
- 아키텍처: `docs/architecture.md`
- ERD: `docs/erd.md`
- Swagger 운영 가이드: `docs/swagger-api-guide.md`
- 최적화 요약: `docs/report/optimization/summary.md`

---

## 실행 가이드
1. 백엔드 실행
```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

2. 백엔드 빌드
```bash
./gradlew build
```

3. 프론트 정적 산출물 빌드/복사 (필요 시)
```bash
./gradlew copyFrontend
```

4. 프론트 산출물 포함 백엔드 빌드 (옵션)
```bash
./gradlew build -PwithFrontend
```

---

## 참고
- 성능 수치는 측정 시점/운영 환경에 따라 달라질 수 있습니다.
- 과거 운영 기록과 실험 원본은 `docs/archive/`에 보관합니다.
