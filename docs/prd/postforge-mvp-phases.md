# PostForge MVP Phases

이 문서는 PostForge를 취업 포트폴리오용 MVP로 완성하기 위한 phase 계획이다.
목표는 기능을 계속 늘리는 것이 아니라, 면접에서 설명 가능한 문제 정의와 검증 흐름을 끝까지 완성하는 것이다.

기준 ERD는 [PostForge MVP ERD](../db/postforge-mvp-erd.md)를 따른다.

## MVP Definition

PostForge MVP는 다음 한 문장으로 설명할 수 있어야 한다.

> 외부 API에서 정보를 수집하고, 사용자가 설정한 관심 키워드에 맞는 새 정보만 이메일로 받아보는 Spring Boot 백엔드

MVP에서 증명해야 하는 것은 다음이다.

- 단순 CRUD 게시판을 넘어 외부 API 수집 데이터를 알림 서비스 데이터로 가공할 수 있다.
- 외부 API 호출 실패, timeout, retry, rate limit을 운영 관점에서 추적할 수 있다.
- 사용자가 관심 키워드를 구독할 수 있다.
- 수집 item과 키워드 구독을 매칭해 중복 없는 알림 이벤트를 만들 수 있다.
- 후속 이벤트 처리를 위한 standalone outbox 기반을 마련한다.
- 이메일 발송 성공/실패/재시도 상태를 추적할 수 있다.

## Explicit Non-Goals

아래 기능은 MVP에서 구현하지 않는다.
문서상 future extension으로만 남긴다.

| Area | Reason |
| --- | --- |
| private workspace | 리포트/초안/협업까지 들어가면 제품 범위가 급격히 커진다. |
| billing/subscription | 실제 결제 정책과 운영 책임 없이 테이블만 만들면 설득력이 떨어진다. |
| full AI writing assistant | 비용, quota, prompt 품질, UX까지 같이 풀어야 한다. |
| AI auto posting | 작성자 책임과 비용 구조가 복잡해진다. |
| public trust score / ranking | 키워드 알림 MVP에는 복합 랭킹이 필요하지 않다. |
| external MQ broker | DB outbox relay 이후 소비자가 늘어날 때 Kafka/RabbitMQ/SQS를 붙인다. |

## Phase Overview

| Phase | Name | Main Question | Output |
| --- | --- | --- | --- |
| 0 | Scope Freeze | 무엇을 만들지, 무엇을 버릴지 확정했는가? | MVP docs, ERD, implementation checklist |
| 1 | Board Baseline | 기본 게시판이 안정적으로 동작하는가? | CRUD/comment/like/file baseline, query baseline |
| 2 | Collector Control | 외부 API 호출을 통제하고 기록할 수 있는가? | source policy, job, request log, duplicate guard |
| 3 | Messaging Outbox | 후속 이벤트 처리를 위한 outbox 인프라를 독립 모듈로 고정했는가? | messaging module, outbox events, relay contract |
| 4 | Keyword Watch | 사용자가 관심 키워드를 구독하고 수집 item과 매칭할 수 있는가? | keyword subscriptions, matching rule |
| 5 | Email Notification | 매칭된 새 정보를 이메일로 보내고 실패를 추적할 수 있는가? | notification events, email delivery logs |
| 6 | Reliability and Performance | 수집/매칭/발송 흐름의 안정성과 성능을 설명할 수 있는가? | duplicate guard, retry, query/k6 comparison |
| 7 | Portfolio Packaging | 왜 이렇게 설계했는지 설명 가능한가? | README, diagrams, test docs, interview script |

## Phase 0. Scope Freeze

### Goal

MVP 범위를 고정한다.
이 phase 이후에는 private workspace, billing, full AI assistant를 구현 범위에 넣지 않는다.

### Tasks

- [ ] [PostForge MVP ERD](../db/postforge-mvp-erd.md)를 기준 ERD로 확정한다.
- [ ] 기존 큰 target ERD는 future extension 문서로 분리해서 유지한다.
- [ ] README의 제품 문장을 MVP 기준으로 맞춘다.
- [ ] 구현 대상 table과 제외 대상 table을 분리한다.
- [ ] 성능 테스트 대상 query를 미리 정한다.

### Exit Criteria

