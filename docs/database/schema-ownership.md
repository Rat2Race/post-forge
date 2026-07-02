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
- 운영 적용 자동화는 아직 없다. 현재 code-backed schema source of truth는 JPA entity, JDBC schema initializer, Spring AI PgVector schema initializer, 그리고 이 문서다.

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
| `messaging` | `outbox_events` | `messaging/outbox/domain/OutboxMessage.java` | standalone reliable event handoff table; no domain-table FK |
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
- `messaging` owns the shared outbox infrastructure: persisted event envelopes, retry state, relay claim policy, and broker publishing adapters. It does not own board, source, ingest, catalog, price, or notification event semantics.
- `notification` owns keyword watch and delivery state: keyword subscriptions, notification events, and email delivery logs.
- `workspace` owns private writing state: workspaces, drafts, draft source selections, saved trend bundles.
- `board` owns user-facing published content: posts, comments, likes, purchase votes, files, post-product links, and post reference links. MVP board sorting stays count/index based.
- `billing` owns plan/subscription state. Public board trust signals must not be plan-gated.
- `ai` owns RAG document embeddings, usage accounting, and model operation logs. AI may generate suggestions or launch-news drafts, but it should not own board post rows, workspace draft rows, source rows, catalog rows, or price rows.
- `support` may provide shared infrastructure helpers such as Redis operations, but the caller module owns each Redis key namespace.
- Read paths such as post detail must not call AI or external APIs on every request.

## Non-Relational Storage

| Owner | Key/Resource | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `refresh_token:*` | `auth/token/infrastructure/redis/RefreshTokenRepository.java` | refresh token state |
| `auth` | `email_verify_token:*`, `email_verified:*` | `auth/email/infrastructure/redis/RedisEmailVerificationStore.java` | email verification token/state |
| `auth` | `email_verify_send:cooldown:email:*`, `email_verify_send:rate:email:*`, `email_verify_send:lock:email:*` | `auth/email/infrastructure/redis/RedisEmailVerificationRequestStore.java` | email verification request cooldown/rate/lock guard |
| `auth` | `oauth2_code:*` | `auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java` | short-lived OAuth exchange code |
| `auth` | `auth:login:rate:user:*`, `auth:login:rate:ip:*`, `auth:login:fail:*`, `auth:login:lock:*` | `auth/login/infrastructure/redis/RedisLoginAttemptStore.java` | login abuse guard |
| `board` | `post:views:*`, `post:viewed:*`, view dirty/processing keys | `board/view/infrastructure/redis/ViewCountRedisKeys.java` | view count cache, dedupe, sync queue |
| `board` | `like:cooldown:*`, `like:rate:*` | `board/like/infrastructure/redis/LikeRequestRedisRepository.java` | like abuse guard |
| `board` | S3 bucket objects | `board/file/infrastructure/storage/S3FileStorageAdapter.java` | `post_file` stores metadata; object lifecycle belongs to board file domain |
| `messaging` | broker topics/queues | future MQ adapter | No broker resource is active yet; the DB outbox remains the source of truth before publish |

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

## Migration Convention

There is no active runtime migration directory in the current tree. Local development schema may be evolved by Hibernate `ddl-auto=update` and JDBC initializers; production-like environments use `ddl-auto=validate`.

When a reviewable SQL artifact is needed, use a `VNNNN__description.sql` file in the chosen migration artifact location and keep this document linked to the same change.

Current reviewable SQL artifacts:
- `docs/database/migrations/V0001__add_post_board_category.sql` adds `posts.board_category`, backfills existing rows to `GENERAL`, and creates post category/origin filter indexes.

Required header:

```sql
-- Owner:
-- Purpose:
-- Tables:
-- Compatibility:
-- Rollback:
-- Verification:
```

Rules:
- One migration file should belong to one primary owner. Cross-owner changes require an explicit coordination note.
- Use idempotent SQL where practical for local/manual replay.
- `application.yml` defaults `SPRING_JPA_HIBERNATE_DDL_AUTO` to `update` for local development unless overridden.
- `application-prod.yml` uses `ddl-auto=validate`.
- Flyway/Liquibase can be introduced later by moving reviewed SQL into the tool's runtime migration location.
