# Modular Monolith Rationale

PostForge는 지금은 하나의 Spring Boot 애플리케이션으로 배포하지만, 내부 구조는 기능 경계별 Gradle module과 port 계약으로 나눈 modular monolith를 지향한다.
목표는 초기 운영 복잡도는 낮게 유지하면서, 트래픽/조직/장애 격리 필요가 생겼을 때 MSA로 분리하는 비용을 낮추는 것이다.
DB ownership, event outbox, API versioning, observability, PgVector ownership 같은 분산 시스템 개념은 [MSA Migration Concepts](./msa-migration-concepts.md)에 별도로 정리한다.

## Architecture Decision

현재 선택은 "MSA를 바로 도입하지 않고, MSA 후보 경계를 코드 안에서 먼저 검증하는 modular monolith"이다.

- 하나의 process로 실행하므로 배포, transaction, local debugging, 운영 관측 비용이 낮다.
- `auth`, `board`, `ai`, `ingest`, `collector`는 독립 기능 모듈로 두어 각 모듈이 자기 기술 의존성을 직접 선언한다.
- 모듈 간 호출은 가능한 구현 class가 아니라 `core`의 port 계약을 통한다.
- `app`은 composition root로 남기고, 실제 기능 로직은 기능 모듈로 둔다.
- Redis/OpenAPI/global exception handler 같은 Spring infrastructure는 `support`로 분리해 기능 모듈에 섞이지 않게 한다.
- 로컬 운영/테스트 도구는 `ops`에 두어 `app` composition root가 기능 구현으로 비대해지지 않게 한다.

## Why Not MSA First

MSA는 서비스별 독립 배포와 장애 격리에 유리하지만, 처음부터 도입하면 다음 비용이 즉시 생긴다.

- 서비스 간 네트워크 실패, timeout, retry, circuit breaker 설계가 필요하다.
- 분산 transaction을 피하기 위한 event/outbox/idempotency 설계가 필요하다.
- 서비스별 observability, 배포 파이프라인, secret, ingress, 인증 정책을 따로 운영해야 한다.
- 아직 검증되지 않은 도메인 경계를 API로 고정하면 이후 변경 비용이 커진다.

그래서 현재는 process는 하나로 유지하되, MSA에서 서비스 후보가 될 경계를 module/port/test 단위로 먼저 고정한다.

## Evidence In This Repository

| 설계 근거 | 현재 코드 근거 | MSA 전환 시 의미 |
|-----------|----------------|------------------|
| 실행 조립과 기능 로직 분리 | `app/build.gradle`, `app/src/main/java/dev/iamrat/app/ApplicationServer.java`, `ops/src/main/java/dev/iamrat/ops/testconsole` | `app`은 조립만 담당하므로 기능 모듈을 별도 service entrypoint로 옮기기 쉽다. |
| 기능 모듈별 기술 의존성 소유 | `auth/build.gradle`, `board/build.gradle`, `ai/build.gradle`, `ingest/build.gradle`, `collector/build.gradle`, `ops/build.gradle` | 모듈 하나를 떼어낼 때 필요한 framework/storage/client 의존성을 식별하기 쉽다. |
| 공통 계약 중심의 `core` | `core/src/main/java/dev/iamrat/board`, `core/src/main/java/dev/iamrat/ai`, `core/src/main/java/dev/iamrat/global` | Java interface/record 계약을 HTTP API, message schema, client adapter로 치환할 수 있다. |
| Spring infrastructure 분리 | `support/src/main/java/dev/iamrat/support` | 전역 web/config 코드가 기능 모듈에 섞이지 않아 서비스별 starter/config로 분해하기 쉽다. |
| AI -> Board 구현 결합 제거 | `ai`는 `PostWriter` port를 사용하고 `board`가 구현한다. | AI service로 분리하면 `PostWriter` 구현을 board HTTP/event client로 바꾸면 된다. |
| Ingest -> AI 구현 결합 제거 | `ingest`는 `NewsAnalysisPostPublisher` port를 사용하고 `ai`가 구현한다. | Ingest service로 분리하면 같은 port 뒤에 AI HTTP/event client adapter를 둘 수 있다. |
| Board -> Auth 구현 결합 방지 | `board` controller는 `UserPrincipal` 계약만 참조한다. | Board service는 auth 구현 class 없이 JWT 검증/JWK/introspection 방식으로 독립 가능하다. |

