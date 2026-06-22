# ADR-003 모듈러 모놀리스

PostForge는 지금은 하나의 Spring Boot 애플리케이션으로 배포하지만, 내부 구조는 기능 경계별 Gradle module과 port 계약으로 나눈 modular monolith를 지향한다.
목표는 초기 운영 복잡도는 낮게 유지하면서, 트래픽/조직/장애 격리 필요가 생겼을 때 MSA로 분리하는 비용을 낮추는 것이다.
DB ownership은 [DB Schema Ownership](../database/schema-ownership.md), 이벤트 발행 경계는 [이벤트 기반 아웃박스](../architecture/event-driven-outbox.md)에 둔다.

## 결정

현재 선택은 "MSA를 바로 도입하지 않고, MSA 후보 경계를 코드 안에서 먼저 검증하는 modular monolith"이다.

- 하나의 process로 실행하므로 배포, transaction, local debugging, 운영 관측 비용이 낮다.
- `auth`, `board`, `source`, `ingest`, `catalog`, `price`, `ai`는 독립 기능 모듈로 두어 각 모듈이 자기 기술 의존성을 직접 선언한다.
- 모듈 간 호출은 가능한 구현 class가 아니라 `core`의 port 계약을 통한다.
- `app`은 composition root로 남기고, 실제 기능 로직은 기능 모듈로 둔다.
- Redis guard, JPA auditing, request logging, exception response base 같은 Spring infrastructure는 `support`로 분리해 기능 모듈에 섞이지 않게 한다.
- OpenAPI, route/security, 실제 global exception advice, 로컬 테스트 콘솔은 실행 조립 정책이므로 `app`에 둔다.

## MSA를 먼저 도입하지 않는 이유

MSA는 서비스별 독립 배포와 장애 격리에 유리하지만, 처음부터 도입하면 다음 비용이 즉시 생긴다.

- 서비스 간 네트워크 실패, timeout, retry, circuit breaker 설계가 필요하다.
- 분산 transaction을 피하기 위한 event/outbox/idempotency 설계가 필요하다.
- 서비스별 observability, 배포 파이프라인, secret, ingress, 인증 정책을 따로 운영해야 한다.
- 아직 검증되지 않은 도메인 경계를 API로 고정하면 이후 변경 비용이 커진다.

그래서 현재는 process는 하나로 유지하되, MSA에서 서비스 후보가 될 경계를 module/port/test 단위로 먼저 고정한다.

## 이 저장소의 근거

| 설계 근거 | 현재 코드 근거 | MSA 전환 시 의미 |
|-----------|----------------|------------------|
| 실행 조립과 기능 로직 분리 | `app/build.gradle`, `app/src/main/java/dev/iamrat/app/ApplicationServer.java` | `app`은 조립만 담당하므로 기능 모듈을 별도 service entrypoint로 옮기기 쉽다. |
| 기능 모듈별 기술 의존성 소유 | `auth/build.gradle`, `board/build.gradle`, `source/build.gradle`, `ingest/build.gradle`, `catalog/build.gradle`, `price/build.gradle`, `ai/build.gradle` | 모듈 하나를 떼어낼 때 필요한 framework/storage/client 의존성을 식별하기 쉽다. |
| 공통 계약 중심의 `core` | `core/src/main/java/dev/iamrat/core` | Java interface/record 계약을 HTTP API, message schema, client adapter로 치환할 수 있다. |
| Spring infrastructure 분리 | `support/src/main/java/dev/iamrat/support` | 공통 Redis/web/persistence 보조 코드가 기능 모듈에 섞이지 않아 서비스별 starter/config로 분해하기 쉽다. |
| AI -> Board 구현 결합 제거 | `ai`는 게시글 초안 생성과 RAG를 소유하고, 게시글 발행은 `PostWriter` 같은 board 계약 뒤로 둔다. | AI service로 분리하면 해당 구현을 board HTTP/event client로 바꾸면 된다. |
| Ingest -> Source/Catalog/Price 경계 분리 | `ingest`는 source contract를 통해 상품/뉴스를 수집하고 catalog/price application API로 상품 정규화와 가격 기록을 위임한다. | Ingest service로 분리하면 source/catalog/price 호출을 HTTP API 또는 message adapter로 바꾸면 된다. |
| Board -> Auth 구현 결합 방지 | `board` controller는 `UserPrincipal` 계약만 참조한다. | Board service는 auth 구현 class 없이 JWT 검증/JWK/introspection 방식으로 독립 가능하다. |

