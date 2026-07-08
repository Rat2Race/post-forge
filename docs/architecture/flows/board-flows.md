# Board API 흐름

Endpoint 명세는 [Board API](../../api/board.md)가 canonical이다.

## 모듈 구조

```text
board
├── post      게시글 CRUD/조회 (PostCommandService, PostQueryService), reference/product link
├── comment   댓글/대댓글 (CommentCommandService, CommentQueryService)
├── like      게시글/댓글 좋아요 (AbstractLikeService 계층)
├── purchase  구매 판단 투표 (PurchaseVoteService, PurchaseVoteEligibility)
├── view      DB 직접 조회수 (ViewCountService)
├── file      S3 presigned URL, orphan 파일 정리
└── profile   프로필 조회/변경 (auth의 계정 데이터를 core port로 접근)
```

## GET /api/posts — 목록/검색

1. `PostQueryService.getPosts` (`@Transactional(readOnly = true)`) — keyword/category/boardCategory/publishOrigin 필터를 JPA Specification으로 조합해 페이징 조회.
2. 목록 전체의 파생 데이터를 postIds 기준으로 조회해 채운다: 좋아요 수/본인 좋아요 여부, DB 조회수, 댓글 수, 투표 집계, reference link.

## GET /api/posts/{postId} — 상세 + 조회수

1. `PostQueryService.readPost` — **인증된 사용자일 때만** `ViewCountService.incrementIfNew(postId, accountId)`를 호출한다. 비회원 조회는 조회수를 올리지 않는다.
2. `incrementIfNew` 내부: Redis 중복 방지 없이 `posts.views`를 DB에서 직접 1 증가시킨다. MVP에서는 단순 기능 동작을 우선한다.
3. 응답 조립은 조회수/좋아요/댓글 수/투표 집계/reference를 모아 `PostDetailResponse`로.

## POST/PUT/DELETE /api/posts — 쓰기

- 작성: `PostCommandService` (`@Transactional`)가 principal의 `accountId` + nickname snapshot을 저장하고, `fileIds`가 있으면 업로드된 파일 metadata를 게시글에 연결한다. 공개 작성 API의 category는 항상 `GENERAL`로 고정된다.
- 수정: owner 또는 ADMIN(`isOwner` 검사는 `PostPolicy`). 파일 연결은 replace 방식.
- 삭제: 파일 연결 해제 → **게시글 row 물리 삭제**(댓글/태그 cascade) → `PostDeletedEvent`를 `DomainEventRecorder`로 기록. soft delete는 target policy다([delete-policy](../../policy/delete-policy.md)).

## POST/DELETE /api/posts/{postId}/like — 좋아요

1. `PostInteractionService` (`@Transactional`)가 게시글 존재를 확인한다.
2. `AbstractLikeService.likeTarget` — 존재 확인 후 insert하되, 동시 요청으로 unique constraint(`post_like(post_id, account_id)`)에 걸리면 `DataIntegrityViolationException`을 무시한다(멱등). 이후 count를 DB에서 재집계해 `posts.like_count`에 갱신한다.

원리: 좋아요는 사용자별 상태의 정합성이 중요하므로 DB를 source of truth로 유지한다. `like_count`는 like row에서 재계산 가능한 파생 데이터다.

## 댓글 (POST/GET/PUT/DELETE /api/posts/{postId}/comments)

- 작성: `parentId`가 있으면 같은 게시글의 부모 댓글인지 확인 후 1-depth 대댓글로 저장.
- 삭제: **row 물리 삭제**, 하위 대댓글은 `cascade = ALL, orphanRemoval = true`로 함께 삭제.
- 댓글 좋아요는 게시글 좋아요와 같은 `AbstractLikeService` 계층을 상속해 동일한 멱등 규칙을 따른다.

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
