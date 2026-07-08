# Price API 흐름

Endpoint 명세는 [Price API](../../api/price.md)가 canonical이다.

## 모듈 구조

```text
price
├── tracking  가격 스냅샷 이력 (PriceSnapshotService)
└── check     response-only 가격 판정 (PriceCheckService)
```

## GET /api/products/{productId}/prices — 가격 이력

1. `PriceSnapshotService.getHistory` — `from`/`to`가 없으면 최신순 전체, 있으면 구간 오름차순으로 `price_snapshots`를 반환한다.
2. 이력은 상승/하락 이벤트로 해석하지 않는다. 그래프/변동성 계산은 프론트 책임이다.

스냅샷 기록은 사용자 API가 아니라 상품 수집 흐름에서 일어난다: `ingest`의 수집이 product/offer upsert 후 `recordSnapshot`을 호출하고, 이때 `PriceSnapshotCreatedEvent`를 `DomainEventRecorder` 계약으로 기록한다.

## POST /api/price-checks — response-only 가격 판정

1. `PriceCheckService.check` (`@Transactional(readOnly = true)`) — 입력 검증 후 candidate effective price를 계산한다. `finalPaidPrice`가 있으면 그것을 우선, 없으면 `basePrice + shippingFee - discountAmount`.
2. `source` 모듈의 `SourceRequestExecutor`로 Naver Shopping 샘플 최대 10건을 조회하고, 가격이 유효한 항목만 정렬한다. **이 API가 이 모듈에서 유일하게 사용자 요청으로 외부 API를 호출하는 지점이다.**
3. 샘플이 없으면 `INSUFFICIENT_INFO`.
4. 샘플 중앙값(median) 기준으로 판정 경계를 만든다: `low = median × 0.95`, `high = median × 1.05`. effective price가 low 미만이면 `CHEAP`, high 초과면 `EXPENSIVE`, 사이면 `NORMAL`.
5. **배송비 불확실성 안전장치**: effective price가 판정 경계에서 ±3,000원 이내면 배송비 유무에 따라 판정이 뒤집힐 수 있으므로 판정 대신 `INSUFFICIENT_INFO`를 반환한다.
6. 현재 구현은 샘플의 배송비 포함 여부를 검증하지 않으므로 모든 응답이 `shippingIncludedVerified=false` + `배송비 포함 여부 미확인` 경고 + confidence `LOW`다. `HIGH`/`MEDIUM`은 배송비 검증이 구현될 때 쓸 예약 값이다.

원리 두 가지:

- **response-only**: 이 API는 `price_snapshots`도 게시글도 만들지 않는다. 사용자 입력 기반의 일회성 판단이라 저장하면 오히려 오염된 가격 데이터가 쌓인다.
- **판정을 포기하는 조건을 명시**: 확신 없는 구간에서 `CHEAP`/`EXPENSIVE`를 말하는 것보다 `INSUFFICIENT_INFO`가 낫다는 판단. 판정의 신뢰 경계를 코드 상수(`SHIPPING_UNCERTAINTY_BUFFER = 3000`)로 드러낸다.
