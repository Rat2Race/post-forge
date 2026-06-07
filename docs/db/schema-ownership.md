# DB Schema Ownership

이 문서는 PostForge modular monolith에서 DB와 준영속 저장소를 어느 모듈이 소유하는지 선언한다.
목표는 DB를 바로 쪼개기 전에 테이블 변경 책임, migration 리뷰 위치, MSA 분리 후보를 명확히 하는 것이다.

## Ownership Rules

- 하나의 table은 하나의 owning module만 갖는다.
- owning module만 해당 table의 entity/repository/write policy를 변경한다.
- 다른 module이 owning module의 table을 직접 읽거나 쓰면 안 된다. 필요한 경우 `core` port, API, message contract로 우회한다.
- table/column/index/constraint를 바꾸면 같은 변경에서 `docs/db/migrations`와 이 문서를 함께 갱신한다.
- destructive change는 migration artifact에 rollback/compatibility note를 남긴다.
- 운영 적용 자동화는 아직 없다. 현재 migration artifact는 reviewable SQL source of truth이고, Flyway/Liquibase 도입 전까지 수동 적용 절차의 기준이다.

## Relational Tables

현재 구현 기준 source of truth는 아래 표다. "Target Schema Candidates" 섹션은 다음 DB redesign에서 추가/확장할 후보이며, 실제 JPA entity/migration이 생기기 전까지 운영 schema로 간주하지 않는다.

| Owner | Table | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `accounts` | `auth/account/domain/Account.java` | 계정 identity, OAuth provider identity, account fields, optimistic lock version |
| `auth` | `account_roles` | `Account.roles` `@CollectionTable` | account role set; `accounts` lifecycle에 종속 |
| `board` | `posts` | `board/post/domain/Post.java` | 게시글 본문, 조회수, category, 작성자 account id와 nickname snapshot |
| `board` | `post_tags` | `Post.tags` `@CollectionTable` | 게시글 tag collection; `posts` lifecycle에 종속 |
| `board` | `comments` | `board/comment/domain/Comment.java` | 댓글/대댓글 tree; 작성자는 `accounts.id` 값을 `account_id` scalar로 보관 |
| `board` | `post_like` | `board/like/domain/PostLike.java` | 게시글 좋아요 uniqueness: `(post_id, account_id)` |
| `board` | `comment_like` | `board/like/domain/CommentLike.java` | 댓글 좋아요 uniqueness: `(comment_id, account_id)` |
| `board` | `post_file` | `board/file/domain/PostFile.java` | S3 object metadata and post attachment relation |
| `board` | `auto_post_drafts` | `board/post/domain/autopost/AutoPostDraft.java` | AI 가격 하락 게시글 초안과 발행 상태 |
| `source` | `source_policies` | `source/domain/SourcePolicy.java` | source별 enable/quota/retry/circuit breaker 정책 |
| `source` | `external_api_request_logs` | `source/domain/ExternalApiRequestLog.java` | source별 외부 API 호출 결과, 응답 시간, 실패 사유 |
| `ingest` | `tracked_keywords` | `ingest/product/domain/TrackedKeyword.java` | 스케줄 수집 대상 키워드와 source/display policy |
| `ingest` | `collection_jobs` | `ingest/product/domain/CollectionJob.java` | 상품 수집 실행 단위 상태 |
| `ingest` | `raw_products` | `ingest/product/domain/RawProduct.java` | 외부 API 응답 원본 payload |
| `catalog` | `product_categories` | `catalog/domain/ProductCategory.java` | 상품 카테고리 |
| `catalog` | `products` | `catalog/domain/Product.java` | 외부 상품 데이터를 내부 표준 모델로 정규화한 상품 원본 |
| `catalog` | `offers` | `catalog/domain/Offer.java` | source/mall별 외부 판매 상품 식별자 |
| `catalog` | `product_embeddings` | `catalog/infrastructure/persistence/matching/ProductEmbeddingJdbcStore.java` | pgvector 기반 product embedding 저장소 |
| `catalog` | `product_match_candidates` | `catalog/domain/matching/ProductMatchCandidate.java` | 자동 매칭 확신이 낮은 상품 병합 후보 |
| `price` | `price_snapshots` | `price/domain/PriceSnapshot.java` | offer 수집 시점별 가격 스냅샷 |
| `price` | `lowest_price_snapshots` | `price/domain/LowestPriceSnapshot.java` | product별 최신 최저가 read model과 하락률 |
| `board` | `post_product_links` | `board/post/domain/PostProductLink.java` | 자동/상품 관련 게시글과 상품의 느슨한 연결 |
| `messaging` | `outbox_events` | `messaging/outbox/domain/OutboxMessage.java` | standalone reliable event handoff table; no domain-table FK |
| `ai` | `vector_store` | `ai/search/infrastructure/vector/PgVectorConfig.java`, Spring AI PgVector default | RAG document embeddings; default table name from local Spring AI PgVector 1.0.7 constant |

