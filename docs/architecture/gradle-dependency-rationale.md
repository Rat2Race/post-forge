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

## Root `build.gradle`

루트 `build.gradle`은 개별 기능을 구현하지 않고, 모든 module에 공통으로 적용되는 build policy를 정의한다.

| 설정 | 이유 |
|------|------|
| `org.springframework.boot` plugin `apply false` | Boot plugin version은 루트에서 고정하지만, 실제 executable app인 `app`만 Boot plugin을 적용한다. library module까지 Boot app처럼 만들지 않기 위해서다. |
| `ext` version constants | SpringDoc, Spring AI, AWS SDK, JJWT처럼 여러 module에서 쓸 수 있는 버전을 한 곳에서 관리해 version drift를 막는다. |
| `allprojects` group/version/repositories | 모든 module artifact identity와 repository policy를 통일한다. |
| Java 21 toolchain | 개발자 로컬 JDK 차이에 상관없이 Java 21 기준으로 compile한다. |
| Spring Boot dependency BOM | Spring Boot와 맞는 Spring/Jackson/Micrometer 등 transitive dependency version을 통일한다. |
| Lombok `compileOnly`/`annotationProcessor` | Lombok은 compile-time code generation 도구이므로 runtime dependency로 넣지 않는다. |
| `-parameters` compiler arg | Spring MVC, validation, reflection 기반 binding에서 method parameter name을 보존한다. |
| JUnit Platform + `excludeTags` | unit/webmvc/integration test를 Gradle property로 선택 실행할 수 있게 한다. |
| `JavaLibraryPlugin` sources/javadoc jar | `java-library` module은 나중에 내부 library처럼 배포/참조될 수 있으므로 source/javadoc artifact를 생성한다. |

중요한 원칙은 "Spring Boot plugin은 실행 모듈에만 적용하고, 기능 모듈은 library로 둔다"는 점이다.
이렇게 해야 `auth`, `board`, `ai`, `ingest`가 각각 독립 테스트 가능한 library module로 남는다.

## `app/build.gradle`

`app`은 composition root다.
비즈니스 기능을 직접 소유하지 않고, 실행 가능한 Spring Boot 애플리케이션으로 여러 기능 module을 조립한다.

| 의존성/설정 | 이유 |
|-------------|------|
| `id "java"` + `id "org.springframework.boot"` | 최종 실행 artifact인 `bootJar`를 만드는 유일한 module이다. |
| `bootJar.mainClass` | 실행 진입점 `ApplicationServer`를 명시한다. |
| `implementation project(':core')` | 실행 app도 공통 계약/예외 타입을 runtime graph에 포함해야 한다. |
| `implementation project(':support')` | Redis/OpenAPI/global exception handler 같은 Spring infrastructure bean을 최종 app에 조립한다. |
| `implementation project(':auth')`, `:board`, `:ai`, `:ingest` | 기능 module을 하나의 monolith runtime에 조립한다. `app`이 module composition을 담당한다는 뜻이다. |
| `spring-boot-starter-web` | 최종 app이 HTTP server와 Spring MVC runtime을 실행한다. |
| `spring-boot-starter-actuator` | health/metrics endpoint를 제공한다. |
| `spring-boot-starter-data-jpa` | `ApplicationServer`가 `@EntityScan`, `@EnableJpaRepositories`로 JPA bootstrap을 담당한다. |
| `micrometer-registry-prometheus` | Actuator metric을 Prometheus format으로 export한다. |
| `runtimeOnly postgresql` | compile에는 JDBC driver type이 필요 없고, 실행 시점에 PostgreSQL driver가 필요하다. |
| `testImplementation spring-boot-starter-test` | app context smoke/spring test를 위한 기본 test stack이다. |

`app`의 project dependency는 `api`가 아니라 `implementation`이다.
`app`은 다른 module이 compile-time으로 소비하는 library가 아니라 최종 실행물이다.
따라서 `app`이 조립한 내부 module 의존성을 외부에 노출할 이유가 없다.

## `core/build.gradle`

`core`는 middleware가 아니라 shared kernel / contract module이다.
직접 실행되지 않고, 모듈 간 공통 DTO, 예외 타입, principal 계약, port 계약을 제공한다.

| 의존성 | 이유 |
|--------|------|
| `java-library` plugin | `api`/`implementation` 경계를 표현해야 하는 contract module이므로 `java-library`가 맞다. |
| `api org.springframework:spring-web` | `ErrorCode`가 `HttpStatus`를 public getter로 노출한다. 소비 module도 그 타입을 compile할 수 있어야 한다. |
| `api org.springframework.data:spring-data-commons` | `PageResponse.from(Page<T>)`가 `Page`를 public method parameter로 노출한다. |
| `api com.fasterxml.jackson.core:jackson-annotations` | `ErrorResponse`의 Jackson annotation이 public class metadata로 쓰인다. |