## Migration Path

1. `board` service 분리
   - `posts`, `comments`, `post_like`, `comment_like`, `post_file` 테이블 소유권을 board로 둔다.
   - 현재 `PostWriter`/`PostReader`/`CommentWriter`/`CommentReader` port 구현을 board 내부 구현에서 HTTP client 또는 event adapter로 바꾼다.
   - AI가 게시글을 발행할 때 직접 Java bean 호출 대신 board internal API 또는 message를 사용한다.

2. `ai` service 분리
   - `PostGenerationService`를 AI service 내부 use case로 유지한다.
   - `NewsAnalysisPostPublisher` port의 monolith 구현은 유지하고, ingest 쪽에는 원격 AI client 구현을 둔다.
   - OpenAI/PgVector 설정은 AI/RAG service가 소유하도록 정리한다.

3. `ingest` service 분리
   - `collector`가 현재 Java port로 호출하는 ingest 계약을 독립 ingest endpoint 또는 message 계약으로 옮긴다.
   - 문서 저장 후 자동 게시 요청은 AI service API 또는 message로 전달한다.
   - PgVector ownership은 AI/RAG service 기준으로 둔다. 현재 monolith에서 ingest가 `VectorStore` API를 직접 쓰는 부분은 분리 시 AI/RAG API 또는 message로 치환한다.

4. `auth` service 분리
   - `accounts`, `account_roles`, refresh token/email verification Redis key를 auth가 소유한다.
   - 다른 서비스는 auth 구현 module을 의존하지 않고 JWT 검증, JWK, introspection 중 하나로 인증 결과만 소비한다.
   - 현재 `UserPrincipal` 계약은 서비스 내부 principal DTO 또는 API gateway claim contract로 치환한다.

5. `support` 분리
   - `support`는 runtime service가 아니라 공통 Spring infrastructure library/starter 후보로 본다.
   - 서비스별로 필요한 config만 가져가고, global exception handler/OpenAPI/Redis 설정은 각 service policy에 맞게 분화한다.

## What This Does Not Solve Yet

현재 구조는 MSA 전환 비용을 낮추기 위한 코드 경계를 만든 것이지, MSA 자체를 완성한 것은 아니다.
분리 시점에는 다음 설계가 추가로 필요하다.

- 서비스별 DB ownership과 schema migration 전략
- transaction 경계를 넘는 흐름의 event/outbox/idempotency 설계
- API contract versioning과 backward compatibility 정책
- 서비스별 observability, deployment, secret, ingress, rate limit
- DB migration tool 도입 여부와 운영 적용 절차

## Explanation Script

이 프로젝트는 처음부터 MSA로 쪼개지 않고 modular monolith로 설계했습니다.
이유는 초기에는 하나의 process로 운영 복잡도를 낮추되, 나중에 서비스 분리가 필요할 때 비용이 커지지 않도록 기능 경계를 Gradle module과 port 계약으로 먼저 고정하기 위해서입니다.
예를 들어 ingest는 AI 구현체를 직접 부르지 않고 `NewsAnalysisPostPublisher` port만 의존하고, AI는 board 구현체를 직접 의존하지 않고 `PostWriter` port만 의존합니다.
따라서 MSA로 전환할 때 핵심 비즈니스 로직을 크게 다시 쓰기보다, port 뒤의 구현을 Java bean에서 HTTP client나 message adapter로 교체하는 방식으로 이동할 수 있습니다.
다만 DB ownership, event/outbox, service observability 같은 분산 시스템 문제는 분리 시점에 별도 설계가 필요하다고 명시했습니다.
