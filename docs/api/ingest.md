# Ingest API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/ingest/documents` | USER/ADMIN | `200` | `List<DocumentRequest>` | `DocumentResponse` |
| `POST` | `/api/admin/tracked-keywords` | ADMIN | `201` | `TrackedKeywordRequest` | `TrackedKeywordResponse` |
| `GET` | `/api/admin/tracked-keywords` | ADMIN | `200` | none | `List<TrackedKeywordResponse>` |
| `PATCH` | `/api/admin/tracked-keywords/{id}/disable` | ADMIN | `200` | path `id` | `MessageResponse` |
| `POST` | `/api/admin/collection-jobs/manual` | ADMIN | `202` | `CollectionJobRunRequest` | `CollectionJobResponse` |
| `GET` | `/api/admin/collection-jobs` | ADMIN | `200` | pageable | `PageResponse<CollectionJobResponse>` |
| `POST` | `/api/admin/news-documents/manual` | ADMIN | `202` | `ProductNewsDocumentCollectRequest` | `ProductNewsDocumentCollectResponse` |
| `POST` | `/api/admin/launch-news/manual` | ADMIN | `202` | `LaunchNewsManualPostRequest` | `LaunchNewsAutoPostResponse` |

## Query defaults

| Endpoint | Defaults |
| --- | --- |
| `GET /api/admin/collection-jobs` | `size=20`, `sort=requestedAt,DESC` |

## Request DTO

| DTO | Fields |
| --- | --- |
| `DocumentRequest` | `content` required, `source?`, `metadata?` as `Map<String, String>` |
| `TrackedKeywordRequest` | `source?`, `keyword` required, `intervalMinutes?`, `displayCount?` |
| `CollectionJobRunRequest` | `source?` defaults to `MOCK`, `keyword` required, `displayCount?` defaults to `20` |
| `ProductNewsDocumentCollectRequest` | `productId?`, `keyword` required, `displayCount?`, `topics?` max 10 items and each max 30 |
| `LaunchNewsManualPostRequest` | `keyword` required, `productId?`, `displayCount?` 1-20, `dailyCap?` 1-20, `topics?` max 10 items and each max 30 |

## Response DTO

| DTO | Fields |
| --- | --- |
| `DocumentResponse` | `count`, `chunkCount`, `embeddingsStored`, `degradationReason`, `message` |
| `TrackedKeywordResponse` | `id`, `source`, `keyword`, `intervalMinutes`, `displayCount`, `enabled`, `createdAt`, `updatedAt` |
| `CollectionJobResponse` | `id`, `trackedKeywordId`, `source`, `keyword`, `status`, `requestedAt`, `startedAt`, `finishedAt`, `collectedCount`, `failureReason`, `retryCount` |
| `ProductNewsDocumentCollectResponse` | `keyword`, `productId`, `queries`, `newsCount`, `chunkCount`, `embeddingsStored`, `degradationReason`, `message` |
| `LaunchNewsAutoPostResponse` | `keyword`, `acceptedCount`, `skippedCount`, `createdPostIds`, `skips` |
| `LaunchNewsAutoPostResponse.SkipResponse` | `url`, `reason` |

## Launch-News Posting Policy

- 출시 뉴스 자동 게시 대상은 새로운 상품 출시/예약판매/공식 발표/공식 가격 공개 같은 뉴스로 제한한다.
- skip reason은 `DUPLICATE_ARTICLE`, `ADVERTISING`, `UNKNOWN_SOURCE`, `MISSING_LAUNCH_KEYWORD`, `AI_GENERATION_FAILED`, `DAILY_CAP_EXCEEDED`를 사용한다.
- manual endpoint와 scheduler 모두 필터를 통과한 후보만 `PRODUCT_LAUNCH_NEWS` 게시글로 만든다.
- 공개 read path에서는 AI를 호출하지 않는다.

## Enums

| Enum | Values |
| --- | --- |
| `SourceType` | `MOCK`, `NAVER` |
| `CollectionJobStatus` | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `RETRYING`, `SKIPPED_BY_RATE_LIMIT`, `SKIPPED_BY_CIRCUIT_OPEN` |
| `LaunchNewsSkipReason` | `DUPLICATE_ARTICLE`, `ADVERTISING`, `UNKNOWN_SOURCE`, `MISSING_LAUNCH_KEYWORD`, `AI_GENERATION_FAILED`, `DAILY_CAP_EXCEEDED` |
