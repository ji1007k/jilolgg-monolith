# Stage 1: Build dependencies (Ubuntu 기반)
FROM gradle:7.3-jdk17 AS build

USER root

# Node.js 22.x 설치
RUN apt-get update && apt-get install -y curl \
  && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
  && apt-get install -y nodejs \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# [최적화] 소스 전체를 복사하기 전에 Gradle 파일만 먼저 복사해서 의존성 캐싱 효율 극대화
COPY gradlew ./
COPY gradle/ ./gradle/
COPY build.gradle settings.gradle ./

# [선택] 프론트엔드 의존성 파일도 미리 복사하면 캐싱에 유리합니다.
# COPY frontend/package*.json ./frontend/ (구조에 맞게 조정 필요)

RUN chmod +x gradlew

# 소스 코드 전체 복사
COPY . ./

# [수정] 이전 질문의 연장선으로, 테스트를 제외하고 빌드하려면 아래 줄의 주석을 해제하세요.
RUN --mount=type=cache,target=/root/.gradle ./gradlew build -PwithFrontend --no-daemon -x test --stacktrace --info
# RUN --mount=type=cache,target=/root/.gradle ./gradlew build -PwithFrontend --no-daemon --stacktrace --info


# Stage 2: Final runtime image (OS 호환성을 위해 Ubuntu 기반 JRE 사용)
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# 로그 디렉토리 생성 및 권한 설정용 유저 생성 (Ubuntu/Debian 방식)
ARG UID=10001
RUN useradd \
  --user-group \
  --system \
  --home-dir /nonexistent \
  --no-create-home \
  --shell /bin/false \
  --uid "${UID}" \
  appuser \
  && mkdir -p /app/logs \
  && chown -R ${UID}:${UID} /app/logs

# [수정] plain.jar를 제외하고 실제 실행 가능한 Boot JAR만 정확히 복사하도록 매칭
# (일반적으로 plain이 안 붙은 파일이 실행 가능한 JAR입니다)
COPY --from=build --chown=appuser:appuser /app/build/libs/*[!p][!l][!a][!i][!n].jar app.jar

# USER 전환 후 실행
USER appuser

ENTRYPOINT exec java \
  -Xms128m \
  -Xmx256m \
  -XX:MaxMetaspaceSize=512m \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Seoul \
  -jar app.jar

EXPOSE 8080