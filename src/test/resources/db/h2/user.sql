-- 테스트용 사용자 데이터

INSERT INTO "users" ("password", "name", "email", "authority")
SELECT '$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy', '관리자', 'admin', 'SCOPE_ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'admin'
);

INSERT INTO "users" ("password", "name", "email", "authority")
SELECT '$2b$12$uafO29l5A0yTu8h5sH2GPeCZUaV.yKwwp7ZFdu4RlqCJqI3HEQvRW', '매니저', 'manager', 'SCOPE_MANAGER'
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'manager'
);

INSERT INTO "users" ("password", "name", "email")
SELECT '$2b$12$RHbwxJ0xC1Jp2ip6aV0h3OhDLZFXvo1cNWmnXHPRmUylGzoWEp6zG', '일반사용자', 'user'
WHERE NOT EXISTS (
    SELECT 1 FROM "users" WHERE "email" = 'user'
);


-- INSERT INTO users (password, email, name) VALUES ('password123', 'test@example.com', 'testuser');
-- INSERT INTO users (password, email, name) VALUES ('password123', 'test2@example.com', 'testuser2');
