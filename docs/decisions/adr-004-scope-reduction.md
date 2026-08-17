# ADR-004 스코프 축소: 출시 뉴스 커뮤니티로 최소화

상태: **Proposed**

PostForge의 제품 정의를 "신상품 출시 뉴스를 자동으로 모아 게시하고, 커뮤니티가 구매 판단을 내리는 서비스"로 좁힌다.
이 문장에 기여하지 않는 축은 동결이 아니라 삭제한다.

## 배경

- 가격 축의 유일한 데이터 소스였던 Naver Shopping API가 종료되어, price check는 항상 503을 반환하는 도달 불가 코드로 배포되고 있다.
- 상품 수집 축도 같은 이유로 이미 비기능이다. `ProductSourceClient` 구현은 둘뿐이고, NAVER는 항상 `UnsupportedOperationException`을 던지며(은퇴), MOCK은 prod에서 `MOCK_PRODUCT_SOURCE_ENABLED:false`로 꺼져 있다. 프로덕션에 살아있는 상품 소스가 0개다.
- 최저가 도출의 실제 난제는 알고리즘이 아니라 (1) 소스 간 상품 동일성 판별, (2) 지속 가능한 가격 데이터 확보다. 둘 다 코드가 아니라 데이터 확보의 문제이며, 1인 프로젝트가 엔지니어링으로 극복할 수 없다.
- 구매 판단 투표(`BUYABLE`/`UNSURE`/`WAIT`)가 가격 알고리즘이 제공하려던 가치("이 가격에 사도 되는가")를 커뮤니티 판단으로 이미 대체하고 있다.

## 결정

- **삭제한다 (동결 아님)** — 근거는 셋으로 나뉜다.
  1. *목표와 다른 제품축*: price.check(알고리즘이 구매를 판정 — 목표는 커뮤니티가 판정한다).
  2. *목표에 기여할 수 있으나 재료가 없음*: price.tracking, 상품 수집 축(source.product, ingest.product, catalog 전체 포함 matching). 프로덕션 소스가 0개라 되살릴 대상이 없다.
  3. *목표와 무관하게 실행되지 않는 코드*: messaging(outbox 소비자 없음), board의 `PostProductLink`/`ProductPostController`(링크 생성 주체가 없어 항상 빈 배열), ai 고아 코드(news-analysis 프롬프트, `AiErrorCode`), 생존 모듈의 1:1 통과 계층.
- **유지한다**: RAG 채팅(`ai.chat`, `ai.search`, `ingest.document`). `IngestProductNewsUseCase`가 뉴스 수집 시 pgvector에 자동 적재하므로 "출시 뉴스에 대해 물어보고 구매 판단을 돕는" 목표에 직접 기여하는 살아있는 파이프라인이다.
- 복구는 git 히스토리와 `pre-scope-cut` 태그가 담당한다. 코드 보존을 위한 동결·feature flag를 두지 않는다.
- Flyway V0000 baseline은 삭제 이후 스키마로 스쿼시한다. 마이그레이션 파일이 미커밋 상태인 지금만 가능한 작업이다.
- 생존 모듈 내부의 1:1 통과 계층(Store 인터페이스 + PersistenceAdapter 사슬)도 같은 기준으로 삭제한다.

## 왜 동결이 아니라 삭제인가

- 개발 흐름이 "사람 = 설계 결정·검증, AI = 구현"으로 이동했다. 코드베이스는 AI의 컨텍스트이므로 죽은 코드는 모든 미래 작업의 토큰 비용에 곱해지고, AI는 주변 코드의 관례를 따르므로 죽은 패턴이 새 구현의 템플릿으로 오염된다.
- 동결의 유일한 장점(빠른 복구)은 git 태그가 무료로 제공한다. 동결은 유지 비용만 남는다.

## 영향

- ADR-003 후속 변경에 기록된 messaging/outbox 경계는 이 결정으로 제거된다. 서비스 분리 시점의 event/outbox 설계는 그 시점에 다시 한다.
- 관련 문서(module-dependencies.md, README, API 명세)와 `ModuleBoundaryTest` 허용 그래프는 삭제와 같은 커밋에서 갱신한다. 문서가 코드보다 오래 살아남으면 안 된다.
- 삭제 대상 테이블(outbox_events, price_snapshots 등)에 보존할 실데이터가 없음을 확인한 뒤 진행한다.

## 아직 해결하지 않는 것

- 가격 축의 부활 조건: 지속 가능한 상품·가격 데이터 소스가 확보되는 시점에 별도 ADR로 재설계한다. 이 결정은 "가격 기능이 나쁘다"가 아니라 "지금은 소스가 없다"는 기록이다.
