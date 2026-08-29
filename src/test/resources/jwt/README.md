# 테스트 전용 키페어

여기 있는 `app.key` / `app.pub`은 **테스트에서만 쓰는 일회용 키**다.
`src/test/resources/application.yml`의 `jwt.private.key: classpath:jwt/app.key`가 이걸 참조한다.

- **운영에서는 절대 쓰이지 않는다.** 운영 개인키는 저장소에 없고,
  `docker-entrypoint.sh`가 `JWT_PRIVATE_KEY_B64` 환경변수에서 복원한다.
- 이 키는 공개된 값이므로 어떤 실제 환경에도 등록하지 말 것.
- 테스트 클래스패스가 메인보다 우선하므로, 메인의 `jwt/app.pub`이 아니라 이 쌍이 쓰인다.
  개인키·공개키가 짝이 맞아야 하므로 둘 다 여기에 있어야 한다.
