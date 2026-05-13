# MSA Migration Concepts

이 문서는 PostForge를 modular monolith에서 MSA로 전환할 때 반드시 이해해야 하는 실무 개념을 정리한다.
현재 코드는 하나의 Spring Boot process 안에서 실행되지만, 모듈 간 호출을 port 계약으로 감싸 두었기 때문에 나중에 일부 port 구현을 HTTP client 또는 message publisher로 교체할 수 있다.

## Java Bean Call To Adapter

현재 PostForge의 자동 게시 흐름은 다음 구조다.

```text
ingest AutoPostOrchestrator
  -> core NewsAnalysisPostPublisher port
    -> ai PostGenerationService implementation
      -> core PostWriter port
        -> board BoardPostWriter implementation
```

지금은 `ingest`, `ai`, `board`가 같은 Spring Boot 애플리케이션 안에 있으므로 Spring이 같은 JVM 안의 Java bean을 주입한다.
즉 `AutoPostOrchestrator`가 `NewsAnalysisPostPublisher` interface를 의존하면, 런타임에는 Spring DI가 `PostGenerationService` bean을 연결한다.

```text
same JVM
AutoPostOrchestrator.publishEligible()
  -> PostGenerationService.publishNewsAnalysis()
```

이 호출은 일반 Java method call이다.
네트워크, JSON serialization, timeout, retry, circuit breaker가 없다.
디버깅과 테스트가 쉽고 latency도 낮지만, `ingest`와 `ai`가 같은 process에 같이 배포된다는 전제가 있다.

MSA로 `ai`를 별도 서비스로 분리하면 `ingest` process 안에는 더 이상 `PostGenerationService` class가 없다.
대신 같은 port를 구현하는 adapter를 둔다.

### HTTP Adapter

동기 응답이 필요하면 port 뒤 구현을 HTTP client로 바꾼다.

```text
ingest service
AutoPostOrchestrator
  -> NewsAnalysisPostPublisher
    -> AiNewsAnalysisHttpAdapter
      -> POST http://ai-service/internal/news-analysis-posts

ai service
NewsAnalysisController
  -> PostGenerationService
```

이때 `AutoPostOrchestrator`의 핵심 정책은 유지된다.
바뀌는 것은 `NewsAnalysisPostPublisher`의 구현체다.

HTTP adapter가 책임져야 할 것은 다음이다.

- request/response JSON contract 관리
- connect/read timeout
- 4xx/5xx status mapping
- retry 가능 여부 판단
- idempotency key 전달
- trace/correlation id 전달
- 장애 시 fallback 또는 실패 처리 정책

HTTP는 "요청 결과가 바로 필요할 때" 적합하다.
예를 들어 사용자가 버튼을 눌렀고 즉시 성공/실패를 알려줘야 하는 흐름은 HTTP가 이해하기 쉽다.

### Message Adapter

응답을 바로 받을 필요가 없고, 오래 걸리거나 실패해도 나중에 재처리할 수 있는 작업이면 message adapter가 더 적합하다.

```text
ingest service
AutoPostOrchestrator
  -> NewsAnalysisPostPublisher
    -> NewsAnalysisMessagePublisher
      -> publish news-analysis.requested event

message broker
  -> ai service consumer
    -> PostGenerationService
      -> board service call/message
```

message adapter는 method call을 "명령 또는 이벤트 발행"으로 바꾼다.
호출자는 보통 "요청을 접수했다"는 결과만 받고, 실제 처리는 consumer가 비동기로 수행한다.

message adapter가 책임져야 할 것은 다음이다.

- topic/queue 이름과 event schema 관리
- event id, idempotency key, correlation id 생성
- publish 실패 시 outbox 또는 retry 처리
- consumer 중복 처리 방지
- dead-letter queue와 재처리 정책
- eventual consistency를 사용자 경험에 어떻게 노출할지 결정

Message는 "후행 작업, 대량 처리, 장애 격리, 재시도"가 중요한 흐름에 적합하다.
PostForge의 뉴스 문서 적재 후 AI 자동 게시 흐름은 이미 best-effort 후행 작업이므로, MSA 전환 시 message 방식이 자연스럽다.

