# Use Case Data Policy

이 문서는 PostForge 유스케이스별 Read/Write 데이터를 정리한다.
현재 구현과 target schema 후보가 함께 나오므로, 구현 완료 전 테이블은 `target`으로 표시한다.

## Data Stores

### Implemented Stores

| 저장소 | 주요 데이터 |
| --- | --- |
| `accounts` | 회원 식별자, 이메일, 닉네임, 비밀번호 해시, provider 정보, 계정 상태 |
| `account_roles` | 회원 권한 |
| `posts` | 게시글 본문, 작성자 스냅샷, category, 조회수, 좋아요 수 |
| `post_tags` | 게시글 태그 |
| `post_file` | 게시글 첨부파일 메타데이터와 게시글 연결 |
| `comments` | 댓글/대댓글, 작성자 스냅샷, 좋아요 수 |
| `post_like` | 게시글 좋아요 |
| `comment_like` | 댓글 좋아요 |
| `collected_articles` | 현재 Naver News 수집 이력 |
| `outbox_events` | standalone messaging 모듈의 발행 대기 이벤트 |
| `vector_store` | Spring AI PgVector 문서 |
| Redis `refresh_token:*` | refresh token 저장과 rotation 검증 |
| Redis `email_verify_token:*` | 이메일 인증 토큰 |
| Redis `email_verified:*` | 회원가입 전 이메일 인증 완료 상태 |
| Redis `auth:login:*` | 로그인 실패 누적, 잠금, rate limit 상태 |
| Redis `like:*` | 좋아요 요청 cooldown/rate limit 상태 |
| Redis view count keys | 게시글 조회수 캐시와 중복 조회 방지 상태 |

### Target Stores

| 저장소 | 주요 데이터 |
| --- | --- |
| `collector_sources` | 외부 source/provider 기준 정보 |
| `collector_source_policies` | timeout, retry, quota, circuit state |
| `collector_jobs` | 수집 작업 이력 |
| `collector_api_requests` | 외부 API 호출 attempt 이력 |
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
| `post_reference_links` | future 발행 글이 참고한 collected item/trend/external URL |
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
| 로그인 | Guest | `accounts.user_id`, `accounts.user_pw`, `accounts.nickname`, `account_roles`, `auth:login:*` 제한 상태 | `refresh_token:{accountId}`, 실패/잠금 키 갱신 또는 삭제 |
| 로그아웃 | Member | 인증 principal | `refresh_token:{accountId}` 삭제 |
| 토큰 재발급 | Member | refresh token claims, `refresh_token:{accountId}`, `accounts`, `account_roles` | 새 `refresh_token:{accountId}` |

## Public Board

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 공개 게시글 목록 조회 | Guest, Member | `posts` where `visibility = PUBLIC`, `status = PUBLISHED`, `post_tags`, 파생 count | 없음 |
| 공개 게시글 검색/정렬 | Guest, Member | 공개 `posts.title`, `posts.content`, `post_tags`, count/index 기반 정렬 | 없음 |
| 공개 게시글 상세 조회 | Guest, Member | 공개 `posts`, `post_file`, `post_tags` | Redis view count 증가와 중복 조회 방지 키 |
| 게시글 작성 | Member | 인증 principal, 첨부 `post_file` id 유효성 | `posts`, `post_tags`, 필요 시 `post_file.post_id` |
| 게시글 수정 | Member, Admin | `posts.id`, `posts.account_id`, 기존 `post_file`, 새 첨부 id | `posts.title`, `posts.content`, `posts.updated_at`, `post_file.post_id` 재연결 |
| 게시글 삭제 | Member, Admin | `posts.id`, `posts.account_id`, 연결된 `post_file`, 댓글/좋아요/조회수 파생 데이터 | `posts.status`, `posts.deleted_at`, `post_file.post_id` 해제, Redis view count 정리 |

주의:

- 공개 상세 조회는 AI를 호출하지 않는다.
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

## Collector

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| source 등록/수정 | Admin | `collector_sources` target | `collector_sources`, `collector_source_policies` |
| 수동 수집 실행 | Admin/System | `collector_sources`, `collector_source_policies`, quota/circuit state | `collector_jobs`, `collector_api_requests`, `collected_items` |
| 스케줄 수집 실행 | System | `collector_sources`, `collector_source_policies`, quota/circuit state | `collector_jobs`, `collector_api_requests`, `collected_items` |
| API request 실패 기록 | System | `collector_jobs`, `collector_sources` | `collector_api_requests`, 필요 시 `collector_source_policies.circuit_state` |
| keyword notification 매칭 | System | `collected_items`, 활성 `keyword_subscriptions` | `notification_events` |

주의:

- 외부 API 요청은 source policy를 통과해야 한다.
- 모든 호출 attempt는 request log로 남긴다.
- collector는 public post를 직접 생성하지 않고, 수집 item과 키워드 알림 후보를 만든다.
- 수집 item 생성 이후 여러 후처리가 필요하면 후속 phase에서 event contract를 정한 뒤 `messaging` 연결 여부를 결정한다.

