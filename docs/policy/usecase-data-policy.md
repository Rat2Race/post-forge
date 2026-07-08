# Use Case Data Policy

이 문서는 PostForge 유스케이스별 Read/Write 데이터를 정리한다.
현재 구현과 target schema 후보가 함께 나오므로, 구현 완료 전 테이블은 `target`으로 표시한다.

> 문서 경계: 이 문서는 use case별 data read/write ownership을 다룬다. HTTP contract는 `../api/`, 사용자 시나리오 narrative는 `../usecase/postforge-user-scenarios.md`, schema source of truth는 `../database/schema-ownership.md`를 따른다.

## Data Stores

### Implemented Stores

| 저장소 | 주요 데이터 |
| --- | --- |
| `accounts` | 회원 식별자, 이메일, 닉네임, 비밀번호 해시, provider 정보, 계정 상태 |
| `account_roles` | 회원 권한 |
| `posts` | 게시글 본문, 작성자 스냅샷, category, 조회수, 좋아요 수 |
| `post_tags` | 게시글 태그 |
| `post_file` | 게시글 첨부파일 메타데이터와 게시글 연결 |
| `post_product_links` | 상품 관련 게시글과 상품 연결 |
| `post_reference_links` | 출시 뉴스 게시글의 provider/canonical URL/source evidence와 daily cap 기준 metadata |
| `post_purchase_vote` | 자동 게시된 출시 뉴스에 대한 회원별 구매 판단 투표 |
| `comments` | 댓글/대댓글, 작성자 스냅샷, 좋아요 수 |
| `post_like` | 게시글 좋아요 |
| `comment_like` | 댓글 좋아요 |
| `tracked_keywords`, `collection_jobs`, `raw_products` | 현재 상품 source 수집 대상, 작업 이력, 원본 payload |
| `products`, `product_categories`, `offers` | 정규화 상품, 카테고리, source/mall별 판매 상품 |
| `product_embeddings`, `product_match_candidates` | pgvector 상품 임베딩과 낮은 확신도 유사 상품 매칭 후보 |
| `price_snapshots` | 수집 시점별 가격 이력과 프론트 가격 그래프 원천 데이터 |
| `vector_store` | Spring AI PgVector 문서 |
| Redis `refresh_token:*` | refresh token 저장과 rotation 검증 |
| Redis `oauth2_code:*` | OAuth2 callback 이후 프론트 교환용 short-lived code |
| Redis `email_verify_token:*` | 이메일 인증 토큰 |
| Redis `email_verified:*` | 회원가입 전 이메일 인증 완료 상태 |

### Target Stores

| 저장소 | 주요 데이터 |
| --- | --- |
| `collector_sources` | legacy/target 외부 source/provider 기준 정보. 현재 코드에는 없음 |
| `collector_source_policies` | legacy/target timeout, retry, quota, circuit state. 현재 코드에는 없음 |
| `collector_jobs` | legacy/target 수집 작업 이력. 현재 코드는 `collection_jobs` 사용 |
| `collector_api_requests` | legacy/target 외부 API 호출 attempt 이력. 현재 코드에는 없음 |
| `collected_items` | 확장된 수집 item 원본 metadata |
| `keyword_subscriptions` | 사용자별 관심 키워드 구독 |
| `notification_events` | 수집 item과 keyword subscription 매칭 결과 |
| `email_delivery_logs` | 이메일 발송 성공/실패/재시도 이력 |
| `trend_clusters` | future 관련 트렌드 묶음 read model |
| `trend_cluster_items` | future trend cluster와 collected item 연결 |
| `workspaces` | 개인 리포트 작업공간 |
| `workspace_members` | workspace 권한 |
| `drafts` | 비공개 초안/리포트 작성 상태 |
| `draft_sources` | draft에 저장한 출처/trend |
| `saved_trend_bundles` | 사용자가 저장한 trend 묶음 |
| `post_rank_scores` | future 내부 ranking snapshot |
| `subscription_plans` | plan별 quota/limit |
| `account_subscriptions` | account별 현재 plan |
| `ai_budget_windows` | account/system AI budget window |
| `ai_usage_logs` | 모든 AI operation 사용량/비용 이력 |

