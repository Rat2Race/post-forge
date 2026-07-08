# Catalog API 흐름

Endpoint 명세는 [Catalog API](../../api/catalog.md)가 canonical이다.

## 모듈 구조

```text
catalog
├── product   정규화 상품/카테고리/offer (ProductService)
└── matching  product embedding + 유사 상품 매칭 후보 (ProductMatchingService, ProductMatchCandidateService)
```

원리: 외부 source(Naver 등)마다 같은 상품이 다른 식별자/이름으로 들어오므로, `products`를 내부 표준 모델(truth)로 두고 source별 판매 항목은 `offers`로 분리한다. upsert 기준은 `(source, external_product_id)` unique다.

## 공개 조회 (GET /api/products, /search, /{productId}, /categories)

- `ProductService` (`@Transactional(readOnly = true)`) — `status = ACTIVE`인 상품만 목록/검색에 노출한다. 숨김(`HIDDEN`) 상품은 조회에서 제외.
- 검색은 `Product.normalizeName(query)`로 정규화한 이름 기준이며, categoryId가 함께 오면 카테고리 조건을 결합한다.
- 이 경로는 AI/외부 API를 호출하지 않는다. 저장된 데이터만 읽는다.

## POST /api/admin/products — 수동 upsert

`ProductService.upsertWithOffer` (`@Transactional`)의 분기 순서가 이 모듈의 핵심 로직이다.

1. 카테고리를 찾거나 생성한다(없으면 `기타`).
2. `(source, externalProductId)`로 **기존 offer**가 있으면: 연결된 product와 offer를 갱신하고 끝.
3. offer는 없지만 같은 키의 **기존 product**가 있으면: product 갱신 + offer upsert.
4. 둘 다 없으면 **matching 판단**으로 넘어간다:
   - `ProductMatchingService`가 상품명/브랜드 기반 embedding으로 기존 상품과 similarity를 계산한다.
   - 자동 매칭 기준을 넘으면 기존 상품에 병합(update), 아니면 새 상품을 생성한다.
   - 확신이 낮은 유사 후보는 `product_match_candidates`에 `PENDING`으로 남겨 사람이 검토하게 한다.
5. 상품 embedding 인덱싱(`product_embeddings`, pgvector HNSW cosine)은 실패해도 upsert 자체는 유지된다 — matching은 optional 경로다.

원리: 자동 병합의 오판(다른 상품을 합침)은 되돌리기 비싸므로, 애매한 구간은 자동 결정하지 않고 pending 후보로 밀어 admin 승인 흐름에 맡긴다.

## 매칭 후보 검토 (GET/PATCH /api/admin/product-match-candidates)

1. `ProductMatchCandidateService` — status 필터로 후보 목록을 페이징 조회한다. 후보에는 similarity score, brand/category 일치 여부가 함께 저장돼 있다.
2. approve/reject는 후보 상태를 `APPROVED`/`REJECTED`로 변경한다.

## PgVector 이중 사용 경계

catalog의 `product_embeddings`는 **상품 매칭용**이고, `ai` 모듈의 `vector_store`(RAG 문서)와 소유자/schema가 분리된다. 같은 PostgreSQL `vector` extension을 공유하지만 dimensions/index/입력 텍스트 결정권은 각자 갖는다. 근거: [DB Schema Ownership](../../database/schema-ownership.md)의 PgVector Decision.
