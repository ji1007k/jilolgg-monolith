# 동기화 병렬 처리 현황 및 개선 계획

## 1) 현재 상태 요약
- 배치 병렬 처리: `Spring Batch Partitioning` + `gridSize=5` 적용
- 배치 실행 스레드풀: `limitedTaskExecutor(core=10, max=20, queue=30)` 적용
- 동기화 중복 실행 제어: `Redisson 글로벌 락` 적용
- DB 커넥션 풀(운영): `Hikari maxPoolSize=10`
- 외부 API 호출: `WebClient` 사용, 일부 timeout 설정 존재

## 2) 현재 구조의 한계
- 스레드풀 분리는 되어 있지만, DB 풀/웹 트래픽/외부 API 한도를 통합한 동시성 제어는 미흡
- 외부 API 호출 경로에서 `.block()` 사용으로 워커 스레드 점유 시간이 길어질 수 있음
- 429/5xx 증가 시 자동 감속(백오프/동시성 축소) 정책이 명시적으로 없음
- Job launcher와 배치 Step이 동일 executor를 공유해 완전한 실행 계층 분리는 아님

## 3) 리스크 정리
- DB: 커넥션 풀 고갈, 응답 지연 증가
- 외부 API: rate limit(429), timeout 증가
- 실행 안정성: 큐 적체/스레드 고갈 시 전체 동기화 지연

## 4) 개선 우선순위

### P1. 동시성 상한 재설계 (가장 먼저)
- 원칙: `partition = min(리그 수, DB 허용 동시성, API 허용 동시성, CPU 한계)`
- `hikari.maxPoolSize`에서 웹 트래픽 예약분을 제외한 값으로 배치 동시성 상한 설정
- executor 풀 크기와 `gridSize`를 같은 기준으로 재조정

### P2. 외부 API 보호 장치 추가
- API 호출 경로에 429/5xx 재시도 + 지수 백오프 정책 적용
- per-provider 동시 호출 제한(세마포어/버크헤드) 적용
- timeout/재시도 실패율 기반 감속(동시성 축소) 규칙 추가

### P3. 실행 계층 분리 강화
- `jobLauncher`용 executor와 `partition step`용 executor 분리
- 배치 작업 전용 노드(프로필) 운영 시 스케줄러/워커 역할 분리 검토

### P4. 관측성(Observability) 보강
- 최소 지표: 배치 총 시간, step 실패율, DB active/pending connection, 429 비율, timeout 비율, executor queue length
- 기준 초과 시 경고 알림(로그 기반 또는 외부 알림 채널) 연결

## 5) 검증 기준 (Acceptance)
- 배치 실행 중에도 API p95 응답시간이 허용 범위 내 유지
- DB pending connection이 임계치 이상 장시간 지속되지 않음
- 429/timeout 비율이 기준치 이하 유지
- 동일 데이터셋 기준 배치 완료 시간이 안정적으로 재현됨

## 6) 면접용 한 줄
“현재는 파티션과 스레드풀 제한으로 1차 안정화를 했고, 다음 단계로 DB/API 한도를 포함한 동시성 제어와 관측 지표 기반 자동 튜닝으로 확장 안정성을 높일 계획입니다.”

