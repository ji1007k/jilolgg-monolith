-- 사용자 생성 (이미 있으면 오류 발생하지 않음)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_user WHERE usename = 'jikim') THEN
        CREATE USER jikim WITH PASSWORD 'jikim';
    END IF;
END $$;


-- 데이터베이스 권한 부여
GRANT ALL PRIVILEGES ON DATABASE basic TO jikim;

-- 스키마 내 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO jikim;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO jikim;
GRANT USAGE ON SCHEMA public TO jikim;

-- 앞으로 생성될 테이블과 시퀀스에 대한 기본 권한 설정
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO jikim;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO jikim;
