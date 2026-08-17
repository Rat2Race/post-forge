# DB Schema Ownership

이 문서는 PostForge modular monolith에서 DB와 준영속 저장소를 어느 모듈이 소유하는지 선언한다.
모듈 그래프와 기능 책임은 [Module Dependency Policy](../architecture/module-dependencies.md)에 둔다.

## Source Of Truth Hierarchy

1. production 물리 schema와 적용 순서: `app/src/main/resources/db/migration/`의 Flyway SQL
2. table·key·resource owner: 이 문서
3. application mapping: JPA entity, JDBC initializer, Spring AI initializer. 위 두 기준과 일치해야 하며 독립적인 정본이 아니다.
4. 시각화: [PostForge MVP ERD](./postforge-mvp-erd.md)와 DBML. 위 기준에서 파생한다.

## Ownership Rules

- 하나의 table은 하나의 owning module만 갖는다.
- owning module만 해당 table의 entity/repository/write policy를 변경한다.
- 다른 module이 owning module의 table을 직접 읽거나 쓰면 안 된다. 필요한 경우 `core` port, API, message contract로 우회한다.
- table/column/index/constraint를 바꾸면 같은 변경에서 이 문서를 갱신한다.
- destructive change는 PR/release note 또는 별도 SQL artifact에 rollback/compatibility note를 남긴다.
- 운영 적용은 Flyway runtime migration을 기본 경로로 사용한다.

## Relational Tables

아래 표는 현재 구현의 owner 기준이다. "Target Schema Candidates"는 실제 migration이나 mapping이 생기기 전까지 운영 schema가 아니다.

| Owner | Table | Source | Notes |
| --- | --- | --- | --- |
| `auth` | `accounts` | `auth/account/domain/Account.java` | 계정 identity, OAuth provider identity, account fields, optimistic lock version |
| `auth` | `account_roles` | `Account.roles` `@CollectionTable` | account role set; `accounts` lifecycle에 종속 |
| `board` | `posts` | `board/post/domain/Post.java` | 게시글 본문, summary/tags/category/publish_origin, 조회수, like count, 작성자 account id와 nickname snapshot |
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
| `catalog` | `products` | `catalog/product/domain/Product.java` | 공개 조회에 사용하는 상품 대표 레코드; 현재 cross-source canonical truth는 보장하지 않음 |
| `catalog` | `offers` | `catalog/product/domain/Offer.java` | product에 연결된 source/mall별 외부 판매 상품 식별자; 현재 가격 필드는 없음 |
| `catalog` | `product_embeddings` | `catalog/matching/infrastructure/persistence/ProductEmbeddingSchemaInitializer.java`, `ProductEmbeddingJdbcStore.java` | product matching용 pgvector table; HNSW cosine index |
| `catalog` | `product_match_candidates` | `catalog/matching/domain/ProductMatchCandidate.java` | 자동 매칭 확신이 낮은 상품 병합 후보 |
| `price` | `price_snapshots` | `price/tracking/domain/PriceSnapshot.java` | offer 수집 시점별 가격 스냅샷 |
| `messaging` | `outbox_events` | `messaging/outbox/domain/OutboxMessage.java` | standalone reliable event handoff table; no domain-table FK |
| `ai` | `vector_store` | `ai/search/infrastructure/vector/PgVectorConfig.java`, Spring AI PgVector default | RAG document embeddings |

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

기능 책임을 이 표에서 다시 정의하지 않는다. 현재 모듈 책임은 [Module Dependency Policy](../architecture/module-dependencies.md#모듈-책임)를 따른다.

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

## 마이그레이션 규칙

런타임 마이그레이션은 `app/src/main/resources/db/migration/`의 `VNNNN__description.sql`에 둔다.
신규 빈 DB는 `V0000` baseline부터 실행하고, production-like 환경은 Flyway 적용 후 Hibernate `validate`로 mapping 불일치를 잡는다.
로컬 개발은 기존 DB를 자동 편입하지 않기 위해 Flyway를 기본 비활성화하고 필요할 때만 켠다.

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
3. 로컬에서 Flyway를 확인한다.

```bash
SPRING_FLYWAY_ENABLED=true ./gradlew :app:bootRun
```

4. 리뷰 mirror를 `docs/database/migrations/`에 둘 경우 runtime migration과 byte-for-byte로 같게 유지한다.

### 기존 DB 최초 편입

기존 non-empty DB를 편입하는 1회성 실행에서만 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`와
`SPRING_FLYWAY_BASELINE_VERSION=0`을 사용한다. 편입 전에는 기존 schema가 `V0000`과 호환되는지 rehearsal하고,
완료 후 `baseline-on-migrate`를 다시 `false`로 돌린다.
