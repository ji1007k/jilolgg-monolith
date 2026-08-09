#!/bin/sh
# 컨테이너 기동 스크립트.
# JWT RSA 개인키는 이미지에 포함하지 않고 환경변수(Base64)로 받아 파일로 복원한다.
# 복원된 파일은 컨테이너 안에만 존재하며 이미지 레이어에는 남지 않는다.
set -e

KEY_PATH="${JWT_PRIVATE_KEY_PATH:-/app/secrets/app.key}"

if [ -n "$JWT_PRIVATE_KEY_B64" ]; then
  mkdir -p "$(dirname "$KEY_PATH")"
  echo "$JWT_PRIVATE_KEY_B64" | base64 -d > "$KEY_PATH"
  chmod 400 "$KEY_PATH"
elif [ ! -f "$KEY_PATH" ]; then
  # 키가 없으면 JWT 서명이 불가능해 어차피 기동에 실패한다.
  # 원인을 알기 어려운 스택트레이스 대신 여기서 명확히 알린다.
  echo "FATAL: JWT 개인키가 없습니다." >&2
  echo "  JWT_PRIVATE_KEY_B64 환경변수를 설정하거나 ${KEY_PATH} 에 키 파일을 두세요." >&2
  echo "  자세한 내용은 docs/secret-management.md 5절을 참고하세요." >&2
  exit 1
fi

exec java \
  -Xms128m \
  -Xmx256m \
  -XX:MaxMetaspaceSize=512m \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Seoul \
  -jar /app/app.jar
