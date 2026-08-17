# Price API 흐름

Endpoint 명세는 [통합 API 명세의 Price](../../api/README.md#price)가 canonical이다.

## GET /api/products/{productId}/prices — 가격 이력

1. `PriceSnapshotService.getHistory` — `from`/`to`가 없으면 최신순 전체, 있으면 구간 오름차순으로 `price_snapshots`를 반환한다.
2. 이력은 상승/하락 이벤트로 해석하지 않는다. 그래프/변동성 계산은 프론트 책임이다.

스냅샷 기록은 사용자 API가 아니라 상품 수집 흐름에서 일어난다: `ingest`의 수집이 product/offer upsert 후 `recordSnapshot`을 호출하고, 이때 `PriceSnapshotCreatedEvent`를 `DomainEventRecorder` 계약으로 outbox에 기록한다([messaging-flows](./messaging-flows.md)).

## POST /api/price-checks — response-only 가격 판정

1. `PriceCheckService.check`가 입력을 검증하고 [Price 판정 정책](../../api/README.md#price-judgement-policy)에 따라 후보 가격을 계산한다.
2. `source` 모듈의 `SourceRequestExecutor`로 상품 샘플을 조회한다.
3. 현재 Naver Shopping 검색 API 종료 대응으로 `503 EXTERNAL_SERVICE_UNAVAILABLE`을 반환한다.
4. 대체 소스가 연결되면 기존 계산으로 샘플과 신뢰 경계를 평가한다.
5. 판정 과정은 `price_snapshots`나 게시글을 만들지 않는다.
