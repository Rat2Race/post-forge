# Module Dependency Policy

이 문서는 PostForge 메인 애플리케이션의 멀티 모듈 의존성 배치 기준을 정리한다.
목표는 각 모듈의 `build.gradle`만 보고도 그 모듈이 직접 사용하는 기술을 파악할 수 있게 하는 것이다.
각 `build.gradle` 설정의 상세 이유는 [Gradle Dependency Rationale](./gradle-dependency-rationale.md)에 정리한다.
modular monolith 선택 근거와 MSA 전환 경로는 [Modular Monolith Rationale](./modular-monolith-rationale.md)에 별도로 정리한다.
MSA 전환 시 필요한 분산 시스템 개념은 [MSA Migration Concepts](./msa-migration-concepts.md)에 정리한다.
DB/table ownership과 migration convention은 [DB Schema Ownership](../db/schema-ownership.md)에 정리한다.

## 원칙

- `api`는 다른 모듈의 컴파일 API에 노출되는 타입에만 사용한다.
- 내부 구현에만 필요한 라이브러리는 `implementation`으로 둔다.
- `core`는 공통 계약과 가벼운 공통 DTO/예외 타입만 제공한다.
- Spring infrastructure 전역 설정은 `support`가 소유한다.
- 기능 모듈은 자신이 직접 사용하는 기술 의존성을 직접 선언한다.
- 버전은 가능한 루트 `build.gradle`의 `ext` 값으로 모은다.
- `app`은 실행 조립 모듈이다. 소스는 `ApplicationServer`와 배포 정책 중심으로 유지하고, runtime composition에 필요한 의존성은 `app/build.gradle`에 둔다.
- 로컬 운영/테스트 도구는 `app`에 직접 구현하지 않고 `ops`처럼 분리된 조립 대상 모듈에 둔다.

## 현재 모듈 책임

| 모듈 | 책임 | 직접 선언해야 하는 주요 기술 |
|------|------|------------------------------|
| `app` | 실행 진입점, 전체 모듈 조립, JPA repository/entity scan bootstrap, 배포 route/OpenAPI 정책 | Spring Web, Actuator, Spring Data JPA, Prometheus, PostgreSQL runtime, `support` |
| `core` | 공통 DTO, 공통 예외 타입, 공통 security principal/API metadata 계약, 모듈 간 board/AI/ingest port 및 metadata 계약 | Spring Web API, Spring Data Commons, Jackson annotations |
| `support` | Spring infrastructure 지원, 전역 bean/config/advice | Redis, Spring MVC, SpringDoc, Spring Security Core |
| `auth` | 회원, 로그인, JWT, OAuth2, 이메일 인증, security filter chain | Web, Validation, JPA, Redis, Security, JJWT, Mail, OAuth2 Client |
| `board` | 게시글, 댓글, 좋아요, 파일/S3, 조회수 | Web, Validation, JPA, Redis, Security, AWS S3 SDK |
| `ai` | AI 채팅, 게시글 생성, OpenAI/PgVector 설정 | Web, Validation, JDBC, Spring AI OpenAI, Spring AI PgVector |
| `ingest` | 문서 적재 API, internal collector ingest, 자동 게시 orchestration | Web, Validation, Spring TX, Spring AI VectorStore API |
| `collector` | 외부 source API 수집, source별 parsing, 수집 중복 판단 | Web, JPA |
| `messaging` | 공통 outbox 이벤트 저장, relay claim/retry, MQ adapter 확장 지점 | JPA, Jackson |
| `ops` | 로컬 운영/테스트 콘솔, 허용된 setup runner action 실행, 테스트 산출물 조회 | Web |

## Core vs Support

`core`와 `support`는 모두 여러 모듈에서 쓰일 수 있지만 성격이 다르다.
`core`는 모듈 간 compile-time 계약을 정의하고, `support`는 Spring runtime에 등록되는 공통 infrastructure를 제공한다.

```text
core    = 모듈들이 공유하는 계약 / shared kernel / port definition layer
support = Spring 애플리케이션을 띄우기 위한 공통 infrastructure 설정
```

