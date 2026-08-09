-- 테스트용 사용자 데이터
--
-- id를 명시적으로 지정하는 이유:
-- 테스트가 @Transactional + @Rollback으로 돌면 INSERT는 롤백되지만 identity 시퀀스는 되돌아가지 않는다.
-- 자동 채번에 맡기면 테스트 메서드마다 admin의 id가 달라져,
-- 고정 id로 사용자를 조회하는 코드(RefreshTokenService.createRefreshToken 등)가 실패한다.
-- 자동 채번 값과 겹치지 않도록 1000번대를 사용하며, AuthTestSupport의 id와 맞춰야 한다.

-- 권한 검증용 사용자
INSERT INTO "users" ("id", "password", "name", "email", "authority", "password_version")
SELECT 1001, '$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy', '관리자', 'admin', 'SCOPE_ADMIN', 1
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'admin'
);

INSERT INTO "users" ("id", "password", "name", "email", "authority", "password_version")
SELECT 1002, '$2b$12$uafO29l5A0yTu8h5sH2GPeCZUaV.yKwwp7ZFdu4RlqCJqI3HEQvRW', '매니저', 'manager', 'SCOPE_MANAGER', 1
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'manager'
);

INSERT INTO "users" ("id", "password", "name", "email", "password_version")
SELECT 1003, '$2b$12$RHbwxJ0xC1Jp2ip6aV0h3OhDLZFXvo1cNWmnXHPRmUylGzoWEp6zG', '일반사용자', 'user', 1
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'user'
);