### Important Constraint

port를 둔다고 MSA 전환이 자동으로 끝나는 것은 아니다.
port는 비즈니스 코드가 transport 기술에 오염되지 않게 막는 장치다.
MSA로 전환할 때는 port 뒤 adapter를 바꾸면서 timeout, retry, idempotency, event ordering, monitoring 같은 분산 시스템 문제를 새로 설계해야 한다.

## DB Ownership

DB ownership은 "어떤 서비스가 어떤 데이터를 쓰고 읽을 최종 권한을 가지는가"를 정하는 규칙이다.

MSA에서 가장 흔한 실패 패턴은 여러 서비스가 같은 DB table을 직접 읽고 쓰는 shared database다.
shared database는 초반에는 편하지만, 서비스별 독립 배포와 schema 변경을 거의 불가능하게 만든다.
한 서비스가 column을 바꾸면 다른 서비스 query가 깨질 수 있고, transaction 경계도 흐려진다.

원칙은 다음이다.

- 하나의 table은 하나의 service가 소유한다.
- 다른 service는 소유 service의 API 또는 event를 통해서만 데이터를 소비한다.
- 서비스 간 foreign key는 DB constraint 대신 business id/reference id로 표현한다.
- 조회 성능 때문에 다른 서비스 데이터가 필요하면 read model 또는 cache projection을 둔다.
- schema migration은 owner service의 release lifecycle에 맞춘다.

PostForge의 후보 ownership은 다음처럼 잡을 수 있다.

| Service 후보 | 소유 데이터 후보 | 이유 |
|---------------|------------------|------|
| `auth` | `members`, `member_roles`, refresh token/email verification Redis key | 회원 identity, credential, token state는 인증 도메인이다. |
| `board` | `posts`, `post_tags`, `comments`, `post_like`, `comment_like`, `post_file` | 게시글/댓글/좋아요/첨부파일은 커뮤니티 도메인이다. |
| `ai` 또는 `rag` | embedding, vector index, prompt/output guardrail 관련 데이터 | 검색 품질, embedding model, vector schema는 AI/RAG 변경과 강하게 묶인다. |
| `ingest` | ingest request log, crawl ingest state, deduplication key | 외부 수집 요청의 수신/중복/처리 상태는 적재 도메인이다. |

현재 modular monolith 단계에서는 물리 DB가 하나여도 괜찮다.
다만 코드와 문서에서 "이 table은 어느 module이 owner인가"를 먼저 정해 두면, 나중에 schema 분리와 service extraction 순서가 명확해진다.

## Event Outbox

event outbox는 DB 변경과 message 발행을 같은 transaction boundary 안에 묶기 위한 패턴이다.

분산 시스템에서 자주 생기는 문제는 dual write다.

```text
1. posts table에 게시글 저장 성공
2. message broker에 post-created event 발행 실패
```

또는 반대 상황도 가능하다.

```text
1. message broker에 event 발행 성공
2. DB transaction rollback
```

이러면 DB 상태와 event 상태가 서로 어긋난다.
outbox pattern은 business table 변경과 `outbox_events` insert를 같은 DB transaction 안에서 처리한다.

```text
transaction start
  insert posts
  insert outbox_events(type='post.created', payload='{...}')
transaction commit

outbox relay
  read unpublished outbox_events
  publish to broker
  mark as published
```

핵심은 "비즈니스 변경이 commit되면 event 발행 의도도 반드시 DB에 남는다"는 점이다.
broker publish가 잠시 실패해도 relay가 나중에 다시 보낼 수 있다.

outbox table에는 보통 다음 필드가 들어간다.

| 필드 | 의미 |
|------|------|
| `event_id` | event 고유 id. consumer idempotency key로도 쓸 수 있다. |
| `aggregate_type` | `post`, `document`, `member` 같은 도메인 대상 |
| `aggregate_id` | 대상 id |
| `event_type` | `post.created`, `news-analysis.requested` 등 |
| `payload` | JSON event body |
| `status` | `PENDING`, `PUBLISHED`, `FAILED` 등 |
| `created_at`, `published_at` | relay와 운영 추적용 timestamp |

