# AI Routing Backlog

이 문서는 현재 ERD에 바로 반영하지 않는 향후 방향을 백로그로 보관한다.
우선순위는 작은 시스템을 먼저 완성하고, AI/벡터/비동기 자동화는 단계적으로 붙이는 것이다.

## Current Priority

지금은 "상품 수집 -> 상품/offer 저장 -> 가격 이력 저장 -> 상품 게시글 연결 -> 커뮤니티 조회" 흐름을 작고 안정적으로 완성한다.

우선 집중할 범위:

| Area | Keep focused on |
| --- | --- |
| Product collection | `source_policies`, `collection_jobs`로 외부 API 제어와 수집 성공/실패 추적 |
| Catalog | `products`, `offers`로 내부 상품과 판매처별 상품 저장 |
| Price | `price_snapshots`로 가격 이력 저장과 단순 가격 하락 판단 |
| Board | `posts`, `comments`, `post_like`, `comment_like`, `post_product_links`로 커뮤니티와 상품 글 연결 |
| Runtime reads | 사용자 API는 DB/Redis 조회 중심으로 유지 |

초기에는 AI가 없어도 핵심 기능이 동작해야 한다. AI는 데이터 파이프라인이 안정화된 뒤 자동화 품질을 높이는 계층으로 붙인다.

## Routing Principle

AI 기능은 "OpenAI vs Qwen"으로 나누지 않는다. 작업 난이도와 비용에 따라 아래 순서로 라우팅한다.

```text
1. Rule / SQL / Redis
2. Embedding / Vector Search
3. Local LLM
4. OpenAI
```

원칙:

- LLM은 최대한 늦게 사용한다.
- 추천/트렌드 랭킹은 LLM을 사용하지 않는다.
- Local LLM도 공짜 자원처럼 남용하지 않고, 룰/임베딩으로 애매한 경우에만 사용한다.
- OpenAI는 사용자에게 보이는 고품질 생성물에만 사용한다.
- 사용자 API는 AI를 직접 호출하지 않고, 미리 저장된 결과를 조회한다.

## Future Pipeline

```text
external product api
        |
product-ingest
        |
rule-based normalizer
        |
embedding matcher
        |
local-llm enrichment
        |
trend-score batch
        |
openai post generator
        |
post draft / review
        |
publish to board
```

## Backlog Items

| Priority | Item | Purpose | Suggested tables |
| --- | --- | --- | --- |
| P1 | Product normalization hardening | 상품명, 브랜드, 가격, 카테고리를 rule 기반으로 안정화 | existing `products`, `offers` |
| P1 | Simple price-drop detection | `price_snapshots`에서 최근 가격과 이전 가격을 비교 | existing `price_snapshots` |
| P1 | Product post bridge | 가격 하락 또는 상품 소개 게시글을 상품과 연결 | existing `post_product_links` |
| P2 | Raw payload replay | 외부 API 원본 재처리/장애 분석 지원 | `raw_product_payloads` or current `raw_products` |
| P2 | Current lowest-price read model | 최저가/하락률 조회가 잦아질 때 계산 비용 절감 | `current_lowest_prices` or current `lowest_price_snapshots` |
| P2 | API request observability | 외부 API 요청 단위 장애/쿼터/서킷 분석 | `api_request_logs` or current `external_api_request_logs` |
| P3 | Product vector matching | source가 늘어났을 때 같은 상품 후보 탐지 | `product_vectors`, `product_match_reviews` |
| P3 | Product AI metadata | 키워드, 태그, 요약, 카테고리 confidence 저장 | `product_ai_metadata` |
| P3 | AI task router | rule/embedding/local LLM/OpenAI 실행 상태와 fallback 기록 | `ai_task_runs` |
| P3 | AI usage/cost log | 모델, token, latency, 비용, 실패 원인 추적 | `ai_usage_logs` |
| P4 | OpenAI post draft workflow | 사용자 노출용 글을 비동기로 생성하고 검수/발행 | `post_drafts` or current `auto_post_drafts` |
| P4 | Batch generation | 즉시 응답이 필요 없는 자동 포스팅 후보를 묶어서 생성 | `ai_batches` |
| P4 | Prompt/version management | 프롬프트 변경 이력과 생성 품질 회귀 추적 | `prompt_versions` |

## Task Routing Matrix

| Task | First path | Fallback | Final state |
| --- | --- | --- | --- |
| Product name normalization | Regex/rules | Local LLM only if ambiguous | normalized fields saved |
| Category classification | Dictionary/rule match | Embedding, then local LLM | category + confidence saved |
| Keyword extraction | Token/rule extraction | Local LLM | keywords saved, or empty with failed reason |
| Duplicate product detection | Embedding similarity | Local LLM verification | match review candidate |
| Trend score | SQL batch/Redis sorted set | No LLM | score/ranking saved |
| Recommendation candidates | DB/Redis/vector query | No LLM | Top N candidates |
| Auto post generation | Template draft | OpenAI structured output | post draft pending review |
| Post quality improvement | OpenAI | Retry/admin review | publishable draft |

## Proposed AI Router Shape

```text
ai-router
  - RuleProcessor
  - EmbeddingProcessor
  - LocalLlmClient
  - OpenAiClient
  - FallbackHandler
  - CostPolicy
```

Suggested task policy examples:

```text
KEYWORD_EXTRACTION
  -> rule first
  -> local LLM fallback
  -> empty keyword + FAILED state on failure

CATEGORY_CLASSIFICATION
  -> category dictionary first
  -> embedding similarity
  -> local LLM fallback
  -> REVIEW_REQUIRED when confidence is low

AUTO_POST_GENERATION
  -> OpenAI structured output
  -> template fallback
  -> post draft pending review

RECOMMENDATION
  -> no LLM
  -> Redis + SQL score + vector similarity
```

## Interview Framing

면접에서는 "OpenAI와 Qwen을 붙였다"보다 아래처럼 설명한다.

> AI 작업을 비용과 품질 기준으로 라우팅했습니다. 추천/트렌드 랭킹은 LLM을 사용하지 않고 이벤트 집계와 Redis 기반 점수로 처리하고, 상품 메타데이터 생성은 rule-based와 embedding을 우선 적용했습니다. 애매한 분류나 키워드 추출만 로컬 LLM으로 처리하며, 사용자에게 노출되는 자동 포스팅은 OpenAI를 비동기 워커로 분리해 구조화된 결과로 생성하는 방향을 백로그로 잡았습니다.

## Deferred ERD Changes

아래 테이블은 현재 핵심 ERD에 바로 추가하지 않는다. 작은 시스템이 안정화되고 실제 운영 문제가 생긴 뒤 추가한다.

| Deferred table | Reason to defer |
| --- | --- |
| `product_ai_metadata` | 메타데이터 자동 생성이 핵심 기능이 된 뒤 추가 |
| `ai_task_runs` | AI 라우팅/fallback 추적이 필요해진 뒤 추가 |
| `ai_usage_logs` | 비용/latency 관리가 실제 운영 이슈가 된 뒤 추가 |
| `ai_batches` | OpenAI Batch 같은 비동기 대량 생성이 필요해진 뒤 추가 |
| `prompt_versions` | 프롬프트 변경 이력과 품질 회귀를 관리할 때 추가 |