| 구분 | `core` | `support` |
|------|--------|-----------|
| 실무 표현 | shared kernel, contract module, port definition layer | infrastructure support module, shared Spring configuration module |
| 성격 | 계약 | 인프라 구현 |
| 주 역할 | 모듈 간 약속 정의 | Spring 실행에 필요한 공통 bean/config/advice 제공 |
| 들어가는 것 | interface, record, 공통 DTO, 공통 예외 타입, principal 계약 | `@Configuration`, `@Bean`, `@RestControllerAdvice`, OpenAPI 설정 |
| 들어가면 안 되는 것 | Redis/OpenAPI 설정, S3/OpenAI 구현, JPA 구현, feature service | 도메인 port 계약, 비즈니스 규칙, feature service |
| 의존성 방향 | 기능 모듈이 compile-time으로 참조 가능 | 주로 `app`이 runtime에 조립하고, slice test에서 일부 참조 |
| MSA 전환 시 의미 | HTTP API/message schema/client contract로 치환될 후보 | 서비스별 starter/config 또는 각 서비스 local config로 분화될 후보 |

판단 기준은 다음과 같다.

- 다른 모듈이 compile할 때 알아야 하는 약속이면 `core`에 둔다.
- Spring이 실행될 때 등록해야 하는 공통 bean/config/advice이면 `support`에 둔다.
- 특정 기능 도메인의 실제 구현이면 `auth`, `board`, `ai`, `ingest`에 둔다.
- 최종 실행 조립이면 `app`에 둔다.

예를 들어 `NewsAnalysisPostPublisher`는 `ingest`와 `ai` 사이의 호출 계약이므로 `core`에 둔다.
`NewsDocumentMetadata`도 `collector`가 만든 뉴스 문서를 `ingest`가 자동 게시 후보로 해석하기 위한 module 간 metadata 계약이므로 `core`에 둔다.
반면 `GlobalExceptionHandler`는 Spring MVC runtime에서 예외를 JSON 응답으로 바꾸는 infrastructure 구현이므로 `support`에 둔다.

## Core API Surface

`core`의 `api` 의존성은 다음 항목으로 제한한다.

| 의존성 | `api`인 이유 |
|--------|--------------|
| `org.springframework:spring-web` | `ErrorCode`가 `HttpStatus`를 public getter로 노출한다. |
| `org.springframework.data:spring-data-commons` | `PageResponse.from(Page<T>)`가 Spring Data `Page`를 public method parameter로 노출한다. |
| `com.fasterxml.jackson.core:jackson-annotations` | `ErrorResponse`가 Jackson annotation을 public class metadata로 사용한다. |

`core`는 Redis, SpringDoc, Spring MVC handler, servlet API, Spring Security 구현 의존성을 갖지 않는다.

## Support Infrastructure Surface

`support`는 다음 전역 Spring infrastructure를 소유한다.

| 클래스 | 책임 |
|--------|------|
| `dev.iamrat.support.config.RedisConfig` | 공통 `RedisTemplate<String, String>` bean 생성 |
| `dev.iamrat.support.openapi.OpenApiConfig` | OpenAPI metadata와 security scheme 구성 |
| `dev.iamrat.support.web.GlobalExceptionHandler` | MVC 전역 예외 응답 처리 |

`support`의 구현 의존성은 다른 기능 모듈에 compile API로 노출하지 않는다.
실행 조립은 `app`이 `implementation project(':support')`로 담당한다.
기능 모듈의 MVC slice test에서 전역 예외 핸들러가 필요하면 `testImplementation project(':support')`를 사용한다.

## Feature Module Support Packages

기능 모듈 내부의 `support` 패키지는 전역 `support` 모듈과 다르다.
`auth.support.error`, `board.support.openapi`처럼 해당 모듈 안에서만 쓰이는 보조 코드임을 드러내기 위한 module-local namespace다.
도메인 기능 패키지(`post`, `comment`, `login`, `document`)와 같은 레벨에 `error`, `openapi`, `port`가 흩어지면 기능처럼 보이므로 다음 기준을 따른다.

