# Module Dependency Policy

이 문서는 두 가지 질문에 답한다.

1. 각 모듈은 어느 모듈을 참조할 수 있는가?
2. 새 코드와 의존성은 어느 모듈에 두는가?

개별 `build.gradle`의 상세 근거는 [Gradle Dependency Rationale](./gradle-dependency-rationale.md), modular monolith 선택 근거는 [ADR-003](../decisions/adr-003-modular-monolith.md), DB/table 소유권은 [DB Schema Ownership](../database/schema-ownership.md)을 참고한다.

## 허용 의존성

아래 표가 모듈 간 직접 의존성의 기준이다. `A → B`는 A의 코드가 B의 타입을 참조할 수 있다는 뜻이며, 반대 방향은 별도로 적혀 있지 않으면 허용하지 않는다.

| 모듈 | 직접 참조할 수 있는 모듈 |
|------|--------------------------|
| `app` | `core`, `support`, `auth`, `board`, `source`, `ingest`, `catalog`, `price`, `ai`, `messaging` |
| `core` | 없음 |
| `support` | `core` |
| `auth` | `core`, `support` |
| `board` | `core`, `support` |
| `source` | 없음 |
| `ingest` | `core`, `source`, `catalog`, `price` |
| `catalog` | `core` |
| `price` | `core`, `source`, `catalog` |
| `ai` | `core`, `catalog` |
| `messaging` | `core` |

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
| 특정 기능의 비즈니스 규칙이나 구현인가? | `auth`, `board`, `source`, `ingest`, `catalog`, `price`, `ai` |
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
| `source` | 외부 상품·뉴스 source adapter와 `MOCK`/`NAVER` routing contract |
| `ingest` | 문서 적재, 상품·뉴스 수집 orchestration, tracked keyword, collection job, raw product |
| `catalog` | 상품 조회 모델, source별 offer, 카테고리, product embedding, 유사 상품 후보 |
| `price` | 가격 스냅샷 이력, 가격 이력 조회, response-only 가격 판정 |
| `ai` | AI 채팅, 내부 출시뉴스 문안 생성, product embedding adapter, OpenAI/PgVector 설정 |
| `messaging` | outbox 저장, optional relay/MQ adapter 경계 |

## Gradle 의존성 규칙

- 프로젝트 모듈 의존성은 위의 허용 의존성 표와 일치시킨다.
- 외부 라이브러리는 `implementation`을 기본값으로 사용한다.
- public method/class/record signature에 노출되는 타입만 `api`로 선언한다.
- 기능 모듈은 자신이 직접 사용하는 기술 의존성을 직접 선언한다.
- 공통 버전은 가능한 루트 `build.gradle`의 `ext`에서 관리한다.
- `app`은 다른 모듈이 소비하는 라이브러리가 아니므로 모든 프로젝트 의존성을 `implementation`으로 둔다.

외부 dependency의 configuration 선택과 예외 근거는 [Gradle Dependency Rationale](./gradle-dependency-rationale.md)에 둔다.

## 주요 연결 경계

| 경계 | 규칙 |
|------|------|
| `board` ↔ 인증 | `board`는 `auth` 구현 대신 `core`의 principal 계약만 참조한다. 상세 인증 경계는 [Authentication Architecture](./authentication.md)에 둔다. |
| `ingest` ↔ AI | `ingest`는 `ai` 대신 `VectorStore` API와 `core`의 초안 생성 port를 사용한다. 실제 PgVector bean은 `ai`가 만든다. |
| `ingest` ↔ 상품 수집 | `source`, `catalog`, `price`의 application 경계를 순서대로 호출한다. |
| `price` ↔ 상품 판정 | 상품 샘플은 `source` 계약으로 읽고, 결과를 저장하거나 게시글을 만들지 않는다. 현재는 대체 외부 소스가 없어 `503`을 반환한다. |
| `source` ↔ `ingest` | source 선택은 `SourceType`과 `SourceRequestExecutor` 계약으로 전달한다. |
| `messaging` ↔ 도메인 | 도메인 의미를 해석하지 않고 outbox 저장·relay·retry·dispatch만 담당한다. |
| `app` ↔ 실행 정책 | route/security/OpenAPI와 전체 runtime 조립만 담당한다. |

세부 비즈니스 규칙은 [AI Cost Policy](../policy/ai-cost-policy.md), [통합 API 명세의 Ingest](../api/README.md#ingest), [통합 API 명세의 Price](../api/README.md#price), [Use Case Data Policy](../policy/usecase-data-policy.md)에 둔다. 이 문서에는 모듈 경계만 남긴다.

## 검증

모듈 경계만 빠르게 확인한다.

```bash
bash ./gradlew :app:test --tests dev.iamrat.app.architecture.ModuleBoundaryTest
```

전체 의존성 변경 후에는 전체 테스트를 실행한다.

```bash
bash ./gradlew test
```
