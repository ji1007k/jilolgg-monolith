# 로컬 실행 가이드

클론 직후부터 앱이 뜰 때까지의 절차.

## 사전 준비

| 항목 | 버전 | 비고 |
| --- | --- | --- |
| JDK | 21 | |
| Node.js | 22 | 프론트엔드를 함께 빌드할 때만 |
| PostgreSQL | 16+ | `localhost:5432` |
| Redis | 7+ | `localhost:6379` |

### DB와 계정 생성

접속 정보는 `src/main/resources/application-dev.properties`에 있다.

```bash
psql -U postgres -c "CREATE USER jikim WITH PASSWORD 'jikim';"
psql -U postgres -c "CREATE DATABASE basic OWNER jikim;"
```

`schema.sql`에 `alter table ... owner to jikim` 구문이 있어 **계정 이름이 `jikim`이어야** 한다.
다른 이름을 쓰려면 그 구문들도 함께 바꿔야 한다.

### Redis

도커로 띄워도 된다.

```bash
docker run -d --name jilolgg-redis -p 6379:6379 redis:7
```

## JWT 키 준비

JWT 서명용 RSA 키페어가 `src/main/resources/jwt/`에 있어야 한다.
저장소에 개인키가 포함돼 있지 않다면 직접 만든다.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/jwt/app.key
openssl rsa -in src/main/resources/jwt/app.key -pubout -out src/main/resources/jwt/app.pub
```

개인키를 새로 만들면 **공개키도 반드시 함께 갱신**해야 한다. 짝이 맞지 않으면
로그인은 되지만 이후 모든 요청이 401이 된다.

운영 환경에서의 주입 방식은 [시크릿 관리 설계](secret-management.md) 참조.

## 백엔드 실행

```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

- 프로필 기본값이 **prod**(`application.yml`의 `${SPRING_PROFILES_ACTIVE:prod}`)이므로 `dev` 지정이 필수다.
  빠뜨리면 DataSource 설정이 없어 기동에 실패한다
- 위 `-D` 옵션이 동작하는 것은 `build.gradle`의 `tasks.named('bootRun')` 블록이
  `spring.*` 시스템 프로퍼티를 앱 JVM으로 전달하기 때문이다. **이 블록을 지우면 조용히 prod로 뜬다.**
  환경변수 `SPRING_PROFILES_ACTIVE=dev`도 같은 효과다
- 테이블 생성과 시드 데이터 주입은 기동 시 `spring.sql.init`이 `db/postgres/schema.sql`과
  `data.sql`을 실행해 자동으로 처리한다. 별도 마이그레이션 명령이 없다

### 확인

```bash
curl http://localhost:8080/csrf
```

`200`이면 정상. Swagger UI는 <http://localhost:8080/api/swagger-ui>.

## 프론트엔드 실행 (선택)

백엔드만 띄워도 API는 전부 동작한다. 프론트를 따로 개발할 때만 필요하다.

```bash
cd frontend
cp .env.example .env.development
npm install
npm run nextdev
```

- `npm run nextdev`는 순수 Next dev 서버다. **이쪽을 먼저 써보길 권한다**
- `npm run dev`는 Express 커스텀 서버를 띄우며 기본값이 `USE_HTTPS=true`라
  `src/config/https/`에 로컬 인증서가 필요하다. 저장소에 인증서를 포함하지 않으므로 직접 만든다

  ```bash
  mkcert -key-file src/config/https/localhost-key.pem -cert-file src/config/https/localhost.pem localhost
  ```

`output: 'export'` 설정이라 Next dev 서버는 `/api`를 프록시하지 않는다.
프론트에서 백엔드 API를 함께 쓰려면 `npm run dev`(Express 프록시)를 쓰거나,
아래처럼 빌드해서 백엔드가 서빙하게 한다.

## 모놀리스로 한 번에 빌드

```bash
./gradlew build -PwithFrontend
```

`-PwithFrontend`가 있을 때만 `processResources`가 `copyFrontend`에 의존해
`frontend/out`을 `src/main/resources/static/jikimi`로 복사한다. Docker 빌드도 이 플래그를 쓴다.
백엔드만 빌드하려면 `./gradlew build`.

빌드 후 <http://localhost:8080/jikimi> 에서 프론트가 서빙된다.

## 테스트

```bash
./gradlew test
```

- 테스트 DB는 H2 in-memory(`MODE=PostgreSQL`)라 로컬 PostgreSQL이 없어도 된다
- **Redis는 mock이 아니라 실제 인스턴스에 연결한다.** 기본값이 `localhost:6379`이므로
  로컬에 Redis가 떠 있어야 한다. **기본값을 원격으로 되돌리지 말 것** —
  테스트마다 운영 Redis에 붙어 요금이 나가고 결과가 네트워크에 흔들린다
- 외부 esports API를 실제로 호출하는 테스트는 `-PexcludeExternalApiTests`로 제외할 수 있다.
  CI가 이 옵션을 쓴다

```bash
./gradlew test -PexcludeExternalApiTests
```

## 자주 걸리는 것

| 증상 | 원인 |
| --- | --- |
| 기동 시 DataSource 오류 | `dev` 프로필 미지정 |
| 로그인은 되는데 이후 요청이 전부 401 | JWT 개인키와 공개키의 짝이 안 맞음 |
| `Unable to delete directory ...build\test-results` | Windows 파일 잠금. `./gradlew --stop` 후 재실행 |
| 프론트 변경이 화면에 안 보임 | 브라우저 캐시. 강제 새로고침 |
| Linux CI에서 `./gradlew: Permission denied` | 실행 비트 누락. `git update-index --chmod=+x gradlew` |