`core`에 넣으면 안 되는 것은 다음이다.

- Redis, JPA starter, Spring Security 구현체
- OpenAPI/SpringDoc 설정
- S3/OpenAI/Naver/Gmail 같은 외부 adapter
- 기능 module의 service/repository/entity 구현

`core`가 무거워지면 모든 module이 그 무게를 같이 끌고 간다.
MSA 전환 관점에서는 `core`가 "계약만 담은 얇은 module"이어야 나중에 HTTP schema나 message schema로 치환하기 쉽다.

## `support/build.gradle`

`support`는 공통 Spring infrastructure module이다.
도메인 로직이나 외부 business adapter를 소유하지 않고, 여러 기능 module에 섞이면 중복되거나 오염되기 쉬운 Spring 설정을 모은다.

| 의존성 | 이유 |
|--------|------|
| `implementation project(':core')` | `GlobalExceptionHandler`가 `ErrorCode`, `ErrorResponse`, `CustomException`을 사용한다. |
| `spring-webmvc` | `@RestControllerAdvice`, MVC exception type, `HandlerMethod` 기반 OpenAPI customizer에 필요하다. |
| `spring-boot-starter-data-redis` | 공통 `RedisTemplate<String, String>` bean 설정을 제공한다. |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI metadata, group, security scheme 설정을 제공한다. |
| `spring-security-core` | `@PreAuthorize` annotation을 읽어 OpenAPI security requirement를 붙인다. |
| `compileOnly jakarta.servlet-api` | servlet API는 보통 embedded container/runtime이 제공하므로 support artifact에 implementation으로 묶지 않는다. |
| `testImplementation spring-boot-starter-test` | infrastructure 설정과 handler unit/slice test에 필요하다. |
| `testRuntimeOnly jakarta.servlet-api` | support 단독 test runtime에서 servlet type resolution이 필요할 수 있어 test runtime에만 둔다. |

중요한 점은 `support`가 외부 API 연동 module이 아니라는 것이다.
S3는 `board`, OpenAI는 `ai`, OAuth/Gmail은 `auth`, Naver API는 `crawl`이 소유한다.
`support`는 Spring infrastructure glue만 맡는다.

## `auth/build.gradle`

`auth`는 인증/인가 도메인을 소유한다.
회원, 로그인, JWT, OAuth2, 이메일 인증, refresh token state가 이 module의 책임이다.