PostForge에서 outbox가 필요한 후보는 다음이다.

- 게시글 생성 후 검색 색인 또는 알림 이벤트 발행
- 문서 적재 후 AI 분석 요청 이벤트 발행
- 회원 가입 후 이메일/프로필/활동 이벤트 발행
- 좋아요/댓글 생성 후 집계 projection 갱신

Outbox와 함께 반드시 나오는 개념이 idempotency다.
message는 중복 전달될 수 있으므로 consumer는 같은 `event_id`를 두 번 처리해도 결과가 깨지지 않아야 한다.

## API Versioning

API versioning은 service contract를 변경할 때 기존 consumer를 깨뜨리지 않기 위한 정책이다.

MSA에서는 한 서비스의 API를 여러 consumer가 사용한다.
provider가 response field 이름을 바꾸거나 required field를 추가하면 consumer 배포 없이 장애가 날 수 있다.
그래서 API는 코드 내부 method보다 훨씬 보수적으로 변경해야 한다.

기본 원칙은 다음이다.

- response field 추가는 대체로 backward-compatible이다.
- response field 삭제, 이름 변경, 타입 변경은 breaking change다.
- request required field 추가는 breaking change다.
- enum 값 추가도 consumer 구현에 따라 breaking change가 될 수 있다.
- breaking change는 `/v2` endpoint, media type version, event schema version 등으로 분리한다.

HTTP API 예시는 다음과 같다.

```text
POST /internal/v1/news-analysis-posts
POST /internal/v2/news-analysis-posts
```

event schema도 version을 가져야 한다.

```json
{
  "eventType": "news-analysis.requested",
  "schemaVersion": 1,
  "eventId": "..."
}
```

PostForge에서 MSA 전환을 준비할 때는 현재 `core`의 record/interface 계약을 그대로 원격 API contract 후보로 볼 수 있다.
예를 들어 `NewsAnalysisPostRequest`는 나중에 HTTP request body 또는 message payload의 초안이 될 수 있다.
다만 Java type이 외부 contract가 되는 순간 backward compatibility 정책과 contract test가 필요하다.

## Observability

Observability는 "운영 중인 시스템 내부 상태를 외부 신호로 추론할 수 있게 만드는 능력"이다.
MSA에서는 장애가 한 process 안에서 끝나지 않고 여러 서비스 호출 경로에 퍼지므로 observability가 필수다.

실무에서는 보통 세 가지 신호를 본다.

| 신호 | 목적 | 예시 |
|------|------|------|
| Logs | 특정 요청/오류의 상세 맥락 확인 | `correlationId`, `userId`, `postId`, stack trace |
| Metrics | 시스템 상태를 숫자로 집계 | request count, latency, error rate, queue lag |
| Traces | 서비스 간 호출 경로 추적 | ingest -> ai -> board 전체 span |

MSA에서는 correlation id 또는 trace id가 중요하다.
예를 들어 크롤러가 문서를 보냈고, ingest가 AI 분석을 요청했고, AI가 board에 게시글을 생성했다면 이 전체 흐름을 하나의 trace로 묶어야 한다.
그래야 장애가 났을 때 어느 서비스에서 실패했는지 찾을 수 있다.

PostForge에서 observability를 MSA 수준으로 올릴 때 필요한 것은 다음이다.

- 모든 inbound request에 correlation id 부여
- service 간 HTTP/message에 correlation id 전파
- module/service별 latency, error rate, throughput metric
- AI 호출 latency와 실패율 metric
- vector search latency와 hit count metric
- message broker 도입 시 queue lag, retry count, dead-letter count metric
- Grafana dashboard를 service 단위와 business flow 단위로 분리

실무적으로는 "로그가 많다"와 "관측 가능하다"는 다르다.
관측 가능하려면 장애 질문에 답할 수 있어야 한다.

- 어떤 요청이 실패했나?
- 어느 service/span에서 실패했나?
- retry가 있었나?
- message가 밀렸나?
- 특정 배포 이후 error rate가 올랐나?
- 외부 API 또는 DB latency가 병목인가?

