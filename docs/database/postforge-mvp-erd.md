# PostForge MVP ERD

> Current status: 현재 구현을 시각화한 derived document다.
> Table ownership과 물리 schema의 기준은 [DB Schema Ownership](./schema-ownership.md)이다.

이 문서는 현재 relational/PgVector schema의 관계, 컬럼, index를 ERD 관점으로 보여준다. 자동 수집 뉴스와 데일리 요약은 별도 테이블이 아니라 `posts.category`의 `PRODUCT_LAUNCH_NEWS`와 `DAILY_DIGEST`로 구분하고, 수집 분야는 `posts.board_category`에 기록한다. 향후 메일 구독 schema는 아직 없다.

## ERD

```mermaid
erDiagram
    ACCOUNTS ||--o{ ACCOUNT_ROLES : has
    ACCOUNTS ||--o{ POSTS : writes
    ACCOUNTS ||--o{ COMMENTS : writes
    ACCOUNTS ||--o{ POST_LIKE : gives
    ACCOUNTS ||--o{ COMMENT_LIKE : gives

    POSTS ||--o{ POST_TAGS : has
    POSTS ||--o{ COMMENTS : has
    COMMENTS ||--o{ COMMENTS : replies
    POSTS ||--o{ POST_LIKE : receives
    COMMENTS ||--o{ COMMENT_LIKE : receives
    POSTS ||--o{ POST_FILE : attaches
    POSTS ||--o{ POST_REFERENCE_LINKS : cites_source

    ACCOUNTS {
        bigint id PK
        varchar username UK
        varchar password
        varchar email UK
        varchar nickname UK
        varchar provider
        varchar provider_id
        bigint version
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    ACCOUNT_ROLES {
        bigint account_id FK
        varchar role
    }

    POSTS {
        bigint id PK
        varchar title
        varchar content
        varchar summary
        varchar category
        varchar board_category
        varchar publish_origin
        bigint views
        bigint like_count
        bigint account_id FK "accounts"
        varchar nickname
        timestamp created_at
        varchar created_by
        timestamp modified_at
        varchar modified_by
    }

    POST_TAGS {
        bigint post_id FK
        varchar tag
    }

    COMMENTS {
        bigint id PK
        bigint post_id FK
        varchar content
        bigint account_id FK "accounts"
        varchar nickname
        bigint parent_id FK
        bigint like_count
        timestamp created_at
        varchar created_by
        timestamp modified_at
        varchar modified_by
    }

    POST_LIKE {
        bigint id PK
        bigint post_id FK
        bigint account_id FK "accounts"
        timestamp created_at
        varchar created_by
        timestamp modified_at
        varchar modified_by
    }

    COMMENT_LIKE {
        bigint id PK
        bigint comment_id FK
        bigint account_id FK "accounts"
        timestamp created_at
        varchar created_by
        timestamp modified_at
        varchar modified_by
    }

    POST_FILE {
        bigint id PK
        bigint post_id FK
        varchar original_file_name
        varchar saved_file_name
        varchar file_path
        bigint file_size
        varchar file_type
        timestamp created_at
    }

    POST_REFERENCE_LINKS {
        bigint id PK
        bigint post_id FK
        varchar keyword
        varchar provider
        varchar canonical_url
        varchar original_url
        varchar source_name
        timestamp published_at
        varchar title_snapshot
        varchar publish_origin
    }

    TRACKED_KEYWORDS {
        bigint id PK
        varchar keyword UK
        int display_count
        boolean enabled
        varchar category
        timestamp created_at
        timestamp updated_at
    }

    VECTOR_STORE {
        uuid id PK
        text content
        json metadata
        vector embedding
    }
```

## Index Targets

| Query | Index Candidate |
| --- | --- |
| 게시글 최신순 | `posts(created_at)` |
| 작성자 게시글 | `posts(account_id)` |
| 게시글 유형 필터 | `posts(category)` |
| 분야 필터 | `posts(board_category)` |
| 발행 출처 필터 | `posts(publish_origin)` |
| 댓글 조회 | `comments(post_id, created_at)` |
| 대댓글 조회 | `comments(parent_id)` |
| 뉴스 자동 수집 대상 | `tracked_keywords(keyword)` unique |
| 게시글별·provider별 뉴스 출처 조회 | `post_reference_links(post_id)`, `post_reference_links(provider)` |
| 기사 URL 중복 게시 방지 | `post_reference_links(canonical_url)` unique |
| 중복 좋아요 방지 | `post_like(post_id, account_id)`, `comment_like(comment_id, account_id)` unique |
