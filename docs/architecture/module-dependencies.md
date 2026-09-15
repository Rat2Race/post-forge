# Module Dependency Policy

이 문서는 두 가지 질문에 답한다.

1. 각 모듈은 어느 모듈을 참조할 수 있는가?
2. 새 코드와 의존성은 어느 모듈에 두는가?

modular monolith 선택 근거는 [ADR-003](../decisions/adr-003-modular-monolith.md), DB/table 소유권은 [DB Schema Ownership](../database/schema-ownership.md)을 참고한다.

## 허용 의존성

아래 표가 모듈 간 직접 의존성의 기준이다. `A → B`는 A의 코드가 B의 타입을 참조할 수 있다는 뜻이며, 반대 방향은 별도로 적혀 있지 않으면 허용하지 않는다.

| 모듈 | 직접 참조할 수 있는 모듈 |
|------|--------------------------|
| `app` | `core`, `support`, `auth`, `board`, `source`, `ingest`, `ai` |
| `core` | 없음 |
| `support` | `core` |
| `auth` | `core`, `support` |
| `board` | `core`, `support` |
| `source` | 없음 |
| `ingest` | `core`, `source` |
| `ai` | `core` |

이 규칙은 [ModuleBoundaryTest](../../app/src/test/java/dev/iamrat/app/architecture/ModuleBoundaryTest.java)가 검사한다. 특히 다음 역방향 의존은 만들지 않는다.

- 기능 모듈 → `app`
- `board` → `auth`
- `ingest` → `ai`
- `core` → 다른 프로젝트 모듈

## 코드 배치 기준

새 코드는 다음 순서로 위치를 결정한다.

| 질문 | 위치 |
|------|------|
| 여러 모듈이 컴파일할 때 알아야 하는 계약인가? | `core` |
| Spring 실행 시 공통으로 등록할 bean/config/advice인가? | `support` |
| 특정 기능의 비즈니스 규칙이나 구현인가? | `auth`, `board`, `source`, `ingest`, `ai` |
| 여러 기능 모듈을 최종 실행 형태로 조립하는가? | `app` |
| 특정 외부 시스템을 실제로 사용하는가? | 그 기능을 소유한 모듈의 adapter |
| 로컬 운영·테스트만을 위한 도구인가? | 별도 모듈 또는 외부 스크립트 |

### `core`와 `support`

둘 다 여러 모듈에서 사용되지만 역할은 다르다.

| 구분 | `core` | `support` |
|------|--------|-----------|
| 역할 | 모듈 간 계약 | 공통 Spring 인프라 구현 |
| 들어가는 것 | interface, record, 공통 DTO·예외·principal | `@Configuration`, `@Bean`, 공통 web/redis/persistence helper |
| 들어가면 안 되는 것 | Redis/OpenAPI/S3/OpenAI/JPA 구현, feature service | 도메인 계약·규칙, feature service |
| 참조 방식 | 기능 모듈이 컴파일 시 참조 | `app`이 조립하고 필요한 기능 모듈만 참조 |

예를 들어 `PostWriter`와 `SourceDocumentCommand`는 모듈 간 약속이므로 `core`에 둔다. Redis TTL primitive와 MVC 예외 응답 변환은 공통 Spring 구현이므로 `support`에 둔다.

기능 모듈 내부의 `auth.support.error`, `board.support.error` 같은 패키지는 전역 `support` 모듈과 무관한 module-local namespace다. 다른 모듈도 알아야 하는 계약은 이 패키지에 두지 않고 `core`로 올린다.

## 모듈 책임

| 모듈 | 책임 |
|------|------|
| `app` | 실행 진입점, 전체 모듈 조립, JPA scan bootstrap, route/security/OpenAPI 정책 |
| `core` | 공통 DTO·예외·principal/API metadata와 모듈 간 port 계약 |
| `support` | Redis guard primitive, JPA auditing, request logging, MVC 예외 응답 |
| `auth` | 계정, 로그인, JWT, OAuth2, 이메일 인증, 인증/인가 오류 응답 |
| `board` | 게시글, 댓글, 좋아요, 파일/S3, 조회수 |
| `source` | 외부 뉴스 source adapter |
| `ingest` | 문서 적재, 뉴스 수집·분야별 선별, tracked keyword, 뉴스 스케줄 게시, 전날 뉴스의 데일리 종합 게시 |
| `ai` | AI 채팅(RAG), 뉴스·데일리 포스트 초안 생성, LLM/PgVector 설정 |

