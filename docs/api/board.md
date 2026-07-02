# Board API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/posts` | Optional JWT | `200` | query `keyword?`, `category?`, `boardCategory?`, `publishOrigin?`, pageable | `PageResponse<PostDetailResponse>` |
| `POST` | `/api/posts` | USER | `201` | `PostRequest` | `PostSummaryResponse` |
| `GET` | `/api/posts/{postId}` | Optional JWT | `200` | path `postId` | `PostDetailResponse` |
| `PUT` | `/api/posts/{postId}` | Owner/ADMIN | `200` | `PostRequest` | `PostSummaryResponse` |
| `DELETE` | `/api/posts/{postId}` | Owner/ADMIN | `200` | path `postId` | `MessageResponse` |
| `POST` | `/api/posts/{postId}/like` | USER | `200` | path `postId` | `LikeResponse` |
| `DELETE` | `/api/posts/{postId}/like` | USER | `200` | path `postId` | `LikeResponse` |
| `PUT` | `/api/posts/{postId}/purchase-vote` | USER | `200` | `PurchaseVoteRequest` | `PurchaseVoteResponse` |
| `DELETE` | `/api/posts/{postId}/purchase-vote` | USER | `200` | path `postId` | `PurchaseVoteResponse` |
| `GET` | `/api/posts/{postId}/comments` | Optional JWT | `200` | pageable | `PageResponse<CommentDetailResponse>` |
| `POST` | `/api/posts/{postId}/comments` | USER | `201` | `CommentRequest` | `CommentSummaryResponse` |
| `PUT` | `/api/posts/{postId}/comments/{commentId}` | Owner/ADMIN | `200` | `CommentRequest` | `CommentSummaryResponse` |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}` | Owner/ADMIN | `200` | path ids | `MessageResponse` |
| `POST` | `/api/posts/{postId}/comments/{commentId}/like` | USER | `200` | path ids | `LikeResponse` |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}/like` | USER | `200` | path ids | `LikeResponse` |
| `GET` | `/api/products/{productId}/posts` | Public | `200` | path `productId` | `List<PostDetailResponse>` |
| `GET` | `/api/user/profile` | USER/ADMIN | `200` | none | `ProfileResponse` |
| `PATCH` | `/api/user/profile/nickname` | USER/ADMIN | `200` | `ProfileNicknameUpdateRequest` | `MessageResponse` |
| `PATCH` | `/api/user/profile/password` | USER/ADMIN | `200` | `ProfilePasswordUpdateRequest` | `MessageResponse` |
| `GET` | `/api/files/presigned-url` | USER/ADMIN | `200` | query `fileName`, `contentType` | `FileUploadResponse` |
| `GET` | `/api/files/{fileId}/download-url` | USER/ADMIN | `200` | path `fileId` | `UrlResponse` |

`FileController` also maps the same file endpoints under `/api/files/s3`.

## Query defaults

| Endpoint | Defaults |
| --- | --- |
| `GET /api/posts` | `size=20`, `sort=createdAt,DESC` |
| `GET /api/posts/{postId}/comments` | `size=50`, `sort=createdAt,ASC` |

## Request DTO

| DTO | Fields |
| --- | --- |
| `PostRequest` | `title` required 2-100 no `<`/`>`, `content` required 10-10000, `tags` max 20 items and each max 50, `boardCategory`, `fileIds`. Public write APIs always store `category=GENERAL`. |
| `CommentRequest` | `parentId?`, `content` required 2-500, blocks dangerous HTML tags |
| `PurchaseVoteRequest` | `voteType` required: `BUYABLE`, `UNSURE`, `WAIT` |
| `ProfileNicknameUpdateRequest` | `nickname` required 2-20 Korean/English/digit/underscore |
| `ProfilePasswordUpdateRequest` | `currentPassword` required, `newPassword` required 8-100 |

## Response DTO

| DTO | Fields |
| --- | --- |
| `PostDetailResponse` | `id`, `title`, `content`, `summary`, `tags`, `category`, `boardCategory`, `publishOrigin`, `accountId`, `nickname`, `views`, `commentCount`, `likeCount`, `isLiked`, `purchaseVote`, `references`, `files`, `createdAt`, `modifiedAt` |
| `PostSummaryResponse` | `id`, `title`, `summary`, `tags`, `category`, `boardCategory`, `publishOrigin`, `accountId`, `nickname`, `createdAt`, `modifiedAt` |
| `PostReferenceLinkResponse` | `id`, `keyword`, `productId`, `provider`, `canonicalUrl`, `originalUrl`, `sourceName`, `publishedAt`, `titleSnapshot`, `publishOrigin` |
| `FileInfoResponse` | `fileId`, `originalFileName`, `fileType` |
| `CommentDetailResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `replyCount`, `likeCount`, `isLiked`, `createdAt`, `modifiedAt` |
| `CommentSummaryResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `createdAt`, `modifiedAt` |
| `LikeResponse` | `isLiked`, `likeCount` |
| `PurchaseVoteResponse` | `postId`, `eligible`, `buyableCount`, `unsureCount`, `waitCount`, `myVote` |
| `ProfileResponse` | `accountId`, `username`, `email`, `nickname`, `provider`, `isOAuthUser`, `roles`, `createdAt`, `updatedAt` |
| `FileUploadResponse` | `fileId`, `savedName`, `url` |

## Purchase Vote Policy

- 구매 판단 투표는 `PRODUCT_LAUNCH_NEWS` 및 `SYSTEM_BATCH` 게시글에만 허용한다.
- `BUYABLE`은 살만함, `UNSURE`는 애매함, `WAIT`는 기다림으로 표시한다.
- 투표 집계는 공개할 수 있지만 `myVote`는 인증된 회원에게만 계산한다.
- 일반 게시글, 사용자 수동 작성글, `ADMIN_BACKFILL` 출시 뉴스는 투표 쓰기를 거절한다.

## Enums

| Enum | Values |
| --- | --- |
| `PostCategory` | `GENERAL`, `AI_ANALYSIS`, `PRODUCT_LAUNCH_NEWS` |
| `PostBoardCategory` | `GENERAL`, `DIGITAL`, `APPLIANCE`, `LIVING`, `HEALTH`, `BEAUTY`, `SPORTS` |
| `PurchaseVoteType` | `BUYABLE`, `UNSURE`, `WAIT` |
| `PostReferenceProvider` | `NAVER_NEWS` |
| `PostPublishOrigin` | `USER`, `SYSTEM_BATCH`, `ADMIN_BACKFILL` |
