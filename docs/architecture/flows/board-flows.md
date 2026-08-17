# Board API 흐름

Endpoint 명세는 [통합 API 명세의 Board](../../api/README.md#board)가 canonical이다.

## GET /api/posts — 목록/검색

1. `PostQueryService.getPosts` (`@Transactional(readOnly = true)`) — keyword/category/publishOrigin 필터를 JPA Specification으로 조합해 페이징 조회.
2. 목록 전체의 파생 데이터를 **postIds 단위 batch 조회**로 채운다: 좋아요 수/본인 좋아요 여부, 조회수(Redis, miss는 DB fallback), 댓글 수, 투표 집계, reference link.

원리: 게시글 N건을 돌며 건별 조회하면 N+1이 된다. batch 조회의 측정 결과는 [N+1 분석](../../performance/n-plus-one-analysis.md)에 한 번만 기록한다.

## GET /api/posts/{postId} — 상세 + 조회수

1. `PostQueryService.readPost` — **인증된 사용자일 때만** `ViewCountService.incrementIfNew(postId, accountId)`를 호출한다. 비회원 조회는 조회수를 올리지 않는다.
2. 조회수 저장소가 account 단위 중복 조회를 막고 캐시 증가분을 동기화 대상으로 표시한다. 정확한 key와 상태 전이는 [Redis 캐시 전략](../redis-cache-strategy.md)을 따른다.
3. 응답 조립은 조회수/좋아요/댓글 수/투표 집계/reference를 모아 `PostDetailResponse`로.

조회수 동기화와 허용 손실 범위는 [Redis 캐시 전략](../redis-cache-strategy.md), 선택 근거는 [ADR-001](../../decisions/adr-001-use-redis-for-view-count.md)을 따른다.

## POST/PUT/DELETE /api/posts — 쓰기

- 작성: `PostCommandService` (`@Transactional`)가 principal의 `accountId` + nickname snapshot을 저장하고, `fileIds`가 있으면 업로드된 파일 metadata를 게시글에 연결한다. 공개 작성 API의 category는 항상 `GENERAL`로 고정된다.
- 수정: owner 또는 ADMIN(`isOwner` 검사는 `PostPolicy`). 파일 연결은 replace 방식.
- 삭제: 파일 연결 해제 → Redis 조회수 캐시 삭제 → **게시글 row 물리 삭제**(댓글/태그 cascade) → `PostDeletedEvent`를 `DomainEventRecorder`로 기록. soft delete는 target policy다([delete-policy](../../policy/delete-policy.md)).

## POST/DELETE /api/posts/{postId}/like — 좋아요

1. `PostInteractionService` (`@Transactional`)가 게시글 존재를 확인한다.
2. `LikeRequestGuard` — Redis로 대상별 cooldown(`markCooldownIfAbsent`) + 계정별 분당 30회 rate limit. 초과하거나 **Redis 장애면 fail-closed로 `429`**.
3. `AbstractLikeService.likeTarget` — 존재 확인 후 insert하되, 동시 요청으로 unique constraint(`post_like(post_id, account_id)`)에 걸리면 `DataIntegrityViolationException`을 무시한다(멱등). 이후 count를 DB에서 재집계해 `posts.like_count`에 갱신한다.

원리: 좋아요는 조회수와 달리 사용자별 상태의 정합성이 중요하므로 Redis write-behind를 쓰지 않고 DB를 source of truth로 유지한다. Redis는 보호 장치로만 쓴다. `like_count`는 like row에서 재계산 가능한 파생 데이터다. 결정 기록: [ADR-001](../../decisions/adr-001-use-redis-for-view-count.md)의 대안 검토 표.

## 댓글 (POST/GET/PUT/DELETE /api/posts/{postId}/comments)

- 작성: `parentId`가 있으면 같은 게시글의 부모 댓글인지 확인 후 1-depth 대댓글로 저장.
- 삭제: **row 물리 삭제**, 하위 대댓글은 `cascade = ALL, orphanRemoval = true`로 함께 삭제.
- 댓글 좋아요는 게시글 좋아요와 같은 `AbstractLikeService` 계층을 상속해 동일한 guard/멱등 규칙을 따른다.

## PUT/DELETE /api/posts/{postId}/purchase-vote — 구매 판단 투표

1. `PurchaseVoteService.vote` (`@Transactional`) — `PurchaseVoteEligibility`가 `category = PRODUCT_LAUNCH_NEWS`이면서 `publish_origin = SYSTEM_BATCH`인 게시글만 허용. 아니면 `PURCHASE_VOTE_NOT_ALLOWED`.
2. 기존 투표가 있으면 vote type만 update, 없으면 insert. unique constraint `(post_id, account_id)`로 계정당 1표를 보장한다.
3. 응답은 `BUYABLE`/`UNSURE`/`WAIT` 집계 + 본인 vote(`myVote`는 인증 회원만).

원리: 투표는 자동 게시된 출시 뉴스의 신뢰 신호이므로 사용자 수동 글과 `ADMIN_BACKFILL`에는 열지 않는다. 정책: [access-policy](../../policy/access-policy.md)

## GET /api/files/presigned-url — 파일 업로드

1. `FileUploadService` — `FileTypePolicy`로 파일명/contentType을 검증한다.
2. `post_file` metadata를 먼저 저장하고(UUID 저장명, 날짜 경로 object key), S3 presigned upload URL을 발급해 fileId와 함께 반환한다.
3. 실제 바이트 업로드는 클라이언트가 S3에 직접 한다. 게시글 작성/수정 시 fileId로 연결되며, 연결되지 않은 파일은 `OrphanFileCleanupScheduler`가 정리 대상으로 삼는다.

원리: 앱 서버가 파일 바이트를 중계하지 않으므로 업로드 트래픽이 WAS 용량을 소모하지 않는다.