- "PostForge가 무엇을 하는 프로젝트인가"를 한 문장으로 설명할 수 있다.
- MVP included/deferred 범위가 문서로 분리되어 있다.
- 다음 phase에서 구현할 table과 API가 명확하다.

### Interview Story

처음에는 AI/리포트/결제까지 고려했지만, 취업용 MVP에서는 외부 정보 수집과 키워드 기반 이메일 알림이라는 핵심 문제로 범위를 줄였다고 설명한다.

## Phase 1. Board Baseline

### Goal

기존 게시판 기능을 성능 개선과 collector 연동의 기반으로 안정화한다.

### Scope

- 게시글 CRUD
- 댓글/대댓글
- 좋아요
- 태그
- 파일 첨부
- 조회수 처리
- 목록/상세 조회 baseline 측정

### DB Focus

- `posts`
- `post_tags`
- `comments`
- `post_like`
- `comment_like`
- `post_file`

### Tasks

- [ ] 기존 게시판 기능의 현재 동작을 정리한다.
- [ ] 목록 조회와 상세 조회 API의 query 흐름을 확인한다.
- [ ] N+1 발생 여부를 확인한다.
- [ ] 게시글 목록, 상세, 댓글 조회의 baseline 성능을 측정한다.
- [ ] 현재 index와 필요한 index 후보를 문서화한다.

### Exit Criteria

- 게시글 작성, 조회, 수정, 삭제 흐름이 동작한다.
- 댓글, 좋아요, 파일 첨부 흐름이 동작한다.
- 성능 개선 전 baseline 수치가 문서로 남아 있다.

### Verification

- unit/integration test
- API smoke test
- k6 또는 수동 부하 테스트 baseline
- query log 또는 Hibernate statistics 확인

## Phase 2. Collector Control

### Goal

외부 API를 단순 호출이 아니라 운영 가능한 수집 시스템으로 만든다.

### Scope

- 외부 source 등록
- source별 호출 정책
- 수집 job 기록
- API request attempt 기록
- timeout/retry/rate limit 처리
- 중복 수집 방지

### DB Focus

- `collector_sources`
- `collector_source_policies`
- `collector_jobs`
- `collector_api_requests`
- `collected_items`

### Tasks

- [ ] `collector_sources`로 외부 API/provider를 등록한다.
- [ ] `collector_source_policies`로 timeout, retry, quota, circuit state를 관리한다.
- [ ] 수집 실행 단위를 `collector_jobs`로 저장한다.
- [ ] 외부 API 호출 attempt를 `collector_api_requests`에 저장한다.
- [ ] 성공 응답을 `collected_items`로 정규화한다.
- [ ] `original_link` 또는 `dedupe_hash`로 중복 저장을 막는다.

### Exit Criteria

- 외부 API 호출 성공/실패/timeout/retry가 DB에 남는다.
- 같은 원문이 중복 저장되지 않는다.
- source별 수집 상태를 추적할 수 있다.

### Verification

- 정상 API 응답 저장 테스트
- timeout/retry 테스트
- rate limit 또는 skipped request 테스트
- duplicate guard 테스트

### Interview Story

외부 API는 장애와 제한이 있는 의존성이므로, 호출 결과만 저장하지 않고 request attempt와 source policy를 분리했다고 설명한다.

## Phase 3. Messaging Outbox

### Goal

후속 이벤트 기반 처리를 위해 기능 모듈과 직접 결합하지 않는 outbox 인프라를 먼저 고정한다.

### Scope

- `messaging` 모듈 추가
- `outbox_events` 저장
- 트랜잭션 필수 발행 API
- relay claim/retry contract
- 향후 MQ publisher adapter 확장 지점

### DB Focus

- `outbox_events`

### Tasks

- [ ] `messaging` 모듈을 추가한다.
- [ ] `outbox_events` entity/repository/migration을 추가한다.
- [ ] 도메인 트랜잭션 안에서만 호출 가능한 publisher를 만든다.
- [ ] 기능 모듈 연결은 하지 않고, standalone module test로 먼저 고정한다.
- [ ] `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED` 상태 전이를 테스트한다.
- [ ] relay가 재시도 가능한 이벤트를 claim하는 기준을 정의한다.

### Exit Criteria

