# Stage 1: Build dependencies
FROM gradle:7.3-jdk17 AS build

WORKDIR /app

# Gradle Wrapper 파일과 Gradle 디렉토리를 복사
COPY gradlew ./
COPY gradle/ ./gradle/

# 소스 코드와 build.gradle 파일을 복사
COPY . ./

# Gradle Wrapper에 실행 권한 부여
RUN chmod +x gradlew

# 의존성 다운로드 및 빌드(테스트 포함)
RUN --mount=type=cache,target=/root/.gradle ./gradlew build --no-daemon
# 테스트 생략
#RUN --mount=type=cache,target=/root/.gradle ./gradlew build --no-daemon -x test

# Stage 2: Final runtime image
FROM eclipse-temurin:17-jre-alpine AS runtime

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
 && chown -R ${UID}:${UID} /app/logs
USER appuser

# JAR 파일을 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 실행 시 JAR 파일 실행
ENTRYPOINT [
"java",
"-Xms64m",
"-Xmx128m",
"-XX:MaxMetaspaceSize=128m",
"-XX:+UseSerialGC",
"-Dfile.encoding=UTF-8",
"-Duser.timezone=Asia/Seoul",
"-jar",
"app.jar"
]

EXPOSE 8080