## Auth

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 이메일 인증 발송 | Guest | `accounts.email` 중복 여부 | `email_verify_token:{token}` |
| 이메일 인증 확인 | Guest | `email_verify_token:{token}` | `email_verify_token:{token}` 삭제, `email_verified:{email}` |
| 회원가입 | Guest | `email_verified:{email}`, `accounts.user_id`, `accounts.email`, `accounts.nickname` 중복 여부 | `accounts`, `account_roles`, 기본 `workspaces` target, `email_verified:{email}` 삭제 |
| 로그인 | Guest | `accounts.user_id`, `accounts.user_pw`, `accounts.nickname`, `account_roles` | `refresh_token:{accountId}` |
| 로그아웃 | Member | 인증 principal | `refresh_token:{accountId}` 삭제 |
| 토큰 재발급 | Member | refresh token claims, `refresh_token:{accountId}`, `accounts`, `account_roles` | 새 `refresh_token:{accountId}` |

## Public Board

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 공개 게시글 목록 조회 | Guest, Member | `posts`, `post_tags`, 파생 count. 현재 schema에는 `visibility`/`status` column이 없고 모든 게시글이 공개다 | 없음 |
| 공개 게시글 검색/정렬 | Guest, Member | 공개 `posts.title`, `posts.content`, `post_tags`, count/index 기반 정렬 | 없음 |
| 공개 게시글 상세 조회 | Guest, Member | 공개 `posts`, `post_file`, `post_tags` | `posts.views` 직접 증가 |
| 게시글 작성 | Member | 인증 principal, 첨부 `post_file` id 유효성 | `posts`, `post_tags`, 필요 시 `post_file.post_id` |
| 게시글 수정 | Member, Admin | `posts.id`, `posts.account_id`, 기존 `post_file`, 새 첨부 id | `posts.title`, `posts.content`, `posts.updated_at`, `post_file.post_id` 재연결 |
| 게시글 삭제 | Member, Admin | `posts.id`, `posts.account_id`, 연결된 `post_file`, 댓글/좋아요/조회수 파생 데이터 | `posts` row 물리 삭제(댓글/태그 cascade 포함), `post_file.post_id` 해제. soft delete(`status`/`deleted_at`)는 target policy다 |

주의:

- 공개 상세 조회는 AI를 호출하지 않는다.
- 출시 뉴스 게시글 상세/목록은 저장된 `post_reference_links`와 `post_purchase_vote` 집계를 읽는다. `myVote`는 인증된 회원에게만 포함한다.
- 게시판 목록 정렬은 최신순/조회순/좋아요순/댓글순 같은 count/index 기반 정렬로 시작한다.

## Private Workspace And Reports

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 기본 workspace 생성 | Member | `accounts`, `account_subscriptions` target | `workspaces`, `workspace_members` |
| draft 목록 조회 | Member | `workspaces`, `workspace_members`, `drafts` | 없음 |
| draft 작성 | Member | workspace membership, plan limit target | `drafts` |
| draft 수정 | Member | `drafts`, workspace membership | `drafts.title`, `drafts.content`, `drafts.updated_at` |
| draft에 출처 저장 | Member | `drafts`, `collected_items` target, future `trend_clusters` target | `draft_sources` |
| trend bundle 저장 | Member | workspace membership, plan bundle limit target, future `trend_clusters` target | `saved_trend_bundles`, `saved_trend_bundle_items` |
| draft를 공개 게시글로 발행 | Member | `drafts`, `draft_sources`, workspace membership | `posts`, future `post_reference_links`, `drafts.status`, `drafts.published_post_id` |
| private report 조회 | Member | `posts` where `visibility = PRIVATE`, workspace membership | 없음 |

주의:

- Admin은 moderation 권한만으로 private workspace를 조회하지 않는다.
- plan은 private draft/report 저장량과 AI quota에만 영향을 준다.

## Source / Product Ingest

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 수집 키워드 등록/수정 | Admin | 기존 `tracked_keywords` | `tracked_keywords` |
| 수동 상품 수집 실행 | Admin/System | `tracked_keywords` optional, source adapter 설정 | `collection_jobs`, `raw_products`, catalog `products`/`offers`, price snapshot tables |
| 스케줄 상품 수집 실행 | System | 활성 `tracked_keywords`, source adapter 설정 | `collection_jobs`, `raw_products`, catalog/price tables |
| 외부 source 호출 실패 기록 | System | `collection_jobs` | `collection_jobs.failure_reason`, `collection_jobs.status` |
| 가격 스냅샷 기록 | System | catalog `products`/`offers`, 수집 결과 | `price_snapshots` |
| 수동 뉴스 문서 수집 | Admin/System | Naver News source adapter 설정, keyword/topics 요청 | `vector_store` document embeddings |
| 출시 뉴스 자동 게시 | Admin/System | Naver News source adapter 설정, `post_reference_links.canonical_url`, daily cap metadata, AI draft port | `posts(category=PRODUCT_LAUNCH_NEWS)`, `post_tags`, `post_reference_links` |