## PgVector Ownership

PgVector ownership은 "vector store를 어느 service의 데이터로 볼 것인가"의 문제다.
이 결정은 AI/RAG 구조에서 중요하다.

선택지는 크게 두 가지다.

### Option A: AI/RAG Service Owns PgVector

```text
ingest service
  -> send document ingest command

ai/rag service
  -> embed document
  -> store vector
  -> search vector
  -> generate answer/post
```

장점:

- embedding model, dimension, chunking, vector index 변경을 AI/RAG service가 독립적으로 결정한다.
- vector search 품질 개선이 ingest service 변경과 분리된다.
- PgVector schema가 AI/RAG 내부 구현 detail이 된다.

단점:

- ingest는 저장 완료 여부를 원격 API/message로 확인해야 한다.
- 대량 문서 적재 시 AI/RAG service 처리량과 queue 설계가 중요해진다.

### Option B: Ingest Service Owns PgVector

```text
ingest service
  -> embed/store vector

ai service
  -> query ingest/vector API
  -> generate answer/post
```

장점:

- 외부 문서 수집/정제/중복 제거와 vector 저장을 한 service가 담당한다.
- ingest pipeline 관점에서는 ownership이 단순하다.

단점:

- embedding model과 vector search 품질이 AI 기능인데도 ingest가 AI 기술 결정에 묶인다.
- AI service가 검색을 위해 ingest service API에 강하게 의존한다.
- RAG tuning을 할 때 service boundary가 어색해질 수 있다.

PostForge의 권장 방향은 Option A다.
즉 AI/RAG service가 PgVector를 소유하고, ingest는 "문서를 적재해 달라"는 command를 보내는 쪽이 장기적으로 자연스럽다.
현재 monolith에서는 `ingest`가 `VectorStore` API를 직접 사용하지만, PgVector 구현체와 OpenAI embedding 설정은 `ai` module에 있다.
MSA 전환 시에는 이 경계를 더 밀어서 `ingest`가 vector store 자체를 모르게 만들고, AI/RAG service API 또는 message로 문서를 넘기는 방향이 좋다.

## Practical Migration Roadmap

PostForge 기준으로 현실적인 단계는 다음과 같다.

1. Modular monolith boundary hardening
   - 기능 모듈별 의존성 정리
   - `core` port 계약으로 구현 결합 제거
   - module 단위 test 유지

2. Service contract preparation
   - port request/response를 HTTP/message DTO 후보로 정리
   - breaking change 기준 문서화
   - idempotency key와 correlation id 설계

3. First extraction
   - 가장 독립적인 흐름 하나를 고른다.
   - 추천 후보는 `crawl`처럼 이미 process가 분리된 영역 또는 `ai/rag`처럼 외부 API/비동기 처리 성격이 강한 영역이다.
   - adapter를 Java bean 구현에서 HTTP/message 구현으로 바꾼다.

4. Production MSA hardening
   - service별 DB ownership 확정
   - outbox/relay 또는 transactional messaging 도입
   - OpenTelemetry tracing, Prometheus metrics, Grafana dashboard 구성
   - contract test와 backward compatibility 정책 도입

## Interview Explanation Script

PostForge는 지금 MSA를 바로 적용하지 않고 modular monolith로 설계했습니다.
이유는 초기 운영 복잡도를 낮추면서도, 나중에 서비스 분리가 필요해질 때 핵심 비즈니스 로직을 다시 쓰지 않기 위해서입니다.
모듈 간 호출은 구현 class가 아니라 `core`의 port 계약을 통해 이루어지고, 현재는 Spring이 같은 JVM 안의 Java bean 구현을 주입합니다.
나중에 `ai`나 `board`를 별도 서비스로 분리하면 같은 port 뒤의 구현만 HTTP client 또는 message publisher adapter로 교체할 수 있습니다.
다만 MSA 전환 시에는 DB ownership, event outbox, API versioning, observability, PgVector ownership 같은 분산 시스템 설계를 추가로 해야 하므로, 현재 문서에 남은 과제까지 명시했습니다.
