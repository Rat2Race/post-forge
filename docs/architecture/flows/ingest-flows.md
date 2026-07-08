# Ingest API 흐름

Endpoint 명세는 [Ingest API](../../api/ingest.md)가 canonical이다.

## 모듈 구조

```text
ingest
├── pipeline  문서 chunking + vector store 적재 (IngestPipelineService)
├── product   상품 수집 orchestration (CollectProductsUseCase, TrackedKeywordService, 스케줄러)
└── news      뉴스 문서 수집 + 출시 뉴스 자동 게시 (LaunchNewsAutoPostService, GatePolicy, 스케줄러)
```

경계 원리: `ingest`는 orchestration만 소유한다. 외부 API client는 `source`, 상품 truth는 `catalog`, 가격 이력은 `price`, 게시글 쓰기는 `core`의 `PostWriter` port(구현은 `board`), 벡터 저장은 Spring AI `VectorStore` API(bean은 `ai`가 생성)로 위임한다. `ingest`는 `ai`/`board` 구현 모듈을 직접 의존하지 않는다.

## POST /api/ingest/documents — 문서 적재

1. `IngestPipelineService.store` (`@Transactional`) — `DocumentChunker`가 문서를 chunk로 나눈다.
2. `DocumentChunkStore.store` — 가능하면 embedding을 만들어 `vector_store`에 저장하고, vector store가 불가하면 **임베딩 없이 접수**하며 `degradationReason`을 응답에 담는다.

원리: 적재 API는 embedding 실패를 5xx로 만들지 않고 degraded 결과로 명시한다. 호출자가 재적재 여부를 판단할 수 있다.

## 상품 수집 (POST /api/admin/collection-jobs/manual + 스케줄)

1. `CollectProductsUseCase.collect` — `collection_jobs`에 `REQUESTED` row를 만들고 `RUNNING`으로 전이한다.
2. `source`의 `SourceRequestExecutor`로 외부 상품 검색(`MOCK` 또는 `NAVER`). 호출 시간은 `external_source_fetch` metric으로 계측.
3. 각 item마다: `raw_products`에 원본 payload 저장 → `catalog`의 `ProductService.upsertWithOffer`(정규화/matching, [catalog-flows](./catalog-flows.md)) → `price`의 `recordSnapshot`.
4. 성공 시 `SUCCESS` + 수집 count, 실패 시 예외를 삼키고 `FAILED` + failure reason을 job에 기록한다. 성공/실패 counter와 duration timer를 남긴다.

스케줄 실행: `ProductCollectionScheduler`는 `ingest.product.scheduler.enabled=true`일 때만 활성화되고(`@ConditionalOnProperty`), cron 기본값은 매시 정각이다. 활성 `tracked_keywords`를 순회한다.

원리: 실패를 job row에 남기는 이유는 외부 API 장애가 반복될 때 "언제부터, 어떤 keyword가, 왜" 실패했는지 DB만으로 추적하기 위해서다. raw payload를 보관하므로 정규화 로직이 바뀌어도 재처리할 수 있다.

## 뉴스 문서 수집 (POST /api/admin/news-documents/manual)

1. keyword + topics를 조합해 검색 query들을 만든다.
2. `source`의 Naver News adapter로 검색하고 link 기준으로 중복 제거한다.
3. 뉴스 내용을 `DocumentIngestCommand`로 변환해 위의 문서 적재 파이프라인으로 보낸다(RAG 검색용).

## 출시 뉴스 자동 게시 (POST /api/admin/launch-news/manual + 스케줄)

`LaunchNewsAutoPostService.postLaunchNews` (`@Transactional`)의 후보별 gate 순서:

1. **후보 수집**: keyword × topics(기본: 신제품/출시/공개/사전예약) query로 Naver News 검색, canonical URL로 정규화(scheme/host 소문자화, query/fragment 제거).
2. **중복 gate**: 같은 실행 내 중복 + `post_reference_links.canonical_url` 존재 여부 → `DUPLICATE_ARTICLE`.
3. **콘텐츠 gate**: `LaunchNewsGatePolicy`가 광고성 문구(`ADVERTISING`), 출처 불명(`UNKNOWN_SOURCE`), 필수 출시 키워드 미포함(`MISSING_LAUNCH_KEYWORD`)을 거른다. 순수 함수 정책이라 단위 테스트로 검증된다.
4. **daily cap gate**: keyword+productId+발행일 기준 게시 수가 `dailyCap`을 넘으면 `DAILY_CAP_EXCEEDED`. DB count는 key당 1회만 조회하고 이후는 메모리에서 증가시킨다.
5. **AI 초안**: `core`의 `LaunchNewsPostDraftGenerator` port로 요약/초안 생성. 실패(`Optional.empty`)면 `AI_GENERATION_FAILED` skip — AI 실패가 배치 전체를 중단시키지 않는다.
6. **게시**: `PostWriter` port로 `PRODUCT_LAUNCH_NEWS` 게시글 작성(작성자는 system 계정 `accountId=0`, nickname `PostForge News Bot`). board category는 keyword/제목/초안 텍스트의 규칙 매칭으로 결정(DIGITAL/APPLIANCE/... 기본 GENERAL). 이어서 `post_reference_links`에 출처 evidence를 저장한다.
7. 응답은 생성된 postId 목록 + skip 목록(url, reason).

스케줄 실행: `LaunchNewsAutoPostScheduler`는 `ingest.news.launch.scheduler.enabled=true`일 때만 활성화되고 `SYSTEM_BATCH` origin으로 실행한다. manual endpoint는 admin 전용이다.

원리: "수집된 모든 item → AI → 공개 게시"를 금지하고, deterministic gate를 **AI 호출 앞에** 배치해 LLM 비용을 통과 후보에만 쓴다. 게시된 글에는 반드시 출처 evidence(reference link)가 남아 daily cap과 중복 판정의 기준이 된다. 정책: [AI Cost Policy](../../policy/ai-cost-policy.md)

알려진 한계: 뉴스 검색과 LLM 초안 생성이 `@Transactional` 안에서 실행되므로 외부 API가 느리면 DB 커넥션 점유 시간이 길어지고, 배치 전체가 한 트랜잭션이다. 후보별 트랜잭션 분리가 후속 과제다.
