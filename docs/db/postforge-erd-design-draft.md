# PostForge ERD 설계 초안

이 문서는 2026-05-18 기준으로 다시 정리한 PostForge의 제품 경계와 target schema 초안이다.
구현 migration의 직접 입력이 아니라, 구현 전에 제품 의미와 비용 구조를 먼저 고정하기 위한 설계 문서다.

시각화용 DBML은 [postforge-dbdiagram-draft.dbml](./postforge-dbdiagram-draft.dbml)을 기준으로 한다.

## 제품 정의

PostForge는 공개 커뮤니티 게시판과 개인 리포트 작업공간을 함께 가진다.
외부 API로 수집한 트렌드 데이터를 사용자가 공개 글 또는 비공개 리포트로 재가공하도록 돕는 서비스다.

핵심 흐름은 다음과 같다.

```text
external source
-> collector policy / API request log
-> collected item
-> trend cluster/card
-> draft/report/post
-> evidence links
-> public board or private workspace
```

## 이번 재설계의 핵심 판단

### 공개 게시판의 신뢰도를 플랜으로 나누지 않는다

PostForge가 공개 게시판을 유지한다면, 무료/유료 플랜에 따라 읽을 수 있는 근거나 신뢰 신호가 달라지면 안 된다.
그렇게 하면 "신뢰도 있는 게시판"이라는 theme 자체가 무너진다.

따라서 플랜 차이는 공개 게시판의 신뢰 정보가 아니라 다음 영역에 둔다.

- 개인 작업공간 용량
- 프라이빗 리포트 개수
- AI 작성 보조 쿼터
- export 또는 고급 정리 기능
- 저장 가능한 trend bundle 수

### AI는 조회 시점에 호출하지 않는다

가장 위험한 비용 구조는 다음 세 가지다.

| 문제 | 위험 | 원칙 |
| --- | --- | --- |
| 외부 API 수집 데이터를 AI가 전부 포스팅 | 수집량이 늘수록 AI 비용 폭증 | 조건을 만족한 trend cluster만 저장형 AI brief 후보가 된다 |
| 글쓰기 폼 옆 보조 패널이 항상 AI 호출 | 사용자가 글을 쓸 때마다 비용 발생 | 기본 추천은 DB 검색, AI 보조는 명시적 요청과 quota 기반 |
| 게시글 상세의 관련 트렌드가 조회마다 AI 호출 | 트래픽 증가가 곧 AI 비용 증가 | 관련 트렌드는 precomputed link 또는 DB query로 조회 |

## 사용자 영역

### Public Board

공개 게시판은 기존 프론트와 커뮤니티 기능을 살린다.
일반 사용자 토론글, 공개 리포트, 저장형 AI brief가 같은 board feed에 노출될 수 있다.

단, 공개 게시판은 "AI가 모든 글을 검증한 곳"으로 정의하지 않는다.
대신 글에 연결된 출처와 관련 트렌드를 볼 수 있는 공개 피드로 정의한다.

### Private Workspace

개인 작업공간은 수익 모델의 중심이다.
사용자는 수집된 trend, source, link를 저장하고 비공개 draft/report를 작성한다.

Private workspace에서만 유료 차별이 자연스럽다.

- 더 많은 draft/report 저장
- AI 요약/개요/반론 생성
- report export
- trend bundle 저장
- 월간 AI 사용량 쿼터

### Admin / System

운영자 또는 시스템은 collector source, quota, circuit state, AI brief 후보 생성 규칙을 관리한다.
관리자가 개인 workspace 내용을 기본적으로 볼 수 있어서는 안 된다.
운영 감사가 필요하면 별도 break-glass 정책이 필요하며 MVP 범위 밖이다.

## 글 유형

`posts` 또는 future content table은 다음 유형을 구분해야 한다.

| Type | Visibility | Author | Purpose |
| --- | --- | --- | --- |
| `DISCUSSION` | mostly `PUBLIC` | user | 커뮤니티 의견, 질문, 토론 |
| `REPORT` | `PRIVATE`, `UNLISTED`, or `PUBLIC` | user | 근거를 모아 정리한 리포트 |
| `AI_BRIEF` | `PUBLIC` or `UNLISTED` | system/admin | trend cluster 기반 저장형 요약 |

중요한 점은 AI 보조를 받은 사용자 글도 작성자는 사용자라는 것이다.
AI는 저자 역할이 아니라 assist operation으로 남긴다.