주의:

- 현재 코드는 `collector_sources`, `collector_source_policies`, `collector_api_requests`를 사용하지 않는다.
- 실제 외부 상품 source는 Naver Shopping adapter를 통해 호출하며, `MOCK` source는 로컬/테스트용으로 명시 선택한다.
- 실제 외부 뉴스 source는 Naver News adapter를 통해 문서 적재 흐름으로 보낸다.
- 출시 뉴스 자동 게시 흐름은 문서 적재 흐름과 별도이며, 모든 수집 item을 AI 게시글로 만들지 않는다.
- 출시 뉴스 후보는 중복 기사, 광고성 기사, 출처 불명, 필수 키워드 미포함, AI 요약 실패, 같은 상품/키워드 당 하루 N개 초과 필터를 통과해야 한다.
- 상품 수집은 raw payload, catalog 상품/offer, 가격 snapshot까지 저장하는 동작 경로에 집중한다.
- 가격 수집은 상승/하락 이벤트를 만들지 않고 snapshot history만 저장한다.

## Price Judgement

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 가격 판정 요청 | Member | Naver Shopping source adapter 결과 | 없음 |

주의:

- `POST /api/price-checks`는 response-only API이며 `price_snapshots`나 게시글을 만들지 않는다.
- 요청은 상품명/옵션/판매가/배송비/쿠폰·카드 할인/최종 결제액을 받을 수 있고, `finalPaidPrice`가 있으면 실제 구매가 기준으로 우선 사용한다.
- 현재 구현은 Naver Search Shopping 샘플의 배송비 포함 여부를 검증하지 않으므로, 모든 응답에 `shippingIncludedVerified=false`와 `배송비 포함 여부 미확인` 경고 문구를 포함하고 confidence는 항상 `LOW`다.
- 배송비 불확실성이 판정을 뒤집을 수 있는 경계 구간이거나 비교 샘플이 없으면 `INSUFFICIENT_INFO`로 응답한다.

## Messaging

MVP에서 `messaging`은 별도 DB table 없이 transaction commit 이후 같은 프로세스 안에서 event publisher를 호출한다.
이벤트의 업무 의미와 저장 데이터는 각 기능 모듈이 소유한다.

## Keyword Notification

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 키워드 구독 등록 | Member | 인증 principal, 기존 `keyword_subscriptions(account_id, keyword)` | `keyword_subscriptions` |
| 키워드 구독 비활성화 | Member | 본인 `keyword_subscriptions.id` | `keyword_subscriptions.enabled`, `updated_at` |
| 수집 item 매칭 | System | 새 `collected_items`, 활성 `keyword_subscriptions` | `notification_events` |
| 이메일 알림 발송 | System | `notification_events(status = PENDING)`, `accounts.email`, `collected_items` | `email_delivery_logs`, `notification_events.status`, `processed_at` |
| 이메일 발송 실패 기록 | System | `notification_events`, 발송 예외 | `email_delivery_logs(status = FAILED)`, `notification_events(status = FAILED)` |

주의:

- 같은 subscription과 collected item 조합은 중복 알림을 만들지 않는다.
- domain event는 "어떤 일이 발생했는가"를 전달하고, `notification_events`는 "누구에게 무엇을 알려야 하는가"를 기록한다.
- 메일 발송 실패는 사용자 요청 실패로 전파하지 않고 상태와 오류 메시지로 남긴다.

## AI Assist And Cost

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 기본 관련 정보 추천 | Member | `drafts`, `collected_items`, future `trend_clusters` | 없음 |
| AI 리포트 개요 생성 | Paid Member | `drafts`, `draft_sources`, `account_subscriptions`, `ai_budget_windows` | `ai_usage_logs`, 필요 시 `drafts.assist_used` |
| AI 수집 자료 요약 | Paid Member | `collected_items`, `draft_sources`, `ai_budget_windows` | `ai_usage_logs` |
| AI 문장 개선 | Paid Member | `drafts`, `ai_budget_windows` | `ai_usage_logs`, `drafts.assist_used` |
| 출시 뉴스 AI 초안 생성 | Admin/System | Naver News 후보, duplicate/ad/source/keyword/day-limit filters, system `ai_budget_windows` | `ai_usage_logs`, `posts(category = PRODUCT_LAUNCH_NEWS)`, `post_reference_links` |
| quota 초과 거절 | Member/System | `account_subscriptions`, `subscription_plans`, `ai_budget_windows` | `ai_usage_logs(status = REJECTED_BY_QUOTA)` 또는 거절 이벤트 |

