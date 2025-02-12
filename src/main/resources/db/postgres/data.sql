-- schema.sql 실행 후 실행됨

INSERT INTO users (password, name, email)
VALUES ('admin', '관리자', 'admin')
    ON CONFLICT (email) DO NOTHING;