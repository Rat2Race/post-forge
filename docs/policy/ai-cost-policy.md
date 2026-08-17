# AI Cost Policy

> Current: AI chat, product embedding foundation, internal launch-news AI draft port, public read path AI 호출 금지, deterministic pre-gate, shared `TextGenerationClient` metric/log.
> Target: `ai_usage_logs`, `ai_budget_windows`, billing/plan quota enforcement.

PostForge의 AI 정책은 기능보다 비용 통제를 우선한다. AI는 모든 요청의 기본 동작이 아니라 명시적으로 실행되는 작업이다.

## Current Invariants

- 게시글 목록·상세·댓글과 상품 조회는 AI를 호출하지 않는다.
- 상세 페이지의 관련 트렌드는 저장된 read model에서 조회한다.
- 사용자 AI 기능은 인증된 사용자가 채팅을 명시적으로 요청할 때만 실행한다.
- 출시 뉴스 AI draft는 [통합 API 명세의 Ingest](../api/README.md#ingest) gate를 통과한 batch/admin/system write flow에서만 실행한다.
- 수집된 모든 item을 AI 호출이나 공개 게시글로 연결하지 않는다.
- 현재 호출은 shared `TextGenerationClient`의 metric/log 대상이다.

## Current AI Boundary

AI를 사용하지 않는 검색과 모델 생성 요청을 구분한다.

- DB/vector 검색, 관련 자료 조회, 출처 링크 추천은 AI 호출이 아니다.
- 채팅 답변과 내부 출시 뉴스 draft 생성은 AI 호출이다.
- 출시 뉴스 생성 결과는 별도의 admin/system gate와 write flow를 통과해야 공개 게시글이 된다.

## Target: Budget Enforcement

모든 AI 호출은 실행 전에 다음 조건을 확인한다.

- 인증된 account 또는 승인된 system operation이 있어야 한다.
- plan/trial quota와 `ai_budget_windows` 잔여량이 있어야 한다.
- operation type이 허용 목록에 있어야 한다.
- 성공, 실패, quota 거절을 모두 `ai_usage_logs`에 기록해야 한다.

## Target: Usage Log

| Field | Purpose |
| --- | --- |
| `account_id` | 비용을 발생시킨 사용자. system 작업이면 null 가능 |
| `workspace_id` | private workspace context |
| `post_id` | published content context |
| `operation_type` | 비용 목적 분류 |
| `model` | 사용 모델 |
| `input_tokens` | 입력 token |
| `output_tokens` | 출력 token |
| `estimated_cost_usd` | 비용 추정 |
| `status` | success/failure/quota rejected |
| `failure_reason` | 실패 분석 |

## Target: Plan Boundary

무료/유료 plan의 공개 게시판 권한은 같고 AI·private workspace 생산성 한도만 다르다. 세부 권한은 [Access Policy](./access-policy.md)를 따른다.

## Operational Alerts

- 특정 account의 AI 실패율 급증
- system launch-news draft 비용 급증
- source별 API quota 소진
- AI budget window 80% 이상 소진
- 조회 API에서 AI usage가 발생하는 이상 징후