## AI 비용 정책이 요구하는 DB 구조

AI를 제품 기능으로 유지하려면 다음이 반드시 필요하다.

- `ai_usage_logs`: 모든 AI operation의 token/cost/status 기록
- `ai_budget_windows`: 월간/일간 account 또는 system budget
- `subscription_plans`: plan별 quota와 workspace limit
- `account_subscriptions`: account의 현재 plan
- `posts.post_type`, `posts.visibility`, `posts.assist_used`
- `drafts`: AI 호출 전후의 작성 작업 단위
- `post_evidence_links`, `draft_sources`: 사용자가 채택한 근거

이 구조가 없으면 운영자는 "왜 비용이 나갔는지"를 설명할 수 없다.

## Target Tables

### auth ownership

#### accounts

현재 구현의 계정 원본 테이블이다.
문서상 `users`로 이름을 바꾸는 선택지도 있지만, 실제 코드가 `accounts`를 사용하고 있으므로 target schema도 우선 `accounts`를 기준으로 둔다.

필요한 확장:

- `status`
- `version`: 계정 row 낙관적 락 감지
- `deleted_at`
- provider identity uniqueness
- plan/subscription relation은 billing 영역에서 별도 관리

#### account_roles

계정 role set이다.
MVP role은 `USER`, `ADMIN`이다.

### board ownership

#### posts

공개 게시글과 공개/비공개 리포트의 발행본을 담는다.
현재 구현의 `posts`를 확장하는 방향이 가장 현실적이다.

추가 후보:

- `post_type`: `DISCUSSION`, `REPORT`, `AI_BRIEF`
- `visibility`: `PUBLIC`, `PRIVATE`, `UNLISTED`
- `workspace_id`: private report가 특정 workspace에 속할 때 사용
- `assist_used`: AI 작성 보조 사용 여부
- `source_count`, `evidence_count`: 파생 count
- `status`, `deleted_at`: soft delete

#### post_evidence_links

게시글이 참고한 수집 item, trend cluster, 외부 URL을 저장한다.
공개 신뢰도 점수 대신 "이 글이 어떤 근거를 참고했는지"를 보여주는 핵심 테이블이다.

추천 필드:

- `post_id`
- `reference_type`: `COLLECTED_ITEM`, `TREND_CLUSTER`, `EXTERNAL_URL`
- `reference_id`
- `source_url`, `source_name`, `title`
- `link_type`: `AUTHOR_SELECTED`, `AI_SUGGESTED`, `AI_BRIEF_SOURCE`
- `display_order`

#### post_trend_links

게시글 상세의 관련 트렌드를 조회하기 위한 precomputed relation이다.
상세 페이지에서 AI를 호출하지 않기 위한 read model이다.

#### post_rank_scores

공개 신뢰도 점수가 아니라 목록 정렬용 내부 snapshot이다.

추천 입력:

- 최신성
- 참여도
- 근거 링크 수
- 출처 다양성
- trend recency

### workspace ownership

#### workspaces

개인 리포트 작업공간이다.
MVP에서는 account당 기본 workspace 하나로 시작할 수 있다.

#### workspace_members

초기에는 owner 1명만 있어도 된다.
나중에 협업 리포트 기능을 만들 경우 확장한다.

#### drafts

작성 중인 비공개 작업물이다.
게시글로 발행되기 전의 리포트나 토론글 초안을 표현한다.

필드 후보:

- `workspace_id`
- `author_account_id`
- `draft_type`: `DISCUSSION`, `REPORT`
- `visibility_intent`: 발행 예정 visibility
- `title`, `content`
- `status`: `DRAFT`, `ARCHIVED`, `PUBLISHED`
- `published_post_id`

#### draft_sources

초안에 붙여둔 수집 item/trend/external URL이다.
사용자가 발행하면 일부 또는 전체가 `post_evidence_links`로 복사된다.

#### saved_trend_bundles

사용자가 나중에 리포트에 쓰려고 저장한 trend 묶음이다.
유료 플랜 차별 지점이 될 수 있다.

### collector ownership

#### collector_sources

외부 API/provider 기준 정보다.

#### collector_source_policies

source별 timeout, retry, quota, circuit breaker 상태다.
운영 중 특정 source를 비활성화하거나 quota를 조정할 수 있게 한다.

#### collector_jobs

