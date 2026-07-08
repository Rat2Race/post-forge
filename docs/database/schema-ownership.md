# DB Schema Ownership

이 문서는 PostForge modular monolith에서 DB와 준영속 저장소를 어느 모듈이 소유하는지 선언한다.
목표는 DB를 바로 쪼개기 전에 테이블 변경 책임, migration 리뷰 위치, MSA 분리 후보를 명확히 하는 것이다.

Last verified against code: 2026-06-25.

## Ownership Rules

- 하나의 table은 하나의 owning module만 갖는다.
- owning module만 해당 table의 entity/repository/write policy를 변경한다.
- 다른 module이 owning module의 table을 직접 읽거나 쓰면 안 된다. 필요한 경우 `core` port, API, message contract로 우회한다.
- table/column/index/constraint를 바꾸면 같은 변경에서 이 문서를 갱신한다.
- destructive change는 PR/release note 또는 별도 SQL artifact에 rollback/compatibility note를 남긴다.
- 운영 적용 자동화는 Flyway runtime migration을 기본 경로로 사용한다. 현재 code-backed schema source of truth는 Flyway SQL, JPA entity, 아직 Flyway로 흡수되지 않은 JDBC schema initializer, Spring AI PgVector schema initializer, 그리고 이 문서다.

## Relational Tables

현재 구현 기준 source of truth는 아래 표다. "Target Schema Candidates" 섹션은 다음 DB redesign에서 추가/확장할 후보이며, 실제 JPA entity/migration이 생기기 전까지 운영 schema로 간주하지 않는다.

| Owner | Table | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `accounts` | `auth/account/domain/Account.java` | 계정 identity, OAuth provider identity, account fields, optimistic lock version |
| `auth` | `account_roles` | `Account.roles` `@CollectionTable` | account role set; `accounts` lifecycle에 종속 |
| `board` | `posts` | `board/post/domain/Post.java` | 게시글 본문, summary/tags/category/board_category/publish_origin, 조회수, like count, 작성자 account id와 nickname snapshot |
| `board` | `post_tags` | `Post.tags` `@CollectionTable` | 게시글 tag collection; `posts` lifecycle에 종속 |
| `board` | `comments` | `board/comment/domain/Comment.java` | 댓글/대댓글 tree; 작성자는 `accounts.id` 값을 `account_id` scalar로 보관 |
| `board` | `post_like` | `board/like/domain/PostLike.java` | 게시글 좋아요 uniqueness: `(post_id, account_id)` |
| `board` | `post_purchase_vote` | `board/purchase/domain/PostPurchaseVote.java` | 출시 뉴스 구매 판단 투표 uniqueness: `(post_id, account_id)` |
| `board` | `comment_like` | `board/like/domain/CommentLike.java` | 댓글 좋아요 uniqueness: `(comment_id, account_id)` |
| `board` | `post_file` | `board/file/domain/PostFile.java` | S3 object metadata and post attachment relation |
| `board` | `post_product_links` | `board/post/domain/PostProductLink.java` | 게시글과 catalog product id의 느슨한 연결 |
| `board` | `post_reference_links` | `board/post/domain/PostReferenceLink.java` | 출시 뉴스 source evidence, canonical URL duplicate anchor, publish_origin snapshot, keyword/product daily-cap metadata |
| `ingest` | `tracked_keywords` | `ingest/product/domain/TrackedKeyword.java` | 스케줄 수집 대상 키워드와 source/display policy |
| `ingest` | `collection_jobs` | `ingest/product/domain/CollectionJob.java` | 상품 수집 실행 단위 상태 |
| `ingest` | `raw_products` | `ingest/product/domain/RawProduct.java` | collection job별 외부 API 응답 원본 payload |
| `catalog` | `product_categories` | `catalog/product/domain/ProductCategory.java` | 상품 카테고리 tree metadata |
| `catalog` | `products` | `catalog/product/domain/Product.java` | 외부 상품 데이터를 내부 표준 모델로 정규화한 상품 truth |
| `catalog` | `offers` | `catalog/product/domain/Offer.java` | product에 연결된 source/mall별 외부 판매 상품 식별자 |
| `catalog` | `product_embeddings` | `catalog/matching/infrastructure/persistence/ProductEmbeddingSchemaInitializer.java`, `ProductEmbeddingJdbcStore.java` | product matching용 pgvector table; HNSW cosine index |
| `catalog` | `product_match_candidates` | `catalog/matching/domain/ProductMatchCandidate.java` | 자동 매칭 확신이 낮은 상품 병합 후보 |
| `price` | `price_snapshots` | `price/tracking/domain/PriceSnapshot.java` | offer 수집 시점별 가격 스냅샷 |
| `ai` | `vector_store` | `ai/search/infrastructure/vector/PgVectorConfig.java`, Spring AI PgVector default | RAG document embeddings; default table name from local Spring AI PgVector 1.0.7 constant |

