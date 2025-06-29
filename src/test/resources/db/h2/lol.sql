-- 테스트용 lol 데이터

-- 1. 리그 데이터
INSERT INTO "leagues" ("league_id", "slug", "name", "region", "image", "priority", "display_position", "display_status") VALUES
    ('98767991310872058', 'tft_esports', 'TFT Esports', '국제 대회', 'http://example.com/image.png', 1, 24, 'active');

-- 2. 팀 데이터 (TBD 팀만)
INSERT INTO "teams" ("team_id", "code", "name", "slug", "image", "league_id") VALUES
    ('tbd_team_id', 'TBD', 'TBD', 'tbd', 'http://example.com/tbd.png', '98767991310872058');