- `messaging` 모듈이 독립적으로 outbox event를 저장할 수 있다.
- relay가 pending/failed 이벤트를 재처리 대상으로 식별할 수 있다.
- MQ 도입 전에도 DB 기반 이벤트 처리 계약을 설명할 수 있다.
- board/collector/notification 같은 기능 모듈과의 직접 연결은 후속 phase로 미룬다.

### Verification

- outbox entity 상태 전이 unit test
- outbox publisher serialization test
- messaging module compile/test

### Interview Story

Spring event만 쓰면 커밋 직후 프로세스 장애에서 이벤트가 유실될 수 있으므로, 후속 phase에서 도메인 쓰기와 같은 트랜잭션에 outbox row를 저장할 수 있는 기반을 먼저 분리했다고 설명한다.

## Phase 4. Keyword Watch

### Goal

사용자가 관심 키워드를 등록하고, 새 수집 item과 매칭할 수 있게 한다.

### Scope

- keyword subscription 등록/비활성화
- keyword normalization
- collected item과 subscription 매칭
- 중복 notification event 방지

### DB Focus

- `collected_items`
- `keyword_subscriptions`
- `notification_events`

### Tasks

- [ ] `keyword_subscriptions`를 추가한다.
- [ ] 사용자가 관심 키워드를 등록/비활성화할 수 있게 한다.
- [ ] keyword normalization 규칙을 만든다.
- [ ] 새 `collected_items`를 활성 구독과 매칭한다.
- [ ] 매칭 결과를 `notification_events`로 저장한다.
- [ ] `(subscription_id, collected_item_id, channel)` unique로 중복 알림을 막는다.

### Exit Criteria

- 사용자가 키워드를 구독할 수 있다.
- 새 수집 item이 구독 키워드와 매칭되면 알림 이벤트가 생성된다.
- 같은 item이 같은 구독자에게 중복 알림으로 생성되지 않는다.

### Verification

- keyword normalization unit test
- collected item fixture 기반 matching integration test
- duplicate notification guard test

### Interview Story

처음부터 추천/랭킹 시스템을 붙이지 않고, 사용자가 명시적으로 등록한 키워드와 수집 item을 매칭하는 방식으로 제품 범위를 줄였다고 설명한다.

## Phase 5. Email Notification

### Goal

매칭된 새 정보를 이메일로 발송하고 성공/실패/재시도 상태를 추적한다.

### Scope

- pending notification polling
- email template 생성
- email delivery log 저장
- 발송 성공/실패 상태 갱신
- 실패 재시도

### DB Focus

- `notification_events`
- `email_delivery_logs`
- `accounts`
- `collected_items`

### Tasks

- [ ] `email_delivery_logs`를 추가한다.
- [ ] `notification_events(status = PENDING)`를 읽는 mail worker를 만든다.
- [ ] 수집 item title/url/summary 기반 이메일 subject/body를 만든다.
- [ ] 발송 성공 시 `notification_events.status = SENT`로 갱신한다.
- [ ] 발송 실패 시 `email_delivery_logs.last_error`, `retry_count`를 기록한다.

### Exit Criteria

- pending 알림이 이메일 발송 worker를 통해 처리된다.
- 발송 성공/실패 이력이 DB에 남는다.
- 실패한 알림을 재시도할 수 있다.

### Verification

- email sender mock 기반 service test
- notification event 상태 전이 test
- failed delivery retry test

### Interview Story

외부 API 수집과 이메일 발송은 모두 실패 가능한 외부 의존성이므로, 발송 대상과 실제 발송 이력을 분리해 재시도와 장애 분석이 가능하게 했다고 설명한다.

## Phase 6. Reliability and Performance

### Goal

외부 API 수집, 키워드 매칭, 이메일 발송 흐름의 안정성과 성능을 수치로 설명한다.

### Scope

- duplicate guard
- outbox relay polling query
- notification polling query
- email retry flow
- index 적용 전후 비교
- k6 또는 fixture 기반 처리량 테스트 문서화

### DB Focus

- `collected_items`
- `keyword_subscriptions`
- `notification_events`
- `email_delivery_logs`
- `collector_api_requests`
- `outbox_events`

### Tasks