## Target Schema Candidates

다음 테이블은 "공개 게시판 + 신상품 출시 뉴스 + 구매 판단 투표 + 응답 전용 가격 판정 + 상품 데이터 수집" 방향을 위한 설계 후보이다.
구현 시에는 같은 변경에서 JPA entity/JDBC initializer, 필요한 schema review artifact, 이 ownership 문서를 함께 갱신한다.

| Owner | Table | Planned Source | Notes |
| --- | --- | --- | --- |
| `notification` | `keyword_subscriptions` | keyword watch list | 사용자별 관심 키워드와 활성 상태 |
| `notification` | `notification_events` | notification queue | 수집 item과 keyword subscription 매칭 결과, 발송 처리 상태 |
| `notification` | `email_delivery_logs` | email delivery log | 이메일 발송 성공/실패/재시도 이력 |
| `workspace` | `workspaces` | private report workspace | account별 개인 리포트 작업공간 |
| `workspace` | `workspace_members` | private report workspace | workspace 소유자/협업자 권한. MVP는 owner 1명으로 시작 가능 |
| `workspace` | `drafts` | private writing workspace | 공개 발행 전 비공개 초안/리포트 |
| `workspace` | `draft_sources` | private writing workspace | 초안이 저장한 product/trend/external URL |
| `workspace` | `saved_trend_bundles` | private writing workspace | 사용자가 리포트 작성에 쓰려고 저장한 trend 묶음 |
| `workspace` | `saved_trend_bundle_items` | private writing workspace | 저장한 trend bundle 구성 관계 |
| `board` | `post_rank_scores` | future ranking read model | 추천/쇼핑몰 분기에서 검토할 내부 랭킹 score |
| `billing` | `subscription_plans` | plan/quota policy | 무료/유료 plan별 AI quota, private workspace limit |
| `billing` | `account_subscriptions` | account plan state | account별 현재 plan과 상태 |
| `ai` | `ai_budget_windows` | AI cost control | account/system별 월간/일간 AI budget window |
| `ai` | `ai_usage_logs` | AI cost control | 모든 AI operation의 token/cost/status 원본 이력 |

Ownership boundary:
- `source` owns external product/news API adapter execution contracts and concrete clients.
- `ingest` owns collection orchestration, tracked keywords, collection jobs, and raw product payloads.
- `catalog` owns normalized product truth.
- `price` owns price snapshot history and response-only price judgement logic. Price judgement reads source samples but writes no table.
- `messaging` owns in-process event dispatch only. It does not own board, source, ingest, catalog, price, or notification event semantics.
- `notification` owns keyword watch and delivery state: keyword subscriptions, notification events, and email delivery logs.
- `workspace` owns private writing state: workspaces, drafts, draft source selections, saved trend bundles.
- `board` owns user-facing published content: posts, comments, likes, purchase votes, files, post-product links, and post reference links. MVP board sorting stays count/index based.
- `billing` owns plan/subscription state. Public board trust signals must not be plan-gated.
- `ai` owns RAG document embeddings, usage accounting, and model operation logs. AI may generate suggestions or launch-news drafts, but it should not own board post rows, workspace draft rows, source rows, catalog rows, or price rows.
- `support` may provide shared infrastructure helpers, but feature modules own their runtime state and policies.
- Read paths such as post detail must not call AI or external APIs on every request.