수집 작업 단위 이력이다.
스케줄러 실행, 수동 실행, source/keyword 기준 실행을 묶는다.

#### collector_api_requests

외부 API 호출 attempt 이력이다.
retry, timeout, rate limit, latency를 남긴다.

#### collected_items

현재 `collected_articles`의 확장 target이다.
원본에 가까운 metadata와 raw payload를 보관한다.

#### trend_clusters

여러 collected item을 하나의 이슈 단위로 묶는다.
처음에는 keyword/time window 기반으로 만들고, 이후 embedding/AI 요약을 붙일 수 있다.

#### trend_cluster_items

cluster와 collected item의 조인 테이블이다.

### billing / ai ownership

#### subscription_plans

무료/유료 plan별 제한을 저장한다.
공개 게시판의 신뢰 정보는 제한하지 않는다.

제한 후보:

- monthly AI token quota
- monthly AI operation quota
- private draft/report limit
- saved trend bundle limit

#### account_subscriptions

account별 현재 plan과 상태다.

#### ai_budget_windows

월간/일간 budget window다.
account별 budget과 system/global budget을 모두 표현할 수 있어야 한다.

#### ai_usage_logs

AI operation 단위의 원본 이력이다.
모델명, 입력/출력 token, 예상 비용, status, 실패 사유, account/workspace/draft/post context를 남긴다.

## 주요 관계

- `accounts` 1:N `posts`
- `accounts` 1:N `workspaces`
- `workspaces` 1:N `drafts`
- `drafts` 1:N `draft_sources`
- `posts` 1:N `post_evidence_links`
- `posts` 1:N `post_trend_links`
- `trend_clusters` 1:N `trend_cluster_items`
- `collected_items` 1:N `trend_cluster_items`
- `trend_clusters` 1:N `post_trend_links`
- `subscription_plans` 1:N `account_subscriptions`
- `accounts` 1:N `ai_usage_logs`
- `ai_budget_windows` 1:N `ai_usage_logs`

## 유스케이스 반영

### 외부 API 수집 후 포스팅 비용

- 수집 item은 먼저 `collected_items`에 저장한다.
- 모든 item을 AI 게시글로 만들지 않는다.
- `trend_clusters`가 일정 조건을 넘을 때만 AI brief 후보가 된다.
- AI brief 생성은 `ai_usage_logs`에 남긴다.
- 생성 결과는 저장형 `posts(post_type = AI_BRIEF)`로 남기므로 조회 시 AI 비용이 없다.

### 글쓰기 보조 비용

- 무료 사용자는 DB 기반 trend/source 추천만 받는다.
- 유료 사용자는 quota 내에서 AI 보조를 명시적으로 호출한다.
- 호출 단위는 `ai_usage_logs`에 남긴다.
- draft에 채택한 source는 `draft_sources`에 저장한다.
- 발행 시 채택한 source만 `post_evidence_links`로 복사한다.

### 상세 페이지 관련 트렌드 비용

- 상세 조회는 `post_trend_links`와 `trend_clusters`를 읽는다.
- 관련 트렌드 계산은 작성/발행/배치 시점에 수행한다.
- 조회 시점 AI 호출은 금지한다.

## 확정한 설계

- 제품은 공개 게시판과 개인 리포트 작업공간을 함께 가진다.
- 수익 모델은 공개 신뢰도를 제한하지 않고 개인 생산성/AI assist에 둔다.
- AI는 작성/발행/배치 시점에만 호출한다.
- 조회 시점에는 AI를 호출하지 않는다.
- 공개 신뢰도 점수는 만들지 않는다.
- 게시글/리포트의 근거는 evidence link로 보여준다.
- 모든 AI 호출은 usage log와 budget window에 연결한다.
- collector source별 정책과 API request log를 둔다.

## 남은 질문

- `workspace`를 별도 모듈로 만들지, 초기에는 `board` 안의 package로 시작할지?
- `billing/ai cost` 테이블을 별도 모듈로 둘지, `ai` 모듈이 소유할지?
- `reports`를 `posts(post_type = REPORT)`로 통합할지, 별도 `reports` 테이블로 분리할지?
- `trend_clusters`를 collector가 소유하되 board가 read API로만 접근할지, read model 복제를 둘지?
- plan 결제 자체를 MVP에 포함할지, 우선 plan/quota schema와 mock status만 둘지?