- 모듈 전체에서 공유하는 error code와 feature OpenAPI group 설정은 `<module>.support.error`, `<module>.support.openapi`에 둔다.
- 특정 도메인 use case를 구현하는 adapter는 `<module>.<domain>.adapter` 또는 기존 service 패키지에 둔다.
- 다른 모듈도 알아야 하는 port/interface/metadata 계약은 `core`에 둔다.
- 기능 도메인의 controller/service/entity/repository/dto는 계속 `<module>.<domain>.<role>` 구조를 따른다.

## Boundary Notes

- `UserPrincipal`은 `core`의 공통 계약이므로 `dev.iamrat.global.security.UserPrincipal`에 둔다.
- `OpenApiSecurityPolicy`는 runtime 보안 구현이 아니라 문서화 metadata 계약이므로 `core`에 둔다.
- Redis/OpenAPI/global web exception handler는 `support`에 둔다.
- `board`는 인증 구현 모듈인 `auth`를 의존하지 않는다. 컨트롤러는 공통 principal 계약만 참조한다.
- `ingest`는 AI 구현 모듈인 `ai`를 의존하지 않는다. 자동 게시 요청은 `core`의 `NewsAnalysisPostPublisher` port를 통해 전달하고, `ai`가 그 port를 구현한다.
- `board`의 `PostWriter` 구현체는 `board.post.adapter`에 둔다. `board.port.*`처럼 구현체를 별도 루트 port 패키지에 모으지 않는다.
- `collector`와 `ingest` 사이의 뉴스 자동 게시 metadata는 `core`의 `NewsDocumentMetadata`로만 해석한다. 기능 module에서 `autoPostEligible`, `newsTitle`, `originalLink` 같은 raw key를 반복 정의하지 않는다.
- `messaging`은 도메인 의미를 해석하지 않고 outbox envelope 저장/relay/retry만 소유한다. 현재는 독립 infrastructure module로 두고, 기능 module이 직접 의존하는 연결은 별도 phase에서 결정한다.
- `ingest`는 PgVector 구현을 직접 알 필요가 없고, `VectorStore` API만 사용한다. 현재 단일 runtime에서는 실제 `VectorStore` bean을 `ai` 모듈의 설정이 만든다.
- `ai`와 `ingest`의 Spring AI BOM은 루트 `springAiVersion`으로 통일한다.
- `ops`는 local-only 운영 도구를 담는다. 제품 기능 도메인이나 인증/게시판/AI 비즈니스 규칙은 `ops`로 들어오지 않는다.
- `app`의 route/security 조립은 `PostForgeSecurityRoutes`와 `PostForgeOpenApiRoutes`를 기준으로 하고, OpenAPI security requirement는 package prefix가 아니라 `@PreAuthorize` 또는 `OpenApiSecurityPolicy`로 결정한다.

## Validation

의존성 정리 후 최소 검증은 다음 순서로 수행한다.

```bash
bash ./gradlew :core:compileJava :support:compileJava :auth:compileJava :board:compileJava :ai:compileJava :ingest:compileJava :collector:compileJava :messaging:compileJava :ops:compileJava :app:compileJava
bash ./gradlew :core:test :support:test :auth:test :board:test :ai:test :ingest:test :collector:test :messaging:test :ops:test :app:test :app:bootJar
```

특정 전이가 다시 생겼는지 확인할 때는 다음 명령을 사용한다.

```bash
bash ./gradlew :ai:dependencyInsight --configuration compileClasspath --dependency spring-boot-starter-data-jpa
bash ./gradlew :ingest:dependencyInsight --configuration compileClasspath --dependency spring-boot-starter-data-jpa
bash ./gradlew :ingest:dependencies --configuration compileClasspath
bash ./gradlew :core:dependencyInsight --configuration compileClasspath --dependency springdoc-openapi-starter-webmvc-ui
bash ./gradlew :core:dependencyInsight --configuration compileClasspath --dependency spring-boot-starter-data-redis
bash ./gradlew :app:dependencyInsight --configuration runtimeClasspath --dependency spring-ai-pgvector-store
```