## Non-Relational Storage

| Owner | Key/Resource | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `refresh_token:*` | `auth/token/infrastructure/redis/RefreshTokenRepository.java` | refresh token state |
| `auth` | `email_verify_token:*`, `email_verified:*` | `auth/email/infrastructure/redis/RedisEmailVerificationStore.java` | email verification token/state |
| `auth` | `oauth2_code:*` | `auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java` | short-lived OAuth exchange code |
| `board` | S3 bucket objects | `board/file/infrastructure/storage/S3FileStorageAdapter.java` | `post_file` stores metadata; object lifecycle belongs to board file domain |

## PgVector Decision

Current runtime:
- `ai` creates the Spring AI `PgVectorStore` bean and owns OpenAI embedding configuration for RAG document search.
- `ingest` currently depends only on the Spring AI `VectorStore` API and writes documents through that API.
- `catalog` owns a separate product-matching pgvector table, `product_embeddings`, initialized by `ProductEmbeddingSchemaInitializer`.
- `app` config enables Spring AI PgVector schema initialization for the `ai` `vector_store` table.

Decision:
- The Spring AI PgVector table, `vector_store`, is owned by `ai`/RAG.
- The product matching PgVector table, `product_embeddings`, is owned by `catalog`.
- Both tables may share the PostgreSQL `vector` extension in the modular monolith, but schema, index, dimensions, and embedding input ownership stay separate.
- `ingest` may submit documents during the current monolith phase, but it does not own PgVector schema, index, dimensions, or embedding model decisions.
- When splitting services later, move document-write orchestration behind an AI/RAG API or message contract instead of letting ingest write the vector table directly.

## 마이그레이션 규칙

런타임 마이그레이션은 실행 모듈인 `app`의 `app/src/main/resources/db/migration/` 아래에 둔다. 이 경로의 SQL은 Spring Boot가 애플리케이션 시작 시 Flyway로 실행한다. 로컬 개발 환경은 기존 개발 DB를 갑자기 Flyway history로 묶지 않기 위해 Flyway를 기본 비활성화하고, 필요하면 Hibernate `ddl-auto=update`와 JDBC initializer로 schema를 맞출 수 있다. production-like 환경은 Flyway를 기본 활성화하고 Hibernate는 `ddl-auto=validate`로 schema 불일치만 검증한다.

현재 상태는 Flyway만으로 전체 빈 DB를 만드는 완전한 bootstrap 모델이 아니라 과도기적 hybrid schema 모델이다. `app/src/main/resources/db/migration/` 아래 파일은 Flyway 런타임 기준으로 canonical source이고, Spring AI PgVector schema initializer와 아직 Flyway로 흡수되지 않은 JDBC schema initializer는 동등한 Flyway migration이 생길 때까지 명시적 예외로 남는다. 기존 non-empty 운영 DB에 Flyway를 켤 때는 baseline 선택을 release gate로 다룬다.

리뷰 가능한 SQL artifact가 필요하면 `VNNNN__description.sql` 형식으로 런타임 마이그레이션 경로에 추가하고, 같은 변경에서 이 문서도 갱신한다. `app/src/main/resources/db/migration/`이 실제 런타임 동작의 기준이다. `docs/database/migrations/`에는 리뷰용 mirror를 둘 수 있지만, 두 파일이 모두 존재하면 나중에 mirror를 제거하기 전까지 byte-for-byte로 같아야 한다.

현재 런타임 마이그레이션:
- `app/src/main/resources/db/migration/V0001__add_post_board_category.sql`: `posts.board_category`를 추가하고, 기존 row를 `GENERAL`로 backfill한 뒤 post category/origin 필터용 index를 만든다.

현재 리뷰 mirror:
- `docs/database/migrations/V0001__add_post_board_category.sql`: 런타임 `V0001` migration과 같은 내용의 리뷰용 mirror다.

필수 헤더:

