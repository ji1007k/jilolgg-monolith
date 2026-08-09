[← 이전 페이지로 돌아가기](../README.md)

# 접속정보·비밀번호 외부화 설계

저장소에 평문으로 들어 있는 자격 증명을 코드 밖으로 빼내기 위한 설계다.
개인 프로젝트 규모에 맞춰 **의존성 추가 없이 Spring Boot 기본 기능만으로** 구성한다.

---

## 1. 원칙

**1) 코드에는 값이 없고 참조만 있다.**
설정 파일에는 `${ENV_VAR}` 형태의 참조만 남기고, 실제 값은 실행 환경이 주입한다.

**2) 기본값을 두지 않는다.**
```properties
# 안 됨 — 환경변수를 안 넣으면 조용히 운영 값으로 폴백한다
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:hopper.proxy.rlwy.net}

# 이렇게 — 미주입 시 부팅이 실패해 즉시 알아챌 수 있다
spring.data.redis.host=${SPRING_DATA_REDIS_HOST}
```
현재 `application-prod.properties`가 정확히 이 함정에 빠져 있다. 환경변수를 참조하지만 기본값 자리에 실제 운영 호스트가 박혀 있어 외부화의 의미가 없다.

다만 **비밀이 아닌 값**(포트, 커넥션 풀 크기, 파일 경로 같은 튜닝값)은 기본값을 둬도 된다. 기본값 금지는 자격 증명에만 적용한다.

**3) 시크릿이 없으면 뜨지 않는다.**
별도 검증 코드를 넣지 않는다. 원칙 2를 지키면 Spring이 알아서 `IllegalArgumentException`으로 부팅을 막아 주므로, 그것이 곧 검증 장치다.

---

## 2. 시크릿의 두 종류

이 프로젝트의 자격 증명은 처리 방식이 다른 두 부류로 나뉜다. 이걸 구분하지 않으면 파일형에서 막힌다.

| 종류 | 예 | 처리 |
| --- | --- | --- |
| **문자열형** | DB 비밀번호, Redis 비밀번호, API 키 | 환경변수에 그대로 |
| **파일형** | JWT RSA 개인키(`jwt/app.key`), Firebase 서비스계정 JSON | 줄바꿈 때문에 환경변수에 그대로 못 넣음 → **Base64로 인코딩해 주입하고 부팅 시 파일로 복원** |

Firebase는 이미 이 패턴이 적용돼 있다 (`FirebaseConfig`가 `firebase.credentials.json` 문자열과 `firebase.credentials.path` 경로를 모두 지원). **JWT 키도 같은 방식으로 맞추면 된다.**

---

## 3. 환경별 주입 경로

| 환경 | 주입 방법 | 비고 |
| --- | --- | --- |
| **로컬 개발** | `./config/application-local.properties` (gitignore) | Spring Boot가 `./config/` 디렉터리를 자동으로 읽는다. jar에 포함되지 않는다. |
| **테스트** | 시크릿 불필요하게 만드는 것이 목표 (5절) | |
| **CI (GitHub Actions)** | Repository Secrets | 현재 워크플로는 빌드만 하고 테스트를 건너뛰어 시크릿이 필요 없다 |
| **운영 (Railway)** | 서비스 환경변수 | Spring의 relaxed binding이 `SPRING_DATASOURCE_PASSWORD` → `spring.datasource.password`로 자동 매핑 |

### 로컬 설정 파일

`config/application-local.properties`를 만들고 `local` 프로필을 얹어 실행한다.

```bash
./gradlew bootRun -Dspring.profiles.active=dev,local
```

`config/application-local.properties.example`를 커밋해 두면 새로 클론했을 때 무엇을 채워야 하는지 알 수 있다. `.gitignore`에는 `config/application-local.properties`만 추가한다.

이 방식을 고른 이유: `.env` 파일을 쓰려면 플러그인이나 의존성이 필요한데, `./config/` 디렉터리는 Spring Boot가 기본으로 지원한다. 추가 비용이 0이다.

---

## 4. 항목별 매핑

| 현재 위치 | 종류 | 환경변수 | 조치 |
| --- | --- | --- | --- |
| `application-prod.properties` datasource.url | 접속정보 | `SPRING_DATASOURCE_URL` | 기본값 제거 |
| 〃 datasource.username | 접속정보 | `SPRING_DATASOURCE_USERNAME` | 기본값 제거 |
| 〃 datasource.password | 시크릿 | `SPRING_DATASOURCE_PASSWORD` | 기본값 제거 + 회전 |
| 〃 redis host/port/password | 시크릿 | `SPRING_DATA_REDIS_HOST` / `_PORT` / `_PASSWORD` | 기본값 제거 + 회전 |
| `application.yml` lol.esports.api.key | 시크릿 | `LOL_ESPORTS_API_KEY` | 재발급 |
| `src/test/resources/application.yml` riot api key | 시크릿 | `LOL_RIOT_API_KEY` | 재발급 |
| `jwt/app.key` | **파일형** | `JWT_PRIVATE_KEY_B64` | 5절 참조 + 키페어 재생성 |
| `jwt/app.pub` | 공개키 | — | 비밀이 아니므로 classpath 유지 |
| Firebase adminsdk JSON | **파일형** | `FIREBASE_CREDENTIALS_JSON` | 이미 지원됨. 커밋된 적 없어 안전 |
| `compose.yaml` RDS 접속정보 | 시크릿 | — | EC2 시절 잔재. **파일째 삭제 권장** |
| `db/postgres/init.sql` 비밀번호 | 시크릿 | — | `psql -v` 변수로 주입 |
| `db/postgres/password.txt` | 시크릿 | — | 용도 불명. **삭제** |
| `docs/archive/notes/MEMO.txt:111` | 시크릿 | — | `<password>`로 치환 |
| `application-dev.properties` localhost 계정 | 로컬 전용 | — | 비밀 아님. 유지하되 로컬 전용임을 주석으로 명시 |

