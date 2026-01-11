-- schema.sql 실행 후 실행됨

INSERT INTO users (password, name, email, authority)
VALUES ('$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy', '관리자', 'admin', 'SCOPE_ADMIN')
    ON CONFLICT (email) DO NOTHING;
INSERT INTO users (password, name, email, authority)
VALUES ('$2b$12$uafO29l5A0yTu8h5sH2GPeCZUaV.yKwwp7ZFdu4RlqCJqI3HEQvRW', '매니저', 'manager', 'SCOPE_MANAGER')
    ON CONFLICT (email) DO NOTHING;
INSERT INTO users (password, name, email)
VALUES ('$2b$12$RHbwxJ0xC1Jp2ip6aV0h3OhDLZFXvo1cNWmnXHPRmUylGzoWEp6zG', '일반사용자', 'user')
    ON CONFLICT (email) DO NOTHING;

-- 기본 리그 순서 설정 (userId = 0)
-- LCK
INSERT INTO "user_league_orders" (user_id, league_id, display_order) VALUES (0, '98767991310872058', 0) ON CONFLICT DO NOTHING;
-- LCK CL
INSERT INTO "user_league_orders" (user_id, league_id, display_order) VALUES (0, '98767991335774713', 1) ON CONFLICT DO NOTHING;
-- FIRST STAND
INSERT INTO "user_league_orders" (user_id, league_id, display_order) VALUES (0, '113464388705111224', 2) ON CONFLICT DO NOTHING;
-- MSI
INSERT INTO "user_league_orders" (user_id, league_id, display_order) VALUES (0, '98767991325878492', 3) ON CONFLICT DO NOTHING;
-- WORLDS
INSERT INTO "user_league_orders" (user_id, league_id, display_order) VALUES (0, '98767975604431411', 4) ON CONFLICT DO NOTHING;