| 의존성 | 이유 |
|--------|------|
| `implementation project(':core')` | 공통 예외, principal 계약, 응답 타입을 사용한다. |
| `spring-boot-starter-web` | 인증/프로필/토큰 controller를 제공한다. |
| `spring-boot-starter-validation` | request DTO validation에 필요하다. |
| `spring-boot-starter-data-jpa` | `Member` 등 인증 도메인 entity/repository를 소유한다. |
| `spring-boot-starter-data-redis` | refresh token, email verification token/state를 Redis에 저장한다. |
| `spring-boot-starter-security` | `SecurityFilterChain`, authentication filter/provider, 권한 처리를 소유한다. |
| `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | JWT 생성/파싱에 필요하다. Jackson codec은 runtime implementation이므로 `runtimeOnly`다. |
| `spring-boot-starter-mail` | Gmail SMTP 기반 이메일 인증 발송에 필요하다. |
| `spring-boot-starter-oauth2-client` | Google/Naver/Kakao OAuth2 login/code exchange 흐름에 필요하다. |
| `testImplementation project(':support')` | MVC slice test에서 global exception handler 등 공통 web infrastructure를 가져온다. |
| `spring-security-test` | 인증 principal, 권한, security context test helper에 필요하다. |
| `H2` test dependency | JPA repository/service test에서 가벼운 in-memory DB를 사용한다. |

인증 구현은 다른 기능 module로 새어 나가면 안 된다.
예를 들어 `board`는 `auth`의 `CustomUserDetails`를 직접 의존하지 않고 `core`의 `UserPrincipal` 계약만 본다.

## `board/build.gradle`

`board`는 게시글, 댓글, 좋아요, 파일/S3 도메인을 소유한다.

| 의존성 | 이유 |
|--------|------|
| `implementation project(':core')` | `UserPrincipal`, 공통 예외, `PostWriter`/`PostReader` 등 board port 계약을 사용/구현한다. |
| `spring-boot-starter-web` | 게시글/댓글/파일 controller를 제공한다. |
| `spring-boot-starter-validation` | 게시글/댓글 request DTO validation에 필요하다. |
| `spring-boot-starter-data-jpa` | post/comment/like/file entity와 repository를 소유한다. |
| `spring-boot-starter-data-redis` | 조회수 중복 방지, view count sync 등 Redis 기반 상태에 필요하다. |
| `spring-boot-starter-security` | `@PreAuthorize`, `@AuthenticationPrincipal`, method security 표현식에 필요하다. |
| AWS SDK BOM + `software.amazon.awssdk:s3` | S3 presigned URL과 object metadata 연동은 board 파일 도메인의 외부 adapter다. |
| `testImplementation project(':support')` | controller/slice test에서 global exception handler를 사용한다. |
| `spring-security-test` | 인증 principal과 권한 검증 test에 필요하다. |
| `testRuntimeOnly H2` | JPA test runtime DB로 사용한다. |

S3를 `support`나 `app`에 두지 않은 이유는 파일 업로드/다운로드가 board 도메인의 use case이기 때문이다.
외부 adapter는 "그 외부 시스템을 실제로 쓰는 도메인 module"이 소유해야 한다.

## `ai/build.gradle`

`ai`는 AI 채팅, AI 게시글 생성, OpenAI/PgVector 설정을 소유한다.

| 의존성 | 이유 |
|--------|------|
| `implementation project(':core')` | `NewsAnalysisPostPublisher`, `PostWriter`, 공통 예외/계약을 사용한다. |
| `spring-boot-starter-web` | `/ai/chat` 등 AI API controller를 제공한다. |
| `spring-boot-starter-jdbc` | Spring AI PgVector store가 `JdbcTemplate` 기반으로 동작한다. |
| Spring AI BOM | `spring-ai-openai`, `spring-ai-pgvector-store`, vector 관련 transitive version을 맞춘다. |
| `spring-ai-openai` | OpenAI chat/embedding client를 사용한다. |
| `spring-ai-pgvector-store` | PgVector 기반 vector store bean을 제공한다. |
| `spring-boot-starter-validation` | AI request DTO validation에 필요하다. |
| `testImplementation project(':support')` | AI controller slice test에서 공통 exception handler를 사용한다. |

`ai`가 `spring-boot-starter-data-jpa`를 갖지 않는 이유는 JPA entity/repository를 소유하지 않기 때문이다.
AI는 RAG/vector store와 LLM integration이 핵심이므로 JDBC + Spring AI PgVector가 맞다.

## `ingest/build.gradle`

`ingest`는 문서 적재 API와 internal crawl ingest orchestration을 소유한다.
중요한 설계 포인트는 `ingest`가 `ai` 구현 module을 직접 의존하지 않는다는 것이다.

| 의존성 | 이유 |
|--------|------|
| `implementation project(':core')` | `NewsAnalysisPostPublisher` port와 공통 예외/계약을 사용한다. |
| `spring-boot-starter-web` | `/ai/documents`, `/internal/crawl/documents` API controller를 제공한다. |
| `spring-tx` | 문서 저장 use case의 transaction boundary를 표현한다. |
| Spring AI BOM | `spring-ai-vector-store`와 Spring AI transitive version을 맞춘다. |
| `spring-ai-vector-store` | `VectorStore` interface만 사용한다. PgVector 구현체는 직접 알지 않는다. |
| `spring-boot-starter-validation` | ingest request DTO validation에 필요하다. |
| `testImplementation project(':support')` | internal controller test에서 global exception handler를 사용한다. |

`ingest`에서 일부러 뺀 것은 다음이다.

- `project(':ai')`: 자동 게시 요청은 `NewsAnalysisPostPublisher` port로만 전달한다.
- `spring-ai-pgvector-store`: PgVector 구현은 현재 monolith runtime에서 `ai` module이 제공한다.
- `spring-boot-starter-data-jpa`: ingest는 JPA entity/repository를 소유하지 않는다.

이 구조 덕분에 `ingest`는 module 단독 test에서 AI 구현 없이도 검증된다.
MSA 전환 시에는 `NewsAnalysisPostPublisher` 구현을 Java bean에서 HTTP adapter 또는 message adapter로 바꾸면 된다.

## Review Checklist

새 dependency를 추가할 때는 다음 질문을 먼저 확인한다.

1. 이 module의 production code가 직접 import하는가?
2. public API signature에 노출되는가? 그렇다면 `api`, 아니면 `implementation`인가?
3. compile에 필요한가, runtime에만 필요한가?
4. test에서만 필요한가?
5. 이 외부 시스템은 어떤 도메인 module의 책임인가?
6. 이 dependency를 `core`나 `support`에 넣으면 다른 module까지 불필요하게 오염시키지 않는가?
7. MSA로 분리하면 이 dependency는 어느 service에 남아야 하는가?

원칙은 단순하다.
의존성은 "쓰는 곳"에 둔다.
계약은 `core`, Spring infrastructure는 `support`, 실행 조립은 `app`, 외부 adapter는 해당 feature module이 소유한다.
