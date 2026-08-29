# Stage 1: Build dependencies
FROM gradle:8.12.1-jdk21 AS build

USER root

# Node.js 22.x 설치 (Next.js 프론트엔드 모놀리식 통합 빌드용)
RUN apt-get update && apt-get install -y curl \
  && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
  && apt-get install -y nodejs \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 1. Gradle 설정 파일들 복사 (캐싱용)
# Gradle Wrapper 파일과 Gradle 디렉토리를 복사
COPY gradlew ./
COPY gradle/ ./gradle/

# 2. 소스 코드 전체 복사 (★이게 먼저 와서 기존 파일들을 다 덮어써야 합니다)
# 소스 코드와 build.gradle 파일을 복사
COPY . ./

# 3. 모든 복사가 끝난 후, 최종적으로 실행 권한 부여 (★여기로 이동)
# Gradle Wrapper에 실행 권한 부여
RUN chmod +x gradlew

# 의존성 다운로드 및 빌드(테스트 미포함)
#RUN --mount=type=cache,target=/root/.gradle ./gradlew build --no-daemon --stacktrace --info -x test
# -PwithFrontend: Next.js 정적 산출물을 Spring static 경로로 복사 포함
RUN --mount=type=cache,target=/root/.gradle ./gradlew build -PwithFrontend --no-daemon -x test --stacktrace --info
#RUN --mount=type=cache,target=/root/.gradle ./gradlew build -PwithFrontend --no-daemon --stacktrace --info
# 테스트 생략
#RUN --mount=type=cache,target=/root/.gradle ./gradlew build --no-daemon -x test

# Stage 2: Final runtime image
FROM eclipse-temurin:21-jdk-alpine AS runtime

WORKDIR /app

# 로그 디렉토리 생성
RUN mkdir logs

# Create a non-privileged user & 로그 디렉토리 소유자 변경
ARG UID=10001
RUN adduser \
  --disabled-password \
  --gecos "" \
  --home "/nonexistent" \
  --shell "/sbin/nologin" \
  --no-create-home \
  --uid "${UID}" \
  appuser \
  && mkdir -p /app/logs /app/secrets \
  && chown -R ${UID}:${UID} /app/logs /app/secrets \
  && chmod 700 /app/secrets

# JAR 파일을 복사
COPY --from=build --chown=appuser:appuser /app/build/libs/*.jar app.jar

# 엔트리포인트 스크립트 복사 (JWT 개인키를 환경변수에서 복원한다)
COPY --chown=appuser:appuser docker-entrypoint.sh /app/docker-entrypoint.sh

# 파일에 실행 권한 추가 (보너스)
RUN chmod +x app.jar /app/docker-entrypoint.sh

# 사용자 전환
USER appuser

# 실행 중인 아티팩트를 밖에서 확인하기 위한 빌드 정보 (GET /version).
# 반드시 파일 끝, JAR 복사 뒤에 둔다. 앞쪽에 두면 커밋마다 값이 바뀌면서
# 그 아래 레이어 캐시가 전부 깨져 빌드가 느려진다.
ARG GIT_SHA=unknown
ARG IMAGE_TAG=unknown
ARG BUILT_AT=unknown
ENV APP_GIT_SHA=$GIT_SHA \
    APP_IMAGE_TAG=$IMAGE_TAG \
    APP_BUILT_AT=$BUILT_AT

# 엔트리포인트가 JWT_PRIVATE_KEY_B64를 디코드해 /app/secrets/app.key 로 복원한 뒤 앱을 띄운다.
# 키가 없으면 스크립트가 exit 1 로 죽는다 — 조용히 뜬 채 인증만 깨지는 것보다 낫다.
# (LF 형식 필요. .gitattributes 의 *.sh text eol=lf 가 보장한다)
ENTRYPOINT ["/app/docker-entrypoint.sh"]


EXPOSE 8080