주의:

- AI 호출은 명시적 operation type을 가져야 한다.
- AI 호출 성공/실패/거절은 모두 기록 대상이다.
- 게시글 조회, 목록 조회, 댓글 조회는 AI usage log를 만들면 안 된다.

## Comment

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 댓글 작성 | Member | 공개 `posts.id`, 대댓글이면 부모 `comments.id`, 부모 댓글의 `post_id`와 depth | `comments`, 필요 시 부모-자식 관계 |
| 댓글 조회 | Guest, Member | 공개 게시글의 `comments.post_id`, 댓글 좋아요 수, 회원이면 본인 댓글 좋아요 여부 | 없음 |
| 댓글 수정 | Member, Admin | `comments.id`, `comments.account_id` | `comments.content`, `comments.updated_at` |
| 댓글 삭제 | Member, Admin | `comments.id`, `comments.account_id`, 하위 댓글 관계, 댓글 좋아요 파생 데이터 | `comments` row 물리 삭제(하위 대댓글 cascade 포함), 댓글 수 파생 데이터 갱신. soft delete(`status`/`deleted_at`)는 target policy다 |

## Like

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 게시글 좋아요 | Member | 공개 `posts.id`, `post_like(post_id, account_id)` 존재 여부 | `post_like`, `posts.like_count` |
| 게시글 좋아요 취소 | Member | 공개 `posts.id`, `post_like(post_id, account_id)` | `post_like` 삭제, `posts.like_count` |
| 댓글 좋아요 | Member | `comments.id`, `comment_like(comment_id, account_id)` 존재 여부 | `comment_like`, `comments.like_count` |
| 댓글 좋아요 취소 | Member | `comments.id`, `comment_like(comment_id, account_id)` | `comment_like` 삭제, `comments.like_count` |

## Purchase Vote

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 구매 판단 투표 | Member | `posts.id`, `posts.category`, `posts.publish_origin`, 기존 `post_purchase_vote(post_id, account_id)` | `post_purchase_vote` create/update |
| 구매 판단 투표 취소 | Member | `post_purchase_vote(post_id, account_id)` | `post_purchase_vote` 삭제 |
| 구매 판단 집계 조회 | Guest, Member | `post_purchase_vote` aggregate, 회원이면 본인 vote | 없음 |

주의:

- 투표는 `PRODUCT_LAUNCH_NEWS` 및 `publish_origin=SYSTEM_BATCH` 게시글에만 허용한다.
- `ADMIN_BACKFILL` 출시 뉴스와 일반 게시글은 투표를 거절한다.

## Profile And Account

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 프로필 조회 | Member | `accounts`, `account_roles`, `account_subscriptions` target | 없음 |
| 닉네임 변경 | Member | `accounts.user_id`, `accounts.nickname` 중복 여부 | `accounts.nickname`, `accounts.updated_at` |
| 비밀번호 변경 | Member | `accounts.id`, `accounts.user_pw`, `accounts.provider` | `accounts.user_pw`, `accounts.updated_at`, `refresh_token:{accountId}` 삭제 |
| 회원 탈퇴 (target — 탈퇴 API 미구현) | Member | `accounts.id`, 계정 상태, 보존 대상 게시글/댓글 스냅샷 | `accounts.status = DELETED`, `refresh_token:{accountId}` 삭제. `AccountStatus`는 `ACTIVE`/`SUSPENDED`/`DELETED`이며 `deleted_at` column은 없다 |

## Boundaries

- Guest는 공개 게시판 읽기와 인증 진입을 제외하고 영속 데이터를 쓰지 않는다.
- Member 쓰기 유스케이스는 인증 principal의 `account_id`로 소유권을 확인한다.
- Admin 예외 권한은 공개 게시글/댓글 moderation과 상품 source/ingest 운영에 한정한다.
- private workspace는 owner/member 권한으로만 접근한다.
- Redis 데이터는 refresh token, 이메일 인증 token/state, OAuth2 exchange code처럼 인증 기능 상태만 관리한다.
- `like_count`, `comment_count`, `view_count`는 원본이 아니라 다시 계산 가능한 파생 데이터다.
- AI 비용은 `ai_usage_logs`와 `ai_budget_windows` 없이는 운영할 수 없다.
- 공개 게시판 신뢰 신호는 plan으로 나누지 않는다.
