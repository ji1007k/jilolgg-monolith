# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

JILoL.gg — LoL Esports 외부 API에서 경기/팀/리그 데이터를 동기화해 제공하는 **모놀리스**. Spring Boot 3.5(Java 21) 백엔드가 Next.js 정적 빌드 산출물을 `/jikimi` 경로로 직접 서빙한다. 배포는 Railway, 저장소는 PostgreSQL + Redis.

Gradle root project 이름은 `basic`이고 Java 패키지도 `com.test.basic`이다(서비스명 `jilolgg`와 다름 — 정상).

## 명령어

모든 명령은 이 디렉터리(`jilolgg/`) 기준. Windows에서는 `gradlew.bat`.

```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

- 프로필 기본값이 **prod**(`application.yml`의 `${SPRING_PROFILES_ACTIVE:prod}`)이므로 로컬 실행 시 `dev` 지정 필수. dev는 `localhost:5432/basic` PostgreSQL과 `localhost:6379` Redis를 요구한다.
- 위 `-D` 옵션이 동작하는 것은 `build.gradle`의 `tasks.named('bootRun')` 블록이 `spring.*` 시스템 프로퍼티를 앱 JVM으로 전달하기 때문이다. **이 블록을 지우면 명령이 조용히 prod로 뜨면서 DataSource 오류로 죽는다.** bootRun은 앱을 별도 JVM으로 띄우므로 Gradle JVM의 `-D`가 그냥은 전달되지 않는다. 환경변수 `SPRING_PROFILES_ACTIVE=dev`나 `--args='--spring.profiles.active=dev'`도 같은 효과다.
- `./gradlew build` — 백엔드만 빌드(프론트 제외).
- `./gradlew build -PwithFrontend` — `-PwithFrontend`가 있을 때만 `processResources`가 `copyFrontend`에 의존한다. Docker 빌드도 이 플래그를 쓴다.
- `./gradlew copyFrontend` — `npm run build` 실행 후 `frontend/out` → `src/main/resources/static/jikimi`로 복사. `node_modules`가 이미 있으면 `npmInstall`은 스킵된다.
- `./gradlew test` — JUnit 5. CPU 코어의 절반으로 병렬 실행(`maxParallelForks`).
- `./gradlew test --tests "com.test.basic.lol.match.MatchServiceTest"` — 단일 테스트 클래스. 메서드 단위는 `--tests "*.MatchServiceTest.testGetTodaysMatchesByLeagueId"`.

프론트엔드 단독 개발(`frontend/`):

```bash
npm run dev
```

- `dev`는 Next dev 서버가 아니라 **Express 커스텀 서버**(`src/server/server.js`)를 띄운다. `.env.local` → `.env.<NODE_ENV>` 순으로 로드하며 기본값이 `USE_HTTPS=true`라 `src/config/https/`의 로컬 인증서가 필요하다.
- `npm run nextdev` — 프록시 없이 순수 Next dev 서버(turbopack).
- `npm run lint` — ESLint(`eslint-config-next`).

## 아키텍처

### 요청 경로와 `/api` prefix

프론트엔드는 **모든** API 요청에 `/api` prefix를 붙인다. 이를 벗겨내는 지점이 환경마다 다르다:

- **로컬 분리 실행**: Express 프록시(`server.js`)가 `pathRewrite`로 `/api`를 제거하고 백엔드로 전달. 개발 환경에서는 `onProxyRes`로 백엔드의 `Set-Cookie`를 Express가 대신 내려보내 same-origin 쿠키로 변환한다(cross-origin 쿠키 저장 실패 우회).
- **모놀리스 배포**: `ApiPrefixFilter`(`Ordered.HIGHEST_PRECEDENCE`)가 `/api/**`를 `RequestDispatcher.forward`로 내부 경로에 넘긴다. 단 `/api/swagger-ui`, `/api/v3/api-docs`는 그대로 통과.

따라서 **컨트롤러 매핑에는 `/api`를 쓰지 않는다** (`/lol/matches`, `/auth/login` …). Swagger 경로만 예외적으로 `/api`를 포함한다.

`spring.mvc.static-path-pattern: /**` + Next의 `basePath: '/jikimi'` 조합으로 프론트가 `/jikimi/**`에서 서빙된다.

### 인증

무상태 JWT (RSA 키페어 `classpath:jwt/app.key`/`app.pub`, `SecurityConfig`의 `NimbusJwtEncoder`/`Decoder`). 토큰은 httpOnly 쿠키로 전달되고 `CustomJwtFilter`가 `UsernamePasswordAuthenticationFilter` 앞에서 검증한다.

세션 없는 CSRF: `CookieCsrfTokenRepository.withHttpOnlyFalse()`로 `XSRF-TOKEN` 쿠키를 내려주고 클라이언트가 `X-XSRF-TOKEN` 헤더로 되돌려 보내면 서버가 일치만 검사한다. 쿠키의 `secure`/`sameSite`는 프로필별 `cookie.secure`, `cookie.same-site` 값으로 결정된다(dev: `false`/`Lax`).

`csrfRequireMatcher`가 `X-From-Swagger: skip` 헤더를 CSRF 우회로 인정한다 — Swagger 테스트용이며 운영 노출에 주의.

권한은 `SecurityConfig`의 `authorizeHttpRequests`와 컨트롤러의 `@PreAuthorize`가 **이중으로** 걸려 있다. 엔드포인트 권한을 바꿀 때 양쪽 모두 확인할 것.

### 동기화 파이프라인 (핵심)

외부 `esports-api.lolesports.com` → 정제 → DB 저장. 진입점은 모두 `MatchSyncOrchestratorService`로 수렴한다:

- `runManualLeagueSync(year)` — 운영자 수동 전체 갱신 + 캐시 무효화
- `runBatchYearSync(year)` — Spring Batch `syncMatchJob` 실행
- `runTodaySync(today)` — 금일 경기만 갱신(캐시 무효화는 부하 때문에 의도적으로 비활성)

세 경로 모두 `runWithGlobalLock`을 거친다. Redisson `RLock`(`sync:matches:global`, `tryLock(1s)`)로 스케줄러와 수동 실행의 동시 실행을 막고, 실패 시 예외 대신 `SyncExecutionResult(success, lockAcquired, message, elapsedMs)`를 반환한다. **새 동기화 경로를 추가할 때도 이 오케스트레이터를 통해야 한다.**

호출자: `SyncLolEsportsSchedulerProd`/`Dev`(`@Profile`로 분리), `JobTriggerController`(`GET /lol/batch/run-match-job?year=`, ADMIN).

### Spring Batch

`MatchBatchConfig`에 Job/Step/Reader/Processor/Writer가 모두 정의되어 있다.

- `syncMatchJob` → `syncMatchStep`(마스터, 파티셔닝 전담) → `partitionedMatchStep`(chunk 100)
- `LeaguePartitioner`가 리그 단위로 분할하고 `gridSize=5` 고정, `limitedTaskExecutor`(core 10 / max 20 / queue 30)로 병렬 실행. 이 구조로 동기화 시간을 92.5s → 4.7s로 줄였다.
- Reader/Processor/Writer는 모두 `@StepScope` — 파티션마다 독립 인스턴스가 생성되어야 thread-safe하다. `MatchItemReader`는 `stepExecutionContext['leagueId']`, `['targetYear']`를 주입받는다.
- `faultTolerant`: retry 3회, `DataIntegrityViolationException`/`ConstraintViolationException`은 skip(limit 3).
- `asyncJobLauncher`는 `TaskExecutorJobLauncher` — Job을 비동기로 띄워야 분산락이 실제로 동작한다(동기 실행 시 Spring Batch 레벨 동기화가 먼저 걸려 락에 도달하지 못함). 대신 API 응답으로 Job 결과를 받을 수 없다.
- 배치 메타 테이블: dev/prod는 `batchDataSourceInitializer`가 `schema-postgresql.sql`을 `continueOnError`로 실행, 테스트는 H2 `initialize-schema: embedded`. `spring.batch.job.enabled=false`로 부팅 시 자동 실행은 꺼져 있다.

### 캐싱

`MatchCacheService`가 Redis 캐시(`match:leagueId:startDate:endDate` 키, TTL 10분)와 프로세스 내 메모리 캐시를 함께 관리한다. 데이터 변경 시 `invalidateAllCaches()`가 `match:*` 키 삭제 + 메모리 캐시 초기화를 **둘 다** 수행한다. 캐시를 건드리는 쓰기 경로를 추가하면 무효화 누락으로 stale 데이터가 노출되므로 반드시 이 지점을 함께 확인할 것.

Redisson은 `RedissonAutoConfigurationV2`를 `spring.autoconfigure.exclude`로 제외하고 `RedissonConfig`에서 직접 구성한다(캐시 자동설정만 비활성화, 락 기능은 유지).

### 수동 데이터 / 외부 매핑

운영자가 직접 입력한 경기와 외부 API 경기가 식별자 차이로 중복 노출되는 문제를 `match_external_mapping`(`MatchExternalMapping*`)으로 해결한다. 원칙: **외부 API 원본 `matches.match_id`는 절대 치환·삭제하지 않고**, 연결 정보만 저장한 뒤 조회 레이어에서 dedupe(표시 계층 병합)한다. 관련 API는 `/admin/manual-matches/**`, `/admin/match-overrides/**` (`docs/swagger-api-guide.md` 5~6절 참조).

### 패키지 구조

`com.test.basic` 아래 도메인별로 나뉜다: `auth`(jwt/csrf/security), `lol`(api·batch·domain·sync), `notification`(FCM), `post`, `user`, `common`(config·handler·utils), `config`(WebConfig, ApiPrefixFilter).

`lol/domain/<엔티티>` 패키지는 Entity / Repository / Service / Controller / Dto / Mapper(MapStruct)를 한곳에 모으는 패키지-바이-피처 스타일이다. 관심사가 갈리면 접미사로 분리한다 — 예: `MatchService`(조회/영속), `MatchApiService`(외부 API 호출), `MatchCacheService`(캐시), `SyncMatchService`/`MatchSyncWorker`(동기화). 새 코드도 이 규칙을 따를 것.

## 테스트

- 단위 테스트는 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`, 통합 테스트는 `src/test/resources/application.yml` 기준. 스프링 빈 대체는 `@MockitoBean`(`org.springframework.test.context.bean.override.mockito`) — `@MockBean`은 Boot 4에서 제거되므로 쓰지 말 것.
- `.github/workflows/build.yml`과 Dockerfile이 `-x test`로 빌드하므로 **CI는 테스트를 돌리지 않는다.** 깨진 테스트가 오래 방치될 수 있으니 로컬에서 직접 확인할 것.
- 테스트를 건드릴 때 반복해서 걸린 함정들:
  - `@WebMvcTest`에 `AuthController`를 포함하면 `RefreshTokenService`/`RefreshTokenRepository` mock이 필요하고, `createRefreshToken`도 stub해야 한다(안 하면 null 반환 → 로그인 500).
  - `@Transactional` 롤백은 identity 시퀀스를 되돌리지 않는다. 그래서 `db/h2/user.sql`은 id를 1001~1003으로 **명시**하고, `AuthTestSupport.ADMIN_USER_ID`가 이 값과 짝을 이룬다. 둘 중 하나만 바꾸면 깨진다.
  - `webEnvironment = RANDOM_PORT` 테스트는 서버가 별도 트랜잭션에서 돌기 때문에 `@Sql`을 `SqlConfig.TransactionMode.ISOLATED`로 커밋시켜야 서버가 시드를 본다.
  - `@DataJpaTest`는 데이터소스를 자체 임베디드 DB로 갈아끼운다. 이 프로젝트의 H2 설정(PostgreSQL 모드)을 쓰려면 `@AutoConfigureTestDatabase(replace = NONE)`가 필요하다.
  - 테스트 설정의 `globally_quoted_identifiers: true`는 `@Column(columnDefinition = ...)`의 타입명까지 따옴표로 감싼다. 엔티티에 `columnDefinition`을 쓰면 H2 DDL이 조용히 실패해 해당 테이블만 사라진다.
- 테스트 DB는 **H2 in-memory**(`MODE=PostgreSQL`, `ddl-auto: create-drop`, `defer-datasource-initialization: true`)이고 `src/test/resources/db/h2/*.sql`이 시드 데이터를 넣는다. 프로덕션 스키마는 `src/main/resources/db/postgres/schema.sql`이므로 스키마 변경 시 양쪽을 맞춰야 한다.
- Redis는 mock이 아니라 **실제 인스턴스에 연결**한다. 기본값은 `localhost:6379`이므로 로컬에 Redis가 떠 있어야 한다(`SPRING_DATA_REDIS_HOST`/`_PORT`/`_PASSWORD`로 오버라이드). **기본값을 원격으로 되돌리지 말 것** — 테스트마다 운영 Redis에 붙어 요금이 나가고 결과가 네트워크에 흔들린다.
- 외부 esports API를 실제로 호출하는 테스트(`LolEsportsApiClientIntergrationTest`, `SyncMatchJobTest`)는 `-PexcludeExternalApiTests`로 제외할 수 있다. CI가 이 옵션을 쓴다.
- 인증이 필요한 테스트는 `common/support/AuthTestSupport`, `AuthRestTemplateTestSupport`와 `common/fixture/UserFixture`를 활용한다.

## 빌드/배포 흐름

`Dockerfile`이 멀티스테이지로 Node 22 설치 → `./gradlew build -PwithFrontend -x test`(테스트 생략) → `eclipse-temurin:21-jdk-alpine` 런타임. `jar { enabled = false }`로 plain jar 생성을 막아 `build/libs/*.jar`가 하나만 남게 되어 있다(COPY 와일드카드가 이에 의존).

- `.github/workflows/build.yml` — main PR 시 GHCR에 이미지 빌드/푸시.
- `.github/workflows/deploy-railway.yml` — main push 시 `railway redeploy` 트리거(이미지가 GHCR에 이미 있다고 가정, 체크아웃/빌드 없음).

컨테이너 JVM은 `-Xmx256m`로 제한되어 있다. 메모리를 크게 쓰는 변경은 이 한도를 고려할 것.

## 전담 에이전트

기능 영역별 서브에이전트가 `.claude/agents/`에 있다. 해당 영역 작업은 거기로 위임한다.

- `match-sync` — 동기화/배치 파이프라인(batch, sync, MatchSyncWorker, 스케줄러, 캐시 무효화 연동)

작성 규칙은 `docs/agent-authoring-guide.md`.

## 컨벤션

커밋 메시지 라벨(`docs/commit-convention.md`): `feat`, `fix`, `refactor`, `chore`, `test`, `docs`.

문서는 `docs/` 루트에 현행 문서만 두고 과거 기록은 `docs/archive/`로 옮긴다. `docs/README.md`가 색인이다. `docs/architecture.md`는 **레거시**(AWS EC2 + Nginx + WebSocket) 기록이므로 현재 구조는 루트 `README.md`를 기준으로 볼 것 — WebSocket/채팅 기능은 제거되었고 FCM 푸시로 대체되었다.

주석과 로그 메시지는 한국어로 작성되어 있다.
