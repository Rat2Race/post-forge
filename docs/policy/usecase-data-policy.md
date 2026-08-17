# Use Case Data Policy

이 문서는 유스케이스별 data read/write ownership만 정리한다. HTTP contract는 `../api/`, 사용자 흐름은 `../learning/postforge-user-scenarios.md`, schema 정본은 `../database/schema-ownership.md`를 따른다.

`Target` 절의 저장소는 현재 code-backed schema가 아니다.

## Auth

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 이메일 인증 발송 | Guest | `accounts.email`, Redis `email_verify_send:*` | `email_verify_token:{token}`, Redis `email_verify_send:*` |
| 이메일 인증 확인 | Guest | `email_verify_token:{token}` | token 삭제, `email_verified:{email}` |
| 회원가입 | Guest | `email_verified:{email}`, `accounts.user_id`, `accounts.email`, `accounts.nickname` | `accounts`, `account_roles`, 인증 상태 삭제 |
| 로그인 | Guest | `accounts.user_id`, `accounts.user_pw`, `accounts.nickname`, `account_roles`, `auth:login:*` | `refresh_token:{accountId}`, 로그인 보호 상태 |
| 로그아웃 | Member | 인증 principal | `refresh_token:{accountId}` 삭제 |
| 토큰 재발급 | Member | refresh token claims/store, `accounts`, `account_roles` | 새 `refresh_token:{accountId}` |

## Public Board

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 게시글 목록·검색 | Guest, Member | `posts`, `post_tags`, 파생 count | 없음 |
| 게시글 상세 | Guest, Member | `posts`, `post_file`, `post_tags`, 출시 뉴스이면 `post_reference_links`, 투표 집계 | Redis view count와 중복 조회 방지 key |
| 게시글 작성 | Member | 인증 principal, 첨부 file id | `posts`, `post_tags`, `post_file.post_id` |
| 게시글 수정 | Member, Admin | `posts`, 기존·신규 첨부 file | `posts`, `post_file.post_id` |
| 게시글 삭제 | Member, Admin | `posts`, 연결된 file/cache | `posts` 삭제, file 연결 해제, Redis view count 정리 |

## Target: Private Workspace And Reports

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 기본 workspace 생성 | Member | `accounts`, `account_subscriptions` | `workspaces`, `workspace_members` |
| draft 목록 조회 | Member | `workspaces`, `workspace_members`, `drafts` | 없음 |
| draft 작성·수정 | Member | workspace membership, plan limit, `drafts` | `drafts` |
| draft 출처 저장 | Member | `drafts`, `collected_items`, `trend_clusters` | `draft_sources` |
| trend bundle 저장 | Member | workspace membership, plan limit, `trend_clusters` | `saved_trend_bundles`, `saved_trend_bundle_items` |
| draft 공개 발행 | Member | `drafts`, `draft_sources`, workspace membership | `posts`, `post_reference_links`, `drafts.status`, `drafts.published_post_id` |
| private report 조회 | Member | private `posts`, workspace membership | 없음 |

## Source / Product Ingest

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 수집 키워드 관리 | Admin | `tracked_keywords` | `tracked_keywords` |
| 상품 수집 실행 | Admin, System | `tracked_keywords`, source adapter 설정 | `collection_jobs`, `raw_products`, catalog/price tables |
| source 실패 기록 | System | `collection_jobs` | failure reason/status |
| 가격 스냅샷 기록 | System | catalog `products`/`offers`, 수집 결과 | `price_snapshots` |
| 뉴스 문서 수집 | Admin, System | News source adapter 설정, keyword/topics | `vector_store` document embeddings |
| 출시 뉴스 자동 게시 | Admin, System | News 후보, `post_reference_links.canonical_url`, daily cap metadata, AI draft port | `posts`, `post_tags`, `post_reference_links` |

출시 뉴스 gate와 skip reason은 [통합 API 명세의 Ingest](../api/README.md#ingest)를 따른다.

## Price Judgement

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 가격 판정 요청 | Member | 대체 상품 source adapter 결과 — 현재 미연결 | 없음 |

계산과 응답 계약은 [통합 API 명세의 Price](../api/README.md#price)를 따른다.

## Messaging

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| outbox 이벤트 저장 | Domain module | aggregate id/type | `outbox_events(status=PENDING)` |
| relay claim | System | claimable `outbox_events` | `status=PROCESSING` |
| 전달 성공 | System | processing event | `status=PUBLISHED`, `published_at` |
| 전달 실패 | System | processing event, 예외 | `status=FAILED`, retry fields |

## Target: Keyword Notification

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 키워드 구독 관리 | Member | 본인 `keyword_subscriptions` | `keyword_subscriptions` |
| 수집 item 매칭 | System | `collected_items`, 활성 subscription | `notification_events` |
| 이메일 알림 발송 | System | pending notification, `accounts.email`, item | `email_delivery_logs`, notification status |

## Target: AI Assist And Cost

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| AI draft/report assist | Paid Member | draft/source, subscription, budget window | `ai_usage_logs`, draft assist 상태 |
| 출시 뉴스 AI draft | Admin, System | gate 통과 후보, system budget window | `ai_usage_logs`, 게시 flow 입력 |
| quota 거절 | Member, System | subscription, plan, budget window | rejected `ai_usage_logs` 또는 거절 event |

## Comment

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 댓글 작성 | Member | `posts`, 부모 `comments` | `comments` |
| 댓글 조회 | Guest, Member | `comments`, like count, 본인 like | 없음 |
| 댓글 수정 | Member, Admin | `comments.id`, `comments.account_id` | content/updated time |
| 댓글 삭제 | Member, Admin | comment ownership, 하위 댓글 | `comments` 삭제, 파생 count 갱신 |

## Like

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 게시글 좋아요/취소 | Member | `posts`, `post_like`, Redis guard | `post_like`, `posts.like_count`, Redis guard |
| 댓글 좋아요/취소 | Member | `comments`, `comment_like`, Redis guard | `comment_like`, `comments.like_count`, Redis guard |

## Purchase Vote

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 투표/변경 | Member | `posts.category`, `posts.publish_origin`, 기존 vote | `post_purchase_vote` create/update |
| 투표 취소 | Member | 본인 vote | `post_purchase_vote` 삭제 |
| 집계 조회 | Guest, Member | vote aggregate, 회원이면 본인 vote | 없음 |

투표 접근 invariant는 [Access Policy](./access-policy.md)를 따른다.

## Profile And Account

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 프로필 조회 | Member | `accounts`, `account_roles` | 없음 |
| 닉네임 변경 | Member | `accounts.nickname` | nickname/updated time |
| 비밀번호 변경 | Member | `accounts`, provider | password/updated time, refresh token 삭제 |
| 회원 탈퇴 (Target) | Member | account 상태, 보존 대상 snapshot | `accounts.status=DELETED`, refresh token 삭제 |
