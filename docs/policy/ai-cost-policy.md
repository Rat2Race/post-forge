# AI Cost Policy

PostForge의 AI 정책은 기능보다 먼저 비용 통제를 기준으로 한다.
AI는 서비스의 모든 요청에 붙는 기본 동작이 아니라, 명시적으로 실행되고 저장되는 작업이다.

## Cost Invariants

- 게시글 목록 조회는 AI를 호출하지 않는다.
- 게시글 상세 조회는 AI를 호출하지 않는다.
- 상세 페이지의 관련 트렌드는 `post_trend_links`, `trend_clusters` 같은 저장된 read model에서 조회한다.
- 글쓰기 폼의 기본 추천은 DB 검색과 trend read model을 사용한다.
- AI 작성 보조는 사용자가 명시적으로 실행할 때만 호출한다.
- 모든 AI 호출은 `ai_usage_logs`에 기록한다.
- account 또는 system budget을 초과한 AI 호출은 실행 전에 거절한다.

## External API To Posting

외부 API로 들어온 item을 모두 AI 게시글로 만들지 않는다.

허용 흐름:

```text
collector request
-> collected_items
-> trend_clusters
-> eligibility check
-> AI_BRIEF_GENERATION
-> stored posts(post_type = AI_BRIEF)
```

AI brief 후보 조건 예:

- 일정 시간 내 여러 출처에서 반복된 이슈
- source_count 또는 trend_score가 기준 이상
- 중복/저품질 item이 아닌 경우
- source quota와 AI budget이 남아 있는 경우
- 관리자 승인 또는 안전한 batch rule을 통과한 경우

금지 흐름:

```text
every collected item -> AI call -> public post
```

## Writing Assist

글쓰기 보조는 두 단계로 나눈다.

### Basic Assist

비용이 거의 없는 기본 보조다.

- 제목/본문 키워드 기반 trend 검색
- 관련 collected item 조회
- 출처 링크 추천
- 카테고리/태그 추천

이 단계는 AI를 호출하지 않는다.

### AI Assist

유료 또는 quota 기반 보조다.

- 리포트 개요 생성
- 수집 자료 요약
- 문장 개선
- 반론/주의점 제안
- AI brief 초안 생성

실행 조건:

- 인증된 account가 있어야 한다.
- plan 또는 trial quota가 남아 있어야 한다.
- `ai_budget_windows`가 남아 있어야 한다.
- operation type이 허용 목록에 있어야 한다.
- 실행 결과와 실패 모두 `ai_usage_logs`에 남겨야 한다.

## Related Trends On Detail

게시글 상세의 관련 트렌드는 저장된 relation을 읽는다.

허용:

- 발행 시점에 `post_trend_links` 계산
- 수정 시점에 `post_trend_links` 재계산
- 배치 작업으로 trend relation 갱신
- 단순 DB query로 같은 category/tag/keyword trend 조회

금지:

- 상세 조회마다 AI로 관련 트렌드 계산
- 댓글/좋아요/조회수 이벤트마다 AI 재분석
- 비회원 조회에서 AI 호출

## Usage Log Requirements

`ai_usage_logs`는 최소한 다음 정보를 가져야 한다.

| Field | Purpose |
| --- | --- |
| `account_id` | 비용을 발생시킨 사용자. system 작업이면 null 가능 |
| `workspace_id` | private workspace context |
| `draft_id` | draft assist context |
| `post_id` | AI brief 또는 published content context |
| `operation_type` | 비용 목적 분류 |
| `model` | 사용 모델 |
| `input_tokens` | 입력 token |
| `output_tokens` | 출력 token |
| `estimated_cost_usd` | 비용 추정 |
| `status` | success/failure/quota rejected |
| `failure_reason` | 실패 분석 |

## Plan Boundary

플랜 차이는 공개 게시판의 신뢰 정보가 아니라 AI와 private workspace 생산성에 둔다.

무료 플랜:

- 공개 게시판 읽기/쓰기
- 제한된 private draft
- DB 기반 trend/source 추천
- AI assist 없음 또는 매우 제한된 trial

유료 플랜:

- 더 많은 private draft/report
- 더 많은 trend bundle
- AI assist monthly quota
- export 같은 고급 기능

## Operational Alerts

다음 조건은 운영 경고 대상이다.

- 특정 account의 AI 실패율 급증
- system AI brief 비용 급증
- source별 API quota 소진
- collector circuit open
- AI budget window 80% 이상 소진
- 조회 API에서 AI usage log가 생성되는 이상 징후
