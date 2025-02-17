-- jikim 유저 생성 (이미 존재하면 무시)
DO $$ BEGIN
CREATE ROLE jikim WITH LOGIN PASSWORD 'jikim';
EXCEPTION WHEN OTHERS THEN
   RAISE NOTICE 'Role jikim already exists';
END $$;

-- jikim 유저에게 데이터베이스 권한 부여
ALTER DATABASE basic OWNER TO jikim;
GRANT ALL PRIVILEGES ON DATABASE basic TO jikim;

-- 새 테이블을 만들거나 데이터를 삽입할 수 있도록 권한 부여
GRANT ALL ON SCHEMA public TO jikim;