## Messaging

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| outbox 이벤트 저장 | Messaging/Future Domain Module | aggregate id/type logical reference | `outbox_events(status = PENDING)` |
| outbox relay claim | System | `outbox_events(status in PENDING, FAILED)`, `available_at` | `outbox_events(status = PROCESSING)` |
| 이벤트 전달 성공 | System | `outbox_events(status = PROCESSING)` | `outbox_events(status = PUBLISHED)`, `published_at` |
| 이벤트 전달 실패 | System | `outbox_events(status = PROCESSING)`, 전달 예외 | `outbox_events(status = FAILED)`, `retry_count`, `available_at`, `last_error` |

주의:

- `outbox_events`는 업무 알림 테이블이 아니라 기술 이벤트 envelope이다.
- 현재 `board`, `collector`, `notification`은 `messaging`을 직접 의존하지 않는다.
- 기능 모듈에 outbox를 연결할 때는 도메인 변경과 같은 `@Transactional` 경계 안에서 저장해야 한다.
- MQ를 붙이면 relay가 `outbox_events`를 읽어 broker로 publish한다.

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
- `outbox_events`는 "어떤 일이 발생했는가"를 전달하고, `notification_events`는 "누구에게 무엇을 알려야 하는가"를 기록한다.
- 메일 발송 실패는 사용자 요청 실패로 전파하지 않고 상태와 오류 메시지로 남긴다.

## AI Assist And Cost

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 기본 관련 정보 추천 | Member | `drafts`, `collected_items`, future `trend_clusters` | 없음 |
| AI 리포트 개요 생성 | Paid Member | `drafts`, `draft_sources`, `account_subscriptions`, `ai_budget_windows` | `ai_usage_logs`, 필요 시 `drafts.assist_used` |
| AI 수집 자료 요약 | Paid Member | `collected_items`, `draft_sources`, `ai_budget_windows` | `ai_usage_logs` |
| AI 문장 개선 | Paid Member | `drafts`, `ai_budget_windows` | `ai_usage_logs`, `drafts.assist_used` |
| AI brief 생성 | Admin/System | future `trend_clusters`, `trend_cluster_items`, `collected_items`, system `ai_budget_windows` | `ai_usage_logs`, `posts(post_type = AI_BRIEF)`, future `post_reference_links` |
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
| 댓글 삭제 | Member, Admin | `comments.id`, `comments.account_id`, 하위 댓글 관계, 댓글 좋아요 파생 데이터 | `comments.status`, `comments.deleted_at`, 하위 댓글 상태, 댓글 수/좋아요 수 갱신 |

## Like

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 게시글 좋아요 | Member | 공개 `posts.id`, `post_like(post_id, account_id)` 존재 여부, Redis `like:*` 제한 상태 | `post_like`, `posts.like_count`, Redis `like:*` |
| 게시글 좋아요 취소 | Member | 공개 `posts.id`, `post_like(post_id, account_id)`, Redis `like:*` 제한 상태 | `post_like` 삭제, `posts.like_count`, Redis `like:*` |
| 댓글 좋아요 | Member | `comments.id`, `comment_like(comment_id, account_id)` 존재 여부, Redis `like:*` 제한 상태 | `comment_like`, `comments.like_count`, Redis `like:*` |
| 댓글 좋아요 취소 | Member | `comments.id`, `comment_like(comment_id, account_id)`, Redis `like:*` 제한 상태 | `comment_like` 삭제, `comments.like_count`, Redis `like:*` |

## Profile And Account

| 유스케이스 | Actor | Read 데이터 | Write 데이터 |
| --- | --- | --- | --- |
| 프로필 조회 | Member | `accounts`, `account_roles`, `account_subscriptions` target | 없음 |
| 닉네임 변경 | Member | `accounts.user_id`, `accounts.nickname` 중복 여부 | `accounts.nickname`, `accounts.updated_at` |
| 비밀번호 변경 | Member | `accounts.id`, `accounts.user_pw`, `accounts.provider` | `accounts.user_pw`, `accounts.updated_at`, `refresh_token:{accountId}` 삭제 |
| 회원 탈퇴 | Member | `accounts.id`, 계정 상태, 보존 대상 게시글/댓글/workspace 스냅샷 | `accounts.status`, `accounts.deleted_at`, `refresh_token:{accountId}` 삭제, subscription 상태 갱신 target |

## Boundaries

- Guest는 공개 게시판 읽기와 인증 진입을 제외하고 영속 데이터를 쓰지 않는다.
- Member 쓰기 유스케이스는 인증 principal의 `account_id`로 소유권을 확인한다.
- Admin 예외 권한은 공개 게시글/댓글 moderation과 collector 운영에 한정한다.
- private workspace는 owner/member 권한으로만 접근한다.
- Redis 데이터는 토큰, 인증 보호, 좋아요 보호, 조회수처럼 보조 상태만 관리한다.
- `like_count`, `comment_count`, `view_count`는 원본이 아니라 다시 계산 가능한 파생 데이터다.
- AI 비용은 `ai_usage_logs`와 `ai_budget_windows` 없이는 운영할 수 없다.
- 공개 게시판 신뢰 신호는 plan으로 나누지 않는다.
