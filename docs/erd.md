# ERD

기준: `src/main/resources/db/postgres/schema.sql` (현재 코드 기준)

## Mermaid ERD
```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password
        VARCHAR name
        VARCHAR authority
        VARCHAR profile_image_url
        TIMESTAMP created_dt
        TIMESTAMP updated_dt
    }

    leagues {
        BIGINT id PK
        VARCHAR league_id UK
        VARCHAR slug
        VARCHAR name
        VARCHAR region
        TEXT image
        INT priority
        INT display_position
        VARCHAR display_status
    }

    tournaments {
        BIGINT id PK
        VARCHAR tournament_id UK
        VARCHAR slug
        TIMESTAMP start_date
        TIMESTAMP end_date
        VARCHAR league_id
    }

    matches {
        BIGINT id PK
        VARCHAR match_id UK
        VARCHAR league_id
        VARCHAR tournament_id
        TIMESTAMP start_time
        VARCHAR state
        VARCHAR block_name
        INT game_count
        VARCHAR strategy
    }

    teams {
        BIGINT id PK
        VARCHAR team_id UK
        VARCHAR slug
        VARCHAR code
        VARCHAR name
        TEXT image
        VARCHAR league_id
    }

    match_teams {
        BIGINT id PK
        VARCHAR match_id
        VARCHAR team_id
        VARCHAR outcome
        INT game_wins
    }

    manual_match_overrides {
        BIGINT id PK
        VARCHAR match_id UK
        TIMESTAMP override_start_time
        VARCHAR override_block_name
        BOOLEAN lock_start_time
        BOOLEAN lock_block_name
        VARCHAR updated_by
        TIMESTAMP updated_at
    }

    user_favorite_team {
        BIGINT id PK
        BIGINT user_id
        VARCHAR team_id
        INT display_order
    }

    user_league_orders {
        BIGINT id PK
        BIGINT user_id
        VARCHAR league_id
        INT display_order
        TIMESTAMP updated_at
    }

    fcm_tokens {
        BIGINT id PK
        BIGINT user_id
        VARCHAR token
        TEXT device_info
        TIMESTAMP updated_at
    }

    match_alarms {
        BIGINT id PK
        BIGINT user_id
        VARCHAR match_id
    }

    posts {
        BIGINT id PK
        VARCHAR title
        TEXT content
        BIGINT user_id
        TIMESTAMP created_dt
        TIMESTAMP updated_dt
        BIGINT view_cnt
        BIGINT like_cnt
        VARCHAR category
        VARCHAR status
    }

    files {
        BIGINT id PK
        BIGINT post_id
        VARCHAR file_name
        VARCHAR file_path
        TIMESTAMP created_dt
        TIMESTAMP updated_dt
        VARCHAR status
    }

    users ||--o{ user_favorite_team : "user_id"
    users ||--o{ user_league_orders : "user_id"
    users ||--o{ fcm_tokens : "user_id"
    users ||--o{ match_alarms : "user_id"
    users ||--o{ posts : "user_id"

    leagues ||--o{ tournaments : "league_id"
    leagues ||--o{ matches : "league_id"
    leagues ||--o{ teams : "league_id"
    leagues ||--o{ user_league_orders : "league_id"

    tournaments ||--o{ matches : "tournament_id"

    matches ||--o{ match_teams : "match_id"
    teams ||--o{ match_teams : "team_id"
    matches ||--o| manual_match_overrides : "match_id"
    matches ||--o{ match_alarms : "match_id"

    posts ||--o{ files : "post_id"
```

## Notes
- 일부 관계는 JPA 매핑 기준으로는 연관되어 있지만, DB 레벨 `FOREIGN KEY` 제약은 스키마에 명시되지 않은 구간이 있습니다.
- 운영에서 무결성을 강하게 보장하려면 FK 제약 추가를 별도 마이그레이션으로 관리하는 것을 권장합니다.