## 전환 경로

1. `board` service 분리
   - `posts`, `comments`, `post_like`, `comment_like`, `post_file` 테이블 소유권을 board로 둔다.
   - 현재 `PostWriter` port 구현을 board 내부 구현에서 HTTP client 또는 event adapter로 바꾼다.
   - AI가 게시글을 발행할 때 직접 Java bean 호출 대신 board internal API 또는 message를 사용한다.

2. `ai` service 분리
   - `PostDraftGenerationService`를 AI service 내부 use case로 유지한다.
   - 게시글 발행이 필요해지면 board 원격 client 구현으로 교체한다.
   - OpenAI/PgVector 설정은 AI/RAG service가 소유하도록 정리한다.

3. `source` / `ingest` / `catalog` / `price` service 분리
   - `source`는 외부 상품/뉴스 API adapter와 provider별 parsing을 소유한다.
   - `ingest`는 tracked keyword, collection job, raw product payload를 소유한다.
   - `catalog`는 정규화 상품과 offer를 소유한다.
   - `price`는 price snapshot history를 소유한다.
   - 현재 Java bean 호출은 분리 시 HTTP API 또는 message adapter로 치환한다.

4. `auth` service 분리
   - `accounts`, `account_roles`, refresh token/email verification Redis key를 auth가 소유한다.
   - 다른 서비스는 auth 구현 module을 의존하지 않고 JWT 검증, JWK, introspection 중 하나로 인증 결과만 소비한다.
   - 현재 `UserPrincipal` 계약은 서비스 내부 principal DTO 또는 API gateway claim contract로 치환한다.

5. `support` 분리
   - `support`는 runtime service가 아니라 공통 Spring infrastructure library/starter 후보로 본다.
   - 서비스별로 필요한 config만 가져가고, global exception handler/OpenAPI/Redis 설정은 각 service policy에 맞게 분화한다.

## 아직 해결하지 않는 것

현재 구조는 MSA 전환 비용을 낮추기 위한 코드 경계를 만든 것이지, MSA 자체를 완성한 것은 아니다.
분리 시점에는 다음 설계가 추가로 필요하다.

- 서비스별 DB ownership과 schema migration 전략
- transaction 경계를 넘는 흐름의 event/outbox/idempotency 설계
- API contract versioning과 backward compatibility 정책
- 서비스별 observability, deployment, secret, ingress, rate limit
- DB migration tool 도입 여부와 운영 적용 절차

## 설명 스크립트

이 프로젝트는 처음부터 MSA로 쪼개지 않고 modular monolith로 설계했습니다.
이유는 초기에는 하나의 process로 운영 복잡도를 낮추되, 나중에 서비스 분리가 필요할 때 비용이 커지지 않도록 기능 경계를 Gradle module과 port 계약으로 먼저 고정하기 위해서입니다.
예를 들어 ingest는 외부 상품 API 구현체를 직접 부르지 않고 `SourceRequestExecutor` 계약을 통하고, AI는 board 구현체를 직접 소유하지 않고 게시글 생성/발행 계약 뒤에서 동작합니다.
따라서 MSA로 전환할 때 핵심 비즈니스 로직을 크게 다시 쓰기보다, module/application API 뒤의 구현을 Java bean에서 HTTP client나 message adapter로 교체하는 방식으로 이동할 수 있습니다.
다만 DB ownership, event/outbox, service observability 같은 분산 시스템 문제는 분리 시점에 별도 설계가 필요하다고 명시했습니다.
