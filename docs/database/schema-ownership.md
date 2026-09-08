# DB Schema Ownership

이 문서는 PostForge modular monolith에서 DB와 준영속 저장소를 어느 모듈이 소유하는지 선언한다.
모듈 그래프와 기능 책임은 [Module Dependency Policy](../architecture/module-dependencies.md)에 둔다.

## Source Of Truth Hierarchy

1. production 물리 schema와 적용 순서: `app/src/main/resources/db/migration/`의 Flyway SQL
2. table·key·resource owner: 이 문서
3. application mapping: JPA entity, JDBC initializer, Spring AI initializer. 위 두 기준과 일치해야 하며 독립적인 정본이 아니다.
4. 시각화: [PostForge MVP ERD](./postforge-mvp-erd.md). 위 기준에서 파생한다.

## Ownership Rules

- 하나의 table은 하나의 owning module만 갖는다.
- owning module만 해당 table의 entity/repository/write policy를 변경한다.
- 다른 module이 owning module의 table을 직접 읽거나 쓰면 안 된다. 필요한 경우 `core` port, API, message contract로 우회한다.
- table/column/index/constraint를 바꾸면 같은 변경에서 이 문서를 갱신한다.
- destructive change는 PR/release note 또는 별도 SQL artifact에 rollback/compatibility note를 남긴다.
- 운영 적용은 Flyway runtime migration을 기본 경로로 사용한다.

## Relational Tables

아래 표는 현재 구현의 owner 기준이다. 메일 구독은 향후 범위이며 관련 table과 mapping은 아직 없다.

| Owner | Table | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `accounts` | `auth/account/domain/Account.java` | 계정 identity, OAuth provider identity, account fields, optimistic lock version |
| `auth` | `account_roles` | `Account.roles` `@CollectionTable` | account role set; `accounts` lifecycle에 종속 |
| `board` | `posts` | `board/post/domain/Post.java` | 게시글 본문, summary/tags/category/board_category/publish_origin, 조회수, like count, 작성자 account id와 nickname snapshot; 자동 수집 뉴스는 `PRODUCT_LAUNCH_NEWS`, 전날 뉴스의 분야별 요약은 `DAILY_DIGEST`로 저장 |
| `board` | `post_tags` | `Post.tags` `@CollectionTable` | 게시글 tag collection; `posts` lifecycle에 종속 |
| `board` | `comments` | `board/comment/domain/Comment.java` | 댓글/대댓글 tree; 작성자는 `accounts.id` 값을 `account_id` scalar로 보관 |
| `board` | `post_like` | `board/like/domain/PostLike.java` | 게시글 좋아요 uniqueness: `(post_id, account_id)` |
| `board` | `comment_like` | `board/like/domain/CommentLike.java` | 댓글 좋아요 uniqueness: `(comment_id, account_id)` |
| `board` | `post_file` | `board/file/domain/PostFile.java` | S3 object metadata and post attachment relation |
| `board` | `post_reference_links` | `board/post/domain/PostReferenceLink.java` | 자동 게시 뉴스의 출처, canonical URL 중복 기준, publish_origin snapshot, keyword 일일 한도 metadata |
| `ingest` | `tracked_keywords` | `ingest/news/domain/TrackedKeyword.java` | 뉴스 자동 수집 대상 키워드와 display policy, 수집 분야(category, 게시글 `board_category`로 전달) |
| `ai` | `vector_store` | Spring AI PgVector mapping | RAG embeddings; 테이블과 HNSW 인덱스는 Flyway `V0000` baseline에 포함된다. Spring AI의 `initialize-schema: true`도 설정되어 있으며 embedding dimensions 기본값은 1536 |

## Non-Relational Storage

| Owner | Key/Resource | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `refresh_token:*` | `auth/token/infrastructure/redis/RefreshTokenRepository.java` | refresh token state |
| `auth` | `email_verify_token:*`, `email_verified:*` | `auth/email/infrastructure/redis/RedisEmailVerificationStore.java` | email verification token/state |
| `auth` | `email_verify_send:cooldown:email:*`, `email_verify_send:rate:email:*`, `email_verify_send:lock:email:*` | `auth/email/infrastructure/redis/RedisEmailVerificationRequestStore.java` | email verification request cooldown/rate/lock guard |
| `auth` | `oauth2_code:*` | `auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java` | short-lived OAuth exchange code |
| `auth` | `auth:login:rate:user:*`, `auth:login:rate:ip:*`, `auth:login:fail:*`, `auth:login:lock:*` | `auth/login/infrastructure/redis/RedisLoginAttemptLimiter.java` | login abuse guard |
| `board` | `post:views:*`, `post:viewed:*`, view dirty/processing keys | `board/view/infrastructure/redis/ViewCountRedisKeys.java` | view count cache, dedupe, sync queue |
| `board` | `like:cooldown:*`, `like:rate:*` | `board/like/infrastructure/redis/LikeRequestRedisRepository.java` | like abuse guard |
| `board` | S3 bucket objects | `board/file/infrastructure/storage/S3FileStorageAdapter.java` | `post_file` stores metadata; object lifecycle belongs to board file domain |

## PgVector Decision

Decision:
- The Spring AI PgVector table, `vector_store`, is owned by `ai`/RAG.
- `ingest` may submit documents during the current monolith phase, but it does not own PgVector schema, index, dimensions, or embedding model decisions.

## 마이그레이션 규칙

런타임 마이그레이션은 `app/src/main/resources/db/migration/`의 `VNNNN__description.sql`에 둔다.
신규 빈 DB는 `V0000` baseline부터 실행하고, production-like 환경은 Flyway 적용 후 Hibernate `validate`로 mapping 불일치를 잡는다.
로컬 개발도 `application.yml`과 `.env.local.example` 기준으로 Flyway가 기본 활성화되고 Hibernate는 `validate`를 사용한다. 기존 non-empty DB 편입만 아래의 1회성 baseline 절차를 따른다.

각 migration은 primary owner 하나를 갖고 다음 header에 호환성, rollback, 검증 방법을 남긴다.

```sql
-- Owner:
-- Purpose:
-- Tables:
-- Compatibility:
-- Rollback:
-- Verification:
```

### Flyway 사용 런북

1. 다음 migration 번호의 파일을 만들고 header와 SQL을 작성한다.
2. table/column/index/constraint 또는 owner가 바뀌면 같은 변경에서 이 문서를 갱신한다.
3. 로컬에서 애플리케이션을 실행해 Flyway 적용과 Hibernate `validate`를 확인한다. `SPRING_FLYWAY_ENABLED=true`는 기본값이라 보통 생략할 수 있다.

```bash
./gradlew :app:bootRun
```


### 기존 DB 최초 편입

기존 non-empty DB를 편입하는 1회성 실행에서만 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`와
`SPRING_FLYWAY_BASELINE_VERSION=0`을 사용한다. 편입 전에는 기존 schema가 `V0000`과 호환되는지 rehearsal하고,
완료 후 `baseline-on-migrate`를 다시 `false`로 돌린다.
