# Gradle Dependency Rationale

이 문서는 PostForge의 각 `build.gradle`이 왜 현재 형태로 구성되어 있는지 설명한다.
목표는 의존성을 "빌드가 되게 아무 데나 추가"하는 것이 아니라, modular monolith 경계와 MSA 전환 후보 경계를 Gradle dependency로도 드러내는 것이다.

## Dependency Keywords

Gradle 의존성 키워드는 모듈 경계를 표현하는 언어다.

| 키워드 | 의미 | PostForge 기준 사용 원칙 |
|--------|------|--------------------------|
| `api` | 이 모듈을 사용하는 다른 모듈의 compile classpath에도 노출된다. | public method/record/class signature에 노출되는 타입에만 사용한다. |
| `implementation` | 이 모듈 내부 구현에만 필요하다. 소비 모듈의 compile classpath에는 숨긴다. | 기본값이다. 대부분의 framework/library는 여기에 둔다. |
| `runtimeOnly` | compile에는 필요 없고 실행 시점에만 필요하다. | JDBC driver, JWT Jackson codec처럼 런타임에 로딩되는 구현체에 사용한다. |
| `compileOnly` | compile에는 필요하지만 artifact/runtime에 포함하지 않는다. | servlet container처럼 런타임 환경이 제공하는 API에 사용한다. |
| `testImplementation` | test source set에서만 필요하다. | slice test, mock/test helper, H2, security-test 등에 사용한다. |
| `platform` | BOM을 가져와 dependency version set을 맞춘다. | Spring Boot, Spring AI, AWS SDK처럼 연동 라이브러리 버전 정합성이 중요한 묶음에 사용한다. |

실무 기준으로는 `implementation`이 기본이고, `api`는 public contract에 노출된다는 증거가 있을 때만 쓴다.
`api`를 남발하면 모듈 내부 구현이 다른 모듈 compile classpath로 새어 나가고, 나중에 분리할 때 숨겨진 결합이 된다.

## 문서 경계

허용 프로젝트 의존성과 모듈 책임은 [Module Dependency Policy](./module-dependencies.md)가 유일한 기준이다.
실제 선언의 기준은 각 모듈의 `build.gradle`이며, 이 문서는 모든 dependency를 복사하지 않고 선택 이유만 기록한다.

## 공통 빌드 정책

- Spring Boot plugin version은 루트에서 고정하되 executable module만 적용한다.
- Java toolchain, repository, BOM, compiler option, test task 같은 공통 정책은 루트에서 관리한다.
- 기능 모듈은 `java-library`로 유지해 `api`와 `implementation` 경계를 드러낸다.
- Lombok은 compile-time 도구이므로 `compileOnly`와 `annotationProcessor`로 둔다.

## 비자명한 선언

| 선언 | 이유 |
|------|------|
| `app`의 project dependency는 `implementation` | `app`은 다른 모듈이 소비하는 library가 아니라 최종 실행물이다. |
| `core`의 `spring-web`은 `api` | `ErrorCode`가 `HttpStatus`를 public signature에 노출한다. |
| `core`의 `spring-data-commons`는 `api` | `PageResponse.from(Page<T>)`가 `Page`를 public signature에 노출한다. |
| `core`의 `jackson-annotations`는 `api` | annotation이 public class metadata에 남는다. |
| PostgreSQL driver와 JJWT codec은 `runtimeOnly` | compile-time contract가 아니라 런타임 구현체다. |
| servlet API는 필요한 library에서 `compileOnly` | embedded container가 런타임에 제공한다. |
| Spring Boot, Spring AI, AWS SDK는 BOM 사용 | 함께 동작하는 라이브러리의 version set을 맞춘다. |
| test helper와 H2는 test configuration | production classpath에 포함할 이유가 없다. |

외부 기술은 그 기술을 직접 사용하는 모듈이 선언한다. 프로젝트 모듈 간 edge를 추가할 때는 먼저
[허용 의존성 표](./module-dependencies.md#허용-의존성)에 맞는지 확인한다.

## Review Checklist

새 dependency를 추가할 때는 다음 질문을 먼저 확인한다.

1. 이 module의 production code가 직접 import하는가?
2. public API signature에 노출되는가? 그렇다면 `api`, 아니면 `implementation`인가?
3. compile에 필요한가, runtime에만 필요한가?
4. test에서만 필요한가?
5. 이 외부 시스템은 어떤 도메인 module의 책임인가?
6. 이 dependency를 `core`나 `support`에 넣으면 다른 module까지 불필요하게 오염시키지 않는가?
7. MSA로 분리하면 이 dependency는 어느 service에 남아야 하는가?

원칙은 단순하다. 의존성은 쓰는 곳에 두고, 최종 배치는 [Module Dependency Policy](./module-dependencies.md#코드-배치-기준)로 판단한다.