## Gradle 의존성 규칙

목표는 의존성을 "빌드가 되게 아무 데나 추가"하는 것이 아니라, modular monolith 경계와 MSA 전환 후보 경계를 Gradle dependency로도 드러내는 것이다.

- 프로젝트 모듈 의존성은 위의 허용 의존성 표와 일치시킨다.
- 외부 라이브러리는 `implementation`을 기본값으로 사용한다.
- public method/class/record signature에 노출되는 타입만 `api`로 선언한다.
- 기능 모듈은 자신이 직접 사용하는 기술 의존성을 직접 선언한다.
- 공통 버전은 가능한 루트 `build.gradle`의 `ext`에서 관리한다.
- `app`은 다른 모듈이 소비하는 라이브러리가 아니므로 모든 프로젝트 의존성을 `implementation`으로 둔다.

### 공통 빌드 정책

- Spring Boot plugin version은 루트에서 고정하되 executable module만 적용한다.
- Java toolchain, repository, BOM, compiler option, test task 같은 공통 정책은 루트에서 관리한다.
- 기능 모듈은 `java-library`로 유지해 `api`와 `implementation` 경계를 드러낸다(`api`를 남발하면 내부 구현이 소비 모듈 compile classpath로 새어 나가 분리 시 숨은 결합이 된다).
- Lombok은 compile-time 도구이므로 `compileOnly`와 `annotationProcessor`로 둔다.

### 비자명한 선언

| 선언 | 이유 |
|------|------|
| `core`의 `spring-web`은 `api` | `ErrorCode`가 `HttpStatus`를 public signature에 노출한다. |
| `core`의 `spring-data-commons`는 `api` | `PageResponse.from(Page<T>)`가 `Page`를 public signature에 노출한다. |
| `core`의 `jackson-annotations`는 `api` | annotation이 public class metadata에 남는다. |
| PostgreSQL driver와 JJWT codec은 `runtimeOnly` | compile-time contract가 아니라 런타임 구현체다. |
| servlet API는 필요한 library에서 `compileOnly` | embedded container가 런타임에 제공한다. |
| Spring Boot, Spring AI, AWS SDK는 BOM 사용 | 함께 동작하는 라이브러리의 version set을 맞춘다. |
| test helper(slice test, spring-security-test)와 H2는 test configuration | production classpath에 포함할 이유가 없다. |

## 주요 연결 경계

| 경계 | 규칙 |
|------|------|
| `board` ↔ 인증 | `board`는 `auth` 구현 대신 `core`의 principal 계약만 참조한다. 상세 인증 경계는 [Authentication Architecture](./authentication.md)에 둔다. |
| `ingest` ↔ AI | `ingest`는 `ai` 대신 `VectorStore` API와 `core`의 초안 생성 port를 사용한다. 실제 PgVector bean은 `ai`가 만든다. |
| `ingest` ↔ 뉴스 수집 | `source`의 `NewsSourceClient` 경계를 호출하고, 결과를 문서 적재와 출시 뉴스 게시로 넘긴다. |
| `app` ↔ 실행 정책 | route/security/OpenAPI와 전체 runtime 조립만 담당한다. |

세부 비즈니스 규칙은 [AI Cost Policy](../policy.md#ai-cost), [통합 API 명세의 Ingest](../api/README.md#ingest), [Use Case Data Policy](../policy.md)에 둔다. 이 문서에는 모듈 경계만 남긴다.

## 검증

모듈 경계만 빠르게 확인한다.

```bash
bash ./gradlew :app:test --tests dev.iamrat.app.architecture.ModuleBoundaryTest
```

전체 의존성 변경 후에는 전체 테스트를 실행한다.

```bash
bash ./gradlew test
```