## Target Schema Candidates

다음 테이블은 "상품 데이터 수집 + 가격 하락 자동 게시 + 커머스 커뮤니티" 방향을 위한 설계 후보이다.
구현 시에는 같은 변경에서 JPA entity, migration SQL, 이 ownership 문서를 함께 갱신한다.

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
| `board` | `post_reference_links` | future board reference relation | 게시글이 참고한 product/trend/external URL 연결 |
| `board` | `post_rank_scores` | future ranking read model | 추천/쇼핑몰 분기에서 검토할 내부 랭킹 score |
| `billing` | `subscription_plans` | plan/quota policy | 무료/유료 plan별 AI quota, private workspace limit |
| `billing` | `account_subscriptions` | account plan state | account별 현재 plan과 상태 |
| `ai` | `ai_budget_windows` | AI cost control | account/system별 월간/일간 AI budget window |
| `ai` | `ai_usage_logs` | AI cost control | 모든 AI operation의 token/cost/status 원본 이력 |

Ownership boundary:
- `source` owns external API adapter execution and request logs.
- `ingest` owns collection orchestration, tracked keywords, collection jobs, and raw product payloads.
- `catalog` owns normalized product truth.
- `price` owns price history, lowest-price read models, and price-drop detection state.
- `messaging` owns the shared outbox infrastructure: persisted event envelopes, retry state, relay claim policy, and broker publishing adapters. It does not own board, source, ingest, catalog, price, or notification event semantics.
- `notification` owns keyword watch and delivery state: keyword subscriptions, notification events, and email delivery logs.
- `workspace` owns private writing state: workspaces, drafts, draft source selections, saved trend bundles.
- `board` owns user-facing published content: posts, comments, likes, and files. MVP board sorting stays count/index based.
- `billing` owns plan/subscription state. Public board trust signals must not be plan-gated.
- `ai` owns usage accounting and model operation logs. AI may generate suggestions or brief drafts, but it should not own board post rows, workspace draft rows, source rows, catalog rows, or price rows.
- Read paths such as post detail must not call AI or external APIs on every request.

## Non-Relational Storage

| Owner | Key/Resource | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `refresh_token:*` | `auth/token/infrastructure/redis/RefreshTokenRepository.java` | refresh token state |
| `auth` | `email_verify_token:*`, `email_verified:*` | `auth/email/infrastructure/redis/RedisEmailVerificationStore.java` | email verification token/state |
| `auth` | `oauth2_code:*` | `auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java` | short-lived OAuth exchange code |
| `auth` | `auth:login:fail:*`, `auth:login:lock:*` | `auth/login/support/LoginAttemptGuard.java` | login abuse guard |
| `board` | `post:views:*`, `post:viewed:*`, view dirty/processing keys | `board/view/infrastructure/redis/ViewCountRedisKeys.java` | view count cache, dedupe, sync queue |
| `board` | like request cooldown/rate keys | `board/like/application/LikeRequestGuard.java` | like abuse guard |
| `board` | S3 bucket objects | `board/file/infrastructure/storage/S3FileStorageAdapter.java` | `post_file` stores metadata; object lifecycle belongs to board file domain |
| `messaging` | broker topics/queues | future MQ adapter | Outbox relay may publish persisted events to Kafka/RabbitMQ/SQS later; the DB outbox remains the source of truth before publish |

## PgVector Decision

Current runtime:
- `ai` creates the `PgVectorStore` bean and owns OpenAI embedding configuration.
- `ingest` currently depends only on the Spring AI `VectorStore` API and writes documents through that API.
- `app` config enables Spring AI PgVector schema initialization.

Decision:
- The PgVector table is owned by `ai`/RAG.
- `ingest` may submit documents during the current monolith phase, but it does not own PgVector schema, index, dimensions, or embedding model decisions.
- When splitting services later, move document-write orchestration behind an AI/RAG API or message contract instead of letting ingest write the vector table directly.

## Migration Convention

Use `docs/db/migrations/VNNNN__description.sql` for reviewable SQL artifacts.

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
- `application-prod.yml` is temporarily set to `ddl-auto=create` during the Account table naming reset.
- Revert production-like environments to `ddl-auto=validate` before preserving real data or applying reviewed migrations.
- Flyway/Liquibase can be introduced later by moving reviewed SQL into the tool's runtime migration location.
