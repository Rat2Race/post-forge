# Catalog API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/products` | Public | `200` | pageable | `PageResponse<ProductResponse>` |
| `GET` | `/api/products/search` | Public | `200` | query `query`, `categoryId?`, pageable | `PageResponse<ProductResponse>` |
| `GET` | `/api/products/{productId}` | Public | `200` | path `productId` | `ProductResponse` |
| `GET` | `/api/products/categories` | Public | `200` | none | `List<ProductCategoryResponse>` |
| `GET` | `/api/products/categories/{categoryId}` | Public | `200` | path `categoryId`, pageable | `PageResponse<ProductResponse>` |
| `POST` | `/api/admin/products` | ADMIN | `201` | `ProductUpsertRequest` | `ProductResponse` |
| `PATCH` | `/api/admin/products/{productId}/hide` | ADMIN | `200` | path `productId` | `MessageResponse` |
| `GET` | `/api/admin/product-match-candidates` | ADMIN | `200` | query `status?`, pageable | `PageResponse<ProductMatchCandidateResponse>` |
| `PATCH` | `/api/admin/product-match-candidates/{candidateId}/approve` | ADMIN | `200` | path `candidateId` | `MessageResponse` |
| `PATCH` | `/api/admin/product-match-candidates/{candidateId}/reject` | ADMIN | `200` | path `candidateId` | `MessageResponse` |

## Query defaults

| Endpoint | Defaults |
| --- | --- |
| Product list/search/category list | `size=20`, `sort=createdAt,DESC` |
| Match candidate list | Spring pageable defaults unless request supplies `page`, `size`, `sort` |

## Request DTO

| DTO | Fields |
| --- | --- |
| `ProductUpsertRequest` | `source` required, `externalProductId` required, `name` required, `brand?`, `maker?`, `category1?`, `category2?`, `category3?`, `currentPrice` required positive or zero, `imageUrl?`, `productUrl?`, `mallName?` |

## Response DTO

| DTO | Fields |
| --- | --- |
| `ProductResponse` | `id`, `source`, `externalProductId`, `name`, `normalizedName`, `brand`, `maker`, `categoryId`, `categoryName`, `category1`, `category2`, `category3`, `currentPrice`, `imageUrl`, `productUrl`, `mallName`, `status`, `createdAt`, `updatedAt` |
| `ProductCategoryResponse` | `id`, `name`, `parentId`, `depth` |
| `ProductMatchCandidateResponse` | `id`, `sourceProductId`, `source`, `externalProductId`, `sourceProductTitle`, `candidateProductId`, `similarityScore`, `brandMatched`, `categoryMatched`, `status`, `createdAt` |

## Enums

| Enum | Values |
| --- | --- |
| `ProductStatus` | `ACTIVE`, `HIDDEN` |
| `ProductMatchStatus` | `PENDING`, `APPROVED`, `REJECTED` |
