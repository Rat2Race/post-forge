# Price API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/products/{productId}/prices` | Public | `200` | path `productId`, query `from?`, `to?` | `List<PriceSnapshotResponse>` |
| `POST` | `/api/price-checks` | USER/ADMIN | `200` | `PriceCheckRequest` | `PriceCheckResponse` |

## Query

| Endpoint | Query |
| --- | --- |
| `GET /api/products/{productId}/prices` | `from?`, `to?` are ISO date-time values |

## Request DTO

| DTO | Fields |
| --- | --- |
| `PriceCheckRequest` | `keyword` required, `candidateUrl?`, `basePrice?` positive or zero, `shippingFee?` positive or zero, `discountAmount?` positive or zero, `finalPaidPrice?` positive or zero |

## Response DTO

| DTO | Fields |
| --- | --- |
| `PriceSnapshotResponse` | `id`, `productId`, `offerId`, `source`, `externalProductId`, `price`, `collectedAt` |
| `PriceCheckResponse` | `judgement`, `confidence`, `candidateEffectivePrice`, `shippingIncludedVerified`, `message`, `basis`, `items` |
| `PriceCheckBasisResponse` | `provider`, `sampleSize`, `medianPrice`, `lowThreshold`, `highThreshold` |
| `PriceCheckItemResponse` | `title`, `mallName`, `price`, `link` |

## 판정 정책

- `POST /api/price-checks`는 response-only API이며 `price_snapshots`, `posts`, `post_reference_links`를 만들지 않는다.
- `finalPaidPrice`가 있으면 사용자가 실제 결제하려는 가격으로 우선 사용하고, 없으면 `basePrice + shippingFee - discountAmount` 기준으로 candidate effective price를 계산한다.
- Naver Shopping 비교 샘플은 배송비 포함 가격을 우선 사용하되, 포함 여부를 확정하지 못하면 `shippingIncludedVerified=false`와 `배송비 포함 여부 미확인` 경고를 응답에 포함한다.
- 배송비 불확실성이 판정을 뒤집을 수 있으면 `LOW` confidence 또는 `INSUFFICIENT_INFO`를 반환한다.

## Enums

| Enum | Values |
| --- | --- |
| `PriceJudgement` | `CHEAP`, `NORMAL`, `EXPENSIVE`, `INSUFFICIENT_INFO` |
| `PriceCheckConfidence` | `HIGH`, `MEDIUM`, `LOW` |