---

## 5. 파일형 시크릿: JWT RSA 개인키

가장 손이 많이 가는 항목이다. `SecurityConfig`가 키를 **리소스 위치**로 주입받기 때문이다.

```java
@Value("${jwt.private.key}")
RSAPrivateKey priv;   // 값이 아니라 classpath:/file: 경로를 받는다
```

### 설계

위치는 설정 가능하게 두고, 내용은 환경변수로 주입해 컨테이너 기동 시 파일로 복원한다.

```properties
# 위치는 비밀이 아니므로 기본값을 둬도 된다 (로컬 개발 기본값)
jwt.private.key=${JWT_PRIVATE_KEY_LOCATION:file:./secrets/app.key}
jwt.public.key=classpath:jwt/app.pub
```

**로컬** — `./secrets/app.key`에 키 파일을 두고 `.gitignore`에 `secrets/` 추가. 기본값이 이 경로라 별도 설정이 필요 없다.

**운영(Railway)** — Base64 문자열을 환경변수로 넣고 엔트리포인트에서 파일로 되돌린다.

- `JWT_PRIVATE_KEY_B64` : `base64 -w0 app.key` 결과
- `JWT_PRIVATE_KEY_LOCATION` : `file:/app/secrets/app.key`

Dockerfile의 `ENTRYPOINT`를 스크립트로 바꾼다.

```sh
#!/bin/sh
# docker-entrypoint.sh
set -e

if [ -n "$JWT_PRIVATE_KEY_B64" ]; then
  mkdir -p /app/secrets
  echo "$JWT_PRIVATE_KEY_B64" | base64 -d > /app/secrets/app.key
  chmod 400 /app/secrets/app.key
fi

exec java \
  -Xms128m -Xmx256m -XX:MaxMetaspaceSize=512m \
  -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul \
  -jar /app/app.jar
```

복원 파일은 컨테이너 안에만 있고 이미지 레이어에는 남지 않는다.

### 키 재생성 시 주의

RSA 키페어를 새로 만들면 **기존에 발급된 access/refresh 토큰이 전부 무효화된다.** 전 사용자 재로그인이 필요하다. 개인 프로젝트라 영향은 작지만, 로그아웃 처리 흐름이 정상 동작하는지는 확인할 것.

---

## 6. 테스트 환경

현재 `src/test/resources/application.yml`이 **원격 Redis에 실제로 접속**하기 때문에 테스트 설정 파일에 운영과 같은 자격 증명이 들어가 있다. 값을 환경변수로 빼는 것보다, **자격 증명이 필요 없는 구조로 바꾸는 편**이 근본적이다.

- 1순위: Testcontainers로 Redis를 띄운다. 자격 증명 자체가 사라지고, 테스트가 네트워크에 의존하지 않게 되는 부수 효과도 크다.
- 차선: 최소한 환경변수로 빼고 기본값을 제거한다.

같은 이유로 Riot/Esports API 키도 테스트에서는 실제 호출 대신 스텁으로 대체하는 것이 맞다.

---

## 7. 적용 순서

자격 증명 재발급이 **항상 먼저**다. 파일을 지우는 것은 이미 유출된 값을 무효화해 주지 않는다.

1. **키·비밀번호 전량 재발급** — JWT 키페어, Railway DB/Redis 비밀번호, API 키. 이 단계만 끝나면 유출된 값은 무의미해진다.
2. **저장소 임시 private 전환** — 정리가 끝날 때까지
3. **설정 파일에서 값 제거** — 4절 매핑표대로. **fallback 자리까지 비울 것**
4. **`.gitignore` 보강 후 추적 해제** — `.gitignore` 추가만으로는 이미 추적 중인 파일이 빠지지 않는다. `git rm --cached` 필요
5. **히스토리 재작성** — `git filter-repo`. remote가 `origin`/`upstream` 둘이므로 양쪽 모두 force push
6. **재발 방지 장치** — 아래

---

## 8. 재발 방지

- **GitHub Push Protection 활성화** (Settings → Code security). 커밋 단계에서 자격 증명을 차단한다. 무료 저장소도 지원된다.
- **기본값 금지 원칙 유지** — 새 설정을 추가할 때 습관적으로 `${VAR:실제값}`을 쓰지 않는다.
- **테스트를 원격 인프라에 의존시키지 않기** — 의존이 없으면 테스트 설정에 자격 증명이 들어갈 이유도 없다.

---

## 부록: `.gitignore` 추가 항목

```gitignore
### Secrets ###
config/application-local.properties
secrets/
src/main/resources/jwt/app.key
src/main/resources/https/*-key.pem
src/main/resources/db/postgres/password.txt
```