```sql
-- Owner:
-- Purpose:
-- Tables:
-- Compatibility:
-- Rollback:
-- Verification:
```

규칙:
- 하나의 migration 파일은 하나의 primary owner에 속해야 한다. 여러 owner를 동시에 바꾸면 coordination note를 남긴다.
- 로컬 또는 수동 재실행 가능성이 있는 SQL은 가능한 한 idempotent하게 작성한다.
- `application.yml`은 로컬 개발 기본값으로 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 사용한다. 필요하면 환경 변수로 덮어쓴다.
- `application.yml`은 `SPRING_FLYWAY_ENABLED=false`를 기본값으로 둔다. 기존 로컬 개발 DB를 즉시 Flyway history로 강제 편입하지 않기 위해서다.
- `application-prod.yml`은 `SPRING_FLYWAY_ENABLED=true`를 기본값으로 두고, Hibernate는 `ddl-auto=validate`를 사용한다.
- Flyway `baseline-on-migrate`는 기본값을 `false`로 둔다. 기존 DB를 Flyway로 편입하는 1회성 adoption run에서만 의도적으로 켠다.
- 기존 DB에 이미 `V0001` 효과가 반영되어 있으면 최초 1회 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`, `SPRING_FLYWAY_BASELINE_VERSION=1`로 baseline을 잡고, 이후 다시 baseline-on-migrate를 끈다.
- 기존 DB에 `V0001` 효과가 아직 없고 과거 schema만 있으면, 리허설 후 최초 1회 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`, `SPRING_FLYWAY_BASELINE_VERSION=0`으로 `V0001`을 실행시키거나, `V0001`을 수동 적용한 뒤 version `1`로 baseline을 잡는다.
- `V0001` schema change가 실제로 적용되기 전에 `SPRING_FLYWAY_BASELINE_VERSION=1`을 쓰면 안 된다. Flyway가 `V0001`을 이미 적용된 것으로 간주한다.
- 완전한 historical baseline migration이 생기기 전까지는 Flyway만으로 빈 DB를 bootstrap한다고 주장하지 않는다.

### Flyway 사용 런북

새 DB 변경은 운영 DB에 직접 SQL을 치지 않고 migration 파일로 남긴다.

1. `app/src/main/resources/db/migration/`에 다음 번호의 파일을 만든다.

   ```text
   V0002__add_some_column.sql
   ```

2. SQL header를 채우고, 변경 SQL을 작성한다.

   ```sql
   -- Owner: board
   -- Purpose: Add foo column for ...
   -- Tables: posts
   -- Compatibility: Safe for existing rows because ...
   -- Rollback: ALTER TABLE posts DROP COLUMN IF EXISTS foo;
   -- Verification: Hibernate validate must pass after migration.

   ALTER TABLE posts
       ADD COLUMN IF NOT EXISTS foo varchar(50);
   ```

3. 로컬에서 Flyway까지 확인하고 싶으면 일시적으로 켠다.

   ```bash
   SPRING_FLYWAY_ENABLED=true ./gradlew :app:bootRun
   ```

4. production-like 환경은 기본적으로 Flyway가 켜져 있다. 애플리케이션 시작 시 Flyway가 먼저 migration을 적용하고, 그 다음 Hibernate `validate`가 entity와 DB schema 불일치를 잡는다.

5. 운영 DB 최초 편입 시에는 baseline 설정을 release checklist에 포함한다. baseline-on-migrate는 편입용 1회성 설정이고, 평소에는 `false`로 둔다.

운영 질문에 대한 짧은 답변:

> 운영 DB 컬럼 추가는 DB 콘솔에서 직접 처리하지 않고 Flyway migration SQL로 남깁니다. SQL은 `app/src/main/resources/db/migration/`에 `V000N__description.sql` 형식으로 추가하고, 배포 시 Flyway가 적용합니다. production은 적용 후 Hibernate `validate`로 schema mismatch를 잡습니다. 기존 운영 DB를 처음 Flyway에 편입할 때만 baseline을 release gate로 관리합니다.