- [ ] 수집 중복 방지 전후를 비교한다.
- [ ] outbox relay polling/claim query를 최적화한다.
- [ ] keyword subscription 매칭 query를 최적화한다.
- [ ] pending notification polling query를 최적화한다.
- [ ] email delivery 실패/재시도 흐름을 문서화한다.
- [ ] k6 테스트 시나리오를 작성한다.
- [ ] 성능 개선 전후 결과를 `docs/performance`에 기록한다.

### Exit Criteria

- 수집/매칭/발송 흐름의 처리량 또는 latency 수치가 있다.
- 중복 방지와 재시도 상태 추적 근거가 있다.
- 어떤 index가 어떤 query를 위해 필요한지 설명할 수 있다.

### Verification

- k6 load test
- DB explain plan
- query/index comparison
- before/after performance report

### Interview Story

외부 API와 이메일은 실패 가능한 의존성이므로 request log, duplicate guard, notification state, email delivery log를 분리해 운영 가능한 파이프라인으로 만들었다고 설명한다.

## Phase 7. Portfolio Packaging

### Goal

구현 결과를 면접에서 설명 가능한 포트폴리오로 정리한다.

### Scope

- README 정리
- ERD 정리
- architecture 문서 정리
- 성능 테스트 결과 정리
- Docker/build/cache 최적화 결과 정리
- 면접 답변 스크립트 정리

### Tasks

- [ ] README를 MVP 제품 문장 기준으로 정리한다.
- [ ] MVP ERD와 큰 target ERD의 관계를 설명한다.
- [ ] collector, keyword subscription, notification, email delivery 흐름을 architecture 문서에 정리한다.
- [ ] 성능 테스트 결과를 before/after로 정리한다.
- [ ] Docker layered jar, runtime image, cache 테스트 결과를 연결한다.
- [ ] "왜 이 프로젝트를 만들었는가" 답변을 문서로 만든다.

### Exit Criteria

- README만 읽어도 프로젝트 주제가 보인다.
- ERD를 보고 테이블 역할을 설명할 수 있다.
- 성능 테스트 결과가 숫자로 남아 있다.
- 면접에서 설계 trade-off를 설명할 수 있다.

## MVP Complete Checklist

- [ ] 외부 API 수집 source와 policy가 존재한다.
- [ ] 수집 job과 request attempt가 기록된다.
- [ ] 수집 item 중복 저장이 방지된다.
- [ ] 사용자가 keyword subscription을 등록할 수 있다.
- [ ] 수집 item이 keyword subscription과 매칭된다.
- [ ] notification event가 중복 없이 생성된다.
- [ ] pending notification이 email delivery로 처리된다.
- [ ] 발송 성공/실패/재시도 이력이 기록된다.
- [ ] 성능 개선 전후 결과가 문서화되어 있다.
- [ ] README에 제품 문제, 설계 이유, 검증 결과가 정리되어 있다.

## Deferred Backlog

MVP 이후 여유가 있을 때만 검토한다.

| Backlog | Depends On |
| --- | --- |
| private report workspace | 게시판/리포트 기능을 다시 핵심으로 잡을 때 |
| saved trend bundles | trend aggregation 도입 이후 |
| trend aggregation | keyword notification 품질 검증 이후 |
| post reference links | 게시판 글쓰기 확장 시 |
| ranking/read model | 쇼핑몰/추천 분기에서 상품 노출 최적화가 필요할 때 |
| AI writing assist | Phase 2 collector logs, notification 품질 검증 이후 |
| AI usage budget | 실제 AI 기능 도입 결정 |
| billing/subscription | private workspace 또는 AI quota가 실제 제품 기능이 된 이후 |
| AI brief generation | trend cluster 품질이 충분히 검증된 이후 |

## Stop Rule

MVP 중간에 새 기능이 떠오르면 바로 구현하지 않는다.
다음 질문을 통과한 경우에만 phase에 포함한다.

1. 이 기능이 MVP 제품 문장을 더 선명하게 만드는가?
2. 이 기능이 성능 테스트나 설계 설명에 직접 도움이 되는가?
3. 이 기능을 구현하지 않으면 phase exit criteria를 만족할 수 없는가?

세 질문 중 하나도 통과하지 못하면 deferred backlog로 보낸다.
