# 게시판 1단계 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 7개 Gradle 모듈을 `app` 하나로 합치고, 가입·로그인·글·댓글·좋아요·조회수만 있는 CRUD 게시판을 가장 단순한 구조로 다시 짠 뒤 부하 측정 준비까지 마친다.

**Architecture:** `dev.iamrat.postforge` 아래 기능 패키지(`auth`, `post`, `comment`, `like`, `common`)를 한 층으로 두고, 요청은 Controller → Service → Repository로만 흐른다. 조회수와 좋아요 수는 SQL 한 문장(`UPDATE ... SET x = x + 1`, `INSERT ... ON CONFLICT DO NOTHING`)으로 바꾸고, 동시성은 Testcontainers PostgreSQL에서 검증한다. 이 핵심 줄은 사용자가 직접 쓴다(Task 6).

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Spring Security, jjwt 0.12.7, Flyway, PostgreSQL 18(pgvector 이미지), Testcontainers, k6

**Spec:** [docs/superpowers/specs/2026-10-18-board-restructure-design.md](../specs/2026-10-18-board-restructure-design.md)

## Global Constraints

- 브랜치는 `release/postforge`. **push하지 않는다.** 이미지 빌드·배포는 사용자 몫이다.
- 패키지 루트 `dev.iamrat.postforge`. 기능 패키지 안에 하위 패키지를 두지 않는다. `*Store`, `*PersistenceAdapter`, `Abstract*Service`를 만들지 않는다.
- 새 라이브러리는 `spring-boot-starter-security`와 `io.jsonwebtoken:jjwt-*:0.12.7`(둘 다 기존에 쓰던 것)만 쓴다.
- 오류 응답은 모두 `{"code": "...", "message": "..."}`.
- access token 하나만 쓴다. 만료 기본 12시간(`JWT_ACCESS_TOKEN_VALIDITY`, 기본값 `12h`).
- 테스트는 Testcontainers(`jdbc:tc:pgvector:0.8.2-pg18-trixie:///postforge`)로 돈다. **Docker가 켜져 있어야 한다.**
- 커밋 메시지: 한국어 제목 `type(scope): 요약`, 빈 줄, `- `로 시작하는 본문(한 줄에 변경점 하나, 명사형, 마침표 없음). `Co-Authored-By` 줄을 붙이지 않는다.
- 동작을 바꾸는 커밋에서 해당 문서를 같이 고친다. API 명세는 엔드포인트를 만드는 Task에서 그 절을 채운다.
- Task 6의 핵심 줄(조회수 증가, 좋아요 "넣었을 때만 +1")은 사용자가 쓴다. 실행자는 Task 5까지 하고 멈춘다.

## 설계 문서와 다른 점 (계획에서 구체화한 것)

실행 전에 사용자가 확인한다.

1. **좋아요 엔티티를 두지 않는다.** `PostLike`·`CommentLike` 엔티티와 리포지토리 4개 대신 `LikeService`가 SQL을 직접 실행한다(`NamedParameterJdbcTemplate`). 좋아요는 INSERT·DELETE·카운터 UPDATE뿐이라 엔티티로 읽을 일이 없고, 판단이 담긴 SQL이 한곳에 보인다.
2. **응답 정리.** 목록 항목(`PostSummary`)에 본문과 `isLiked`를 싣지 않는다. 상세에서 `commentCount`, 댓글에서 `replyCount`를 뺀다(화면이 쓰지 않는다). 가입 응답은 `{accountId}`.
3. **목록은 `page`·`size`만 받는다.** 정렬은 고정이고 `sort`는 무시한다. 옛 코드에서 없는 속성으로 정렬하면 500이 나던 경로가 사라진다. 음수 `page`는 0, `size`는 1~100으로 맞춘다.
4. **카운터 칸은 JPA가 쓰지 않는다.** `views`·`like_count`를 `insertable = false, updatable = false`로 매핑해, 글을 고칠 때 읽어 둔 옛 값으로 카운터를 덮어쓰지 않게 한다.
5. **댓글 목록은 없는 글이면 빈 페이지다.** 글 존재 확인 쿼리를 생략한다. 쓰기(댓글 작성)는 404다.
6. **요청 로그 필터와 CORS를 지운다.** 요청마다 INFO 로그를 쓰면 부하 측정에 잡음이 되고, 화면은 같은 출처에서 나간다. 생성 시각의 시간대는 Dockerfile의 `-Duser.timezone=Asia/Seoul`로 맞춘다(옛 `ClockConfig` 대체).
7. **화면에 간단한 가입 폼을 넣는다.** 이메일 인증이 없어져 화면에서 가입할 수 있다. 태그 입력·표시를 지운다.
8. **옛 구조 문서를 지운다.** `docs/architecture/*`, `docs/database/*`, `docs/policy.md`, `docs/images/*`. 근거는 ADR-009에 남긴다. ADR, troubleshooting, performance/runs 같은 지난 기록은 둔다.
9. **제목·댓글의 HTML 금지 정규식을 뺀다.** 화면이 모든 출력을 `esc()`로 이스케이프한다.

## 파일 구조

```
settings.gradle                               수정: :app 하나만 include
build.gradle                                  삭제 (설정은 app/build.gradle로)
app/build.gradle                              다시 씀
app/src/main/java/dev/iamrat/postforge/
  PostForgeApplication.java                   Task 1
  common/ErrorCode.java                       Task 1  오류 코드(상태·메시지)
  common/BusinessException.java               Task 1
  common/ErrorResponse.java                   Task 1  {code, message}
  common/GlobalExceptionHandler.java          Task 1
  common/PageResponse.java                    Task 1
  auth/Account.java                           Task 2
  auth/AccountRepository.java                 Task 2
  auth/AccountService.java                    Task 2  가입·로그인·닉네임 조회
  auth/JwtProvider.java                       Task 2
  auth/JwtAuthenticationFilter.java           Task 2
  auth/SecurityConfig.java                    Task 2  API 체인 + actuator 체인
  auth/AuthController.java                    Task 2  요청·응답 record 포함
  post/Post.java                              Task 3
  post/PostRepository.java                    Task 3
  post/PostService.java                       Task 3, 5, 6
  post/PostController.java                    Task 3, 5  PostRequest 포함
  post/PostResponse.java                      Task 3  상세·작성·수정 응답
  post/PostSummary.java                       Task 3  목록 항목
  comment/Comment.java                        Task 4
  comment/CommentRepository.java              Task 4
  comment/CommentService.java                 Task 4, 5
  comment/CommentController.java              Task 4, 5  CommentRequest 포함
  comment/CommentResponse.java                Task 4
  like/LikeService.java                       Task 5, 6
  like/LikeResponse.java                      Task 5
app/src/main/resources/
  application.yml, application-local.yml      Task 1, 2
  db/migration/V1__init.sql                   Task 1
  static/auth.js, static/index.html           Task 7
app/src/test/java/dev/iamrat/postforge/
  ApplicationTest.java                        Task 1
  TestApi.java                                Task 2, 3, 4  API 테스트가 함께 쓰는 요청 묶음
  AuthApiTest.java                            Task 2
  PostApiTest.java                            Task 3
  CommentApiTest.java                         Task 4
  LikeApiTest.java                            Task 5
  CounterConcurrencyTest.java                 Task 5
app/src/test/resources/application-test.yml   Task 1, 2
perf/seed.sql, perf/k6/mixed.js, perf/k6/hot-post.js   Task 8
docs/decisions/adr-009-single-module-board.md          Task 1
```

---

### Task 1: 단일 모듈 골격과 문서 정리

**Files:**
- Delete: `ai/`, `auth/`, `board/`, `core/`, `study/`, `support/`, `scripts/`, `build.gradle`, `app/src/main/java/dev/iamrat/app/`, `app/src/test/java/dev/iamrat/app/`, `app/src/main/resources/application-monitoring.yml`, `app/src/main/resources/db/migration/V0000__baseline_schema.sql`, `app/src/main/resources/static/study.html`, `docs/architecture/`, `docs/database/`, `docs/policy.md`, `docs/images/`
- Modify: `settings.gradle`, `app/build.gradle`, `app/src/main/resources/application.yml`, `app/src/main/resources/application-local.yml`, `app/src/test/resources/application-test.yml`, `docker-compose.local.yml`, `Dockerfile.runtime`, `.env.example`, `CLAUDE.md`, `README.md`, `docs/README.md`, `docs/api/README.md`, `docs/performance/README.md`, `docs/decisions/adr-001-use-redis-for-view-count.md`, `docs/decisions/adr-002-refresh-token-rotation.md`, `docs/decisions/adr-003-modular-monolith.md`
- Create: `app/src/main/java/dev/iamrat/postforge/PostForgeApplication.java`, `common/ErrorCode.java`, `common/BusinessException.java`, `common/ErrorResponse.java`, `common/GlobalExceptionHandler.java`, `common/PageResponse.java`, `app/src/main/resources/db/migration/V1__init.sql`, `docs/decisions/adr-009-single-module-board.md`
- Test: `app/src/test/java/dev/iamrat/postforge/ApplicationTest.java`

**Interfaces:**
- Produces:
  - `enum ErrorCode { INVALID_INPUT, INVALID_COMMENT_PARENT, REPLY_DEPTH_EXCEEDED, UNAUTHORIZED, INVALID_CREDENTIALS, FORBIDDEN, NOT_FOUND, POST_NOT_FOUND, COMMENT_NOT_FOUND, METHOD_NOT_ALLOWED, DUPLICATE_USERNAME, DUPLICATE_NICKNAME, CONFLICT, UNSUPPORTED_MEDIA_TYPE, INTERNAL_ERROR }` — `HttpStatus getStatus()`, `String getMessage()`
  - `new BusinessException(ErrorCode)`
  - `record ErrorResponse(String code, String message)` — `static ErrorResponse of(ErrorCode)`
  - `record PageResponse<T>(...)` — `static <T> PageResponse<T> from(Page<T>)`
  - 테이블 `accounts`, `posts`, `comments`, `post_like`, `comment_like` (V1)

- [ ] **Step 1: 옛 모듈과 문서 삭제**

```bash
git rm -r -q ai auth board core support study scripts build.gradle \
  app/src/main/java/dev/iamrat/app app/src/test/java/dev/iamrat/app \
  app/src/main/resources/application-monitoring.yml \
  app/src/main/resources/db/migration/V0000__baseline_schema.sql \
  app/src/main/resources/static/study.html \
  docs/architecture docs/database docs/policy.md docs/images
rm -rf ai auth board core support study scripts
```

- [ ] **Step 2: Gradle 설정**

`settings.gradle` 전체:

```groovy
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}
rootProject.name = "postforge"
include(":app")
```

`app/build.gradle` 전체:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.14'
}

group = 'dev.iamrat'
version = '0.1.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    runtimeOnly 'org.postgresql:postgresql'

    compileOnly 'org.projectlombok:lombok:1.18.46'
    annotationProcessor 'org.projectlombok:lombok:1.18.46'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.testcontainers:postgresql'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test') {
    useJUnitPlatform()
}

tasks.named('bootJar') {
    archiveFileName = 'app.jar'
}

tasks.named('bootRun') {
    // 루트에서 실행해야 application-local.yml 의 optional:file:.env.local 이 잡힌다.
    workingDir = rootProject.projectDir
}
```

- [ ] **Step 3: 실패하는 테스트 작성**

`app/src/test/resources/application-test.yml` 전체:

```yaml
spring:
  # Testcontainers가 compose·운영과 같은 PostgreSQL 이미지를 띄운다. Flyway V1로 스키마를 만들고 ddl-auto=validate로 엔티티와 대조한다.
  datasource:
    url: jdbc:tc:pgvector:0.8.2-pg18-trixie:///postforge
    username: test
    password: test
```

`app/src/test/java/dev/iamrat/postforge/ApplicationTest.java`:

```java
package dev.iamrat.postforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreatesBoardTables() {
        List<String> tables = jdbc.queryForList("""
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
            ORDER BY table_name
            """, String.class);

        assertThat(tables).containsExactly("accounts", "comment_like", "comments", "post_like", "posts");
    }

    @Test
    void unknownPathReturnsJsonNotFound() throws Exception {
        mockMvc.perform(get("/nothing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
```

- [ ] **Step 4: 실패 확인**

Run: `./gradlew test --tests 'dev.iamrat.postforge.ApplicationTest'`
Expected: FAIL — `@SpringBootConfiguration`을 찾지 못한다(메인 클래스가 아직 없음).

- [ ] **Step 5: 메인 클래스와 common 패키지**

`app/src/main/java/dev/iamrat/postforge/PostForgeApplication.java`:

```java
package dev.iamrat.postforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostForgeApplication.class, args);
    }
}
```

`app/src/main/java/dev/iamrat/postforge/common/ErrorCode.java`:

```java
package dev.iamrat.postforge.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    INVALID_COMMENT_PARENT(HttpStatus.BAD_REQUEST, "같은 글의 댓글에만 답글을 달 수 있습니다"),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "답글에는 답글을 달 수 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌했습니다. 다시 시도해 주세요"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String message;
}
```

`app/src/main/java/dev/iamrat/postforge/common/BusinessException.java`:

```java
package dev.iamrat.postforge.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

`app/src/main/java/dev/iamrat/postforge/common/ErrorResponse.java`:

```java
package dev.iamrat.postforge.common;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
```

`app/src/main/java/dev/iamrat/postforge/common/GlobalExceptionHandler.java`:

```java
package dev.iamrat.postforge.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return respond(e.getErrorCode());
    }

    // 예외 메시지에는 거절된 값(비밀번호 등)이 실린다. 첫 번째 위반 필드의 검증 메시지만 돌려준다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(ErrorCode.INVALID_INPUT.name(), message));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return respond(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND);
    }

    // 둘 다 MVC 디스패치 중에 나서 아래 Exception 처리에 걸리면 500이 된다. 먼저 잡아 405·415로 답하고, 예외가 채운 Allow·Accept 헤더를 붙인다.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, e.getHeaders());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getHeaders());
    }

    // 앞선 중복 검사를 함께 통과한 동시 요청이 UNIQUE·FK 제약에 막힌 경우다. 메시지에 충돌한 값이 실리므로 원인 종류만 남긴다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException e) {
        log.warn("제약 조건 위반: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return respond(ErrorCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode) {
        return respond(errorCode, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, HttpHeaders headers) {
        return ResponseEntity.status(errorCode.getStatus()).headers(headers).body(ErrorResponse.of(errorCode));
    }
}
```

`app/src/main/java/dev/iamrat/postforge/common/PageResponse.java`:

```java
package dev.iamrat.postforge.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    int numberOfElements,
    boolean first,
    boolean last,
    boolean empty
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumberOfElements(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }
}
```

- [ ] **Step 6: 스키마와 설정**

`app/src/main/resources/db/migration/V1__init.sql`:

```sql
-- 게시판 스키마. 이력을 이 파일 하나로 새로 시작한다(ADR-009). 이후 변경은 V2부터 쌓는다.

CREATE TABLE accounts (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username   VARCHAR(20)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    CONSTRAINT uk_accounts_username UNIQUE (username),
    CONSTRAINT uk_accounts_nickname UNIQUE (nickname)
);

-- nickname은 작성 시점 값을 복사해 둔다. 닉네임을 바꾸는 기능이 없어 목록에서 accounts를 조인하지 않아도 된다.
CREATE TABLE posts (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account_id  BIGINT         NOT NULL REFERENCES accounts (id),
    nickname    VARCHAR(20)    NOT NULL,
    title       VARCHAR(100)   NOT NULL,
    content     VARCHAR(10000) NOT NULL,
    views       BIGINT         NOT NULL DEFAULT 0,
    like_count  BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP      NOT NULL,
    modified_at TIMESTAMP      NOT NULL
);
CREATE INDEX idx_posts_created_at ON posts (created_at);
CREATE INDEX idx_posts_account_id ON posts (account_id);

CREATE TABLE comments (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    post_id     BIGINT       NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    account_id  BIGINT       NOT NULL REFERENCES accounts (id),
    parent_id   BIGINT       REFERENCES comments (id) ON DELETE CASCADE,
    nickname    VARCHAR(20)  NOT NULL,
    content     VARCHAR(500) NOT NULL,
    like_count  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    modified_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_comments_post_created_at ON comments (post_id, created_at);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);

-- (대상, 계정) 복합 기본키 하나가 같은 계정의 중복 좋아요를 막는다.
CREATE TABLE post_like (
    post_id    BIGINT    NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    account_id BIGINT    NOT NULL REFERENCES accounts (id),
    created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
    PRIMARY KEY (post_id, account_id)
);

CREATE TABLE comment_like (
    comment_id BIGINT    NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    account_id BIGINT    NOT NULL REFERENCES accounts (id),
    created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
    PRIMARY KEY (comment_id, account_id)
);
```

`app/src/main/resources/application.yml` 전체:

```yaml
# 공통 설정. 환경마다 다른 값(DB 주소)은 application-local.yml / application-prod.yml 에만 둔다. 프로필을 지정하지 않으면 local 이다.
server:
  shutdown: graceful

spring:
  profiles:
    default: local
  lifecycle:
    timeout-per-shutdown-phase: 20s
  datasource:
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE:10}
      connection-timeout: 5000
  flyway:
    clean-disabled: true
  jpa:
    # 요청이 끝날 때까지 커넥션을 잡지 않는다. 지연 로딩은 서비스 트랜잭션 안에서 끝낸다.
    open-in-view: false
    hibernate:
      ddl-auto: validate

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
  metrics:
    tags:
      application: postforge
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 100ms,300ms,500ms,1s,2s

logging:
  level:
    dev.iamrat: ${LOG_LEVEL_APP:INFO}
```

`app/src/main/resources/application-local.yml` 전체:

```yaml
# local 프로필 — 운영과 값이 달라야 하는 항목만 둔다. 나머지는 application.yml.
# 환경변수는 루트의 .env.local 에서 읽는다. bootRun 은 루트에서, IDE 는 app/ 에서 실행되므로 두 경로를 둔다.
spring:
  config:
    import: optional:file:.env.local[.properties],optional:file:../.env.local[.properties]
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB}
```

`docker-compose.local.yml` 전체:

```yaml
# 로컬 인프라만 띄운다. 앱은 호스트에서 ./gradlew :app:bootRun (프로필 기본값 local).
#   docker compose --env-file .env.local -f docker-compose.local.yml up -d
services:
  postgres:
    image: pgvector/pgvector:0.8.2-pg18-trixie
    container_name: postforge-db
    ports:
      - "5432:5432"
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

`Dockerfile.runtime`의 `ENV JDK_JAVA_OPTIONS=...` 줄을 다음으로 바꾼다:

```dockerfile
# 생성 시각은 JVM 시각(LocalDateTime)으로 저장하고 화면은 그대로 보여 준다. 컨테이너 기본 UTC 대신 한국 시각으로 맞춘다.
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Seoul"
```

`.env.example` 전체:

```
POSTGRES_DB=
POSTGRES_PASSWORD=
POSTGRES_USER=
SPRING_PROFILES_ACTIVE=
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew test --tests 'dev.iamrat.postforge.ApplicationTest'`
Expected: PASS (2 tests)

- [ ] **Step 8: ADR-009와 옛 ADR 상태**

`docs/decisions/adr-009-single-module-board.md`:

```markdown
# ADR-009 게시판을 단일 모듈로 다시 짜고 부가 기능을 지운다

상태: **Accepted** (2026-10-19). 이 브랜치(`release/postforge`)에서 [ADR-001](./adr-001-use-redis-for-view-count.md), [ADR-002](./adr-002-refresh-token-rotation.md), [ADR-003](./adr-003-modular-monolith.md)을 대체한다. [ADR-008](./adr-008-switch-to-learning-platform.md)의 학습 플랫폼은 `release/postforge-study`에서 이어 간다.

## 배경

- 이 브랜치의 목표를 CRUD 게시판 하나로 좁혔다. 가장 단순한 구조로 구현하고, 실서버 부하 테스트로 기준 수치를 잰 뒤, 측정으로 확인된 병목만 고친다.
- 기존 코드는 Gradle 모듈 7개(`app`, `core`, `support`, `auth`, `board`, `ai`, `study`)였다. 모듈을 몇 개로 나누든 jar 하나, JVM 하나로 실행되므로 처리량과는 무관하다. 모듈 경계를 지키려고 둔 port·adapter·store 계층이 요청 하나를 여러 파일로 흩었다.
- Redis 조회수 버퍼, 좋아요 요청 제한, 로그인 시도 제한, refresh token 저장소는 측정 전에 가정한 부하에 맞춰 넣은 장치였다.

## 결정

- Gradle 모듈을 `app` 하나로 합치고, 기능 패키지(`auth`, `post`, `comment`, `like`, `common`)를 한 층으로 둔다. 요청은 Controller → Service → Repository로만 흐른다.
- Flyway 이력을 `V1__init.sql` 하나로 새로 시작한다. 이 브랜치는 새 DB에 배포한다.
- 조회수와 좋아요 수는 `UPDATE ... SET x = x + 1` 한 문장으로 바꾼다. 좋아요 행은 `INSERT ... ON CONFLICT DO NOTHING`이 실제로 넣었을 때만 수를 올린다.
- access token 하나만 쓴다(기본 12시간). 로그아웃은 클라이언트가 토큰을 버리는 것이다.

## 지운 것

| 대상 | 이유 |
|---|---|
| `study`, `ai` 모듈 | 학습 플랫폼은 `release/postforge-study`로 옮겼다 |
| `core`, `support` 모듈 | 단일 모듈에서는 모듈 간 계약이 필요 없다. 쓰는 것만 `common`으로 옮겼다 |
| 이메일 인증, OAuth, refresh token 교체, 프로필 수정, 관리자 권한 API | 게시판 기본 범위 밖이다. SMTP와 OAuth 앱 등록 같은 운영 부담이 따른다 |
| 태그 | 게시판 기본 범위 밖이다 |
| OpenAPI 문서, ArchUnit 테스트 | 모듈 경계 규칙이 사라졌다. API 정본은 [API 명세](../api/README.md) 하나다 |
| Redis(조회수 버퍼, 좋아요 요청 제한, 로그인 시도 제한, 토큰 저장) | 측정 전에 가정으로 넣은 장치다. 측정에서 필요가 확인되면 다시 넣는다 |
| 요청 로그 필터(requestId) | 요청마다 INFO 로그를 쓰면 부하 측정에 잡음이 된다. 지연 시간은 Actuator 지표로 본다 |
| CORS 설정 | 화면을 같은 출처에서 내보낸다 |
| 구조 문서(모듈 의존성, 요청 흐름, 인증 구조, Redis 전략, 스키마 소유권, ERD, 정책, 아키텍처 그림) | 설명하던 구조가 사라졌다. 스키마 정본은 `V1__init.sql`, API 정본은 API 명세다 |

## 검토한 대안

| 대안 | 판단 |
|---|---|
| 모듈 구조를 두고 기능만 덜어 낸다 | 처리량과 무관한 경계를 지키는 비용(port·adapter·ArchUnit)이 그대로 남는다 |
| Redis 조회수 버퍼를 유지한다 | 2026-04-09 실험에서 상세는 빨라졌지만 목록은 느려졌다([results.md](../performance/results.md)). 단순 UPDATE로 시작해 측정으로 필요를 확인한다 |

## 결과

- 토큰이 탈취되면 만료까지 막을 수 없다. 강제 로그아웃이 필요해지면 refresh token 교체를 다시 넣는다.
- 인기 글에서는 조회수·좋아요 수 UPDATE가 같은 행의 잠금을 기다린다. 이것을 측정 대상으로 삼는다.
- 로그인 시도 제한과 좋아요 남용 방지가 없다. 필요해지면 Cloudflare 속도 제한 규칙으로 코드 없이 건다.
```

`docs/decisions/adr-001-use-redis-for-view-count.md`, `docs/decisions/adr-002-refresh-token-rotation.md`: 첫 줄(제목) 바로 아래에 빈 줄과 다음 줄을 넣는다.

```markdown
상태: **Superseded** (2026-10-19, [ADR-009](./adr-009-single-module-board.md))
```

`docs/decisions/adr-003-modular-monolith.md`: `상태: **Accepted**` 줄을 위 줄로 바꾼다.

- [ ] **Step 9: CLAUDE.md, README, 문서 안내**

`CLAUDE.md`: 첫 줄부터 `## 개발 환경 재현` 바로 앞까지를 다음으로 바꾼다(`## 개발 환경 재현` 이하는 그대로 둔다).

```markdown
# PostForge

CRUD 게시판. 가장 단순한 구조로 구현하고, 실서버 부하 테스트로 찾은 병목만 하나씩 고친다. 구현·측정·트러블슈팅 과정은 기술 블로그 글로 남긴다. 단일 모듈로 바꾼 근거와 지운 기능은 [ADR-009](docs/decisions/adr-009-single-module-board.md), API는 [API 명세](docs/api/README.md), 측정 방법은 [성능 리포트](docs/performance/README.md)를 따른다.

학습 플랫폼은 `release/postforge-study` 브랜치에서 따로 진행한다.

## 구조

Gradle 모듈은 `app` 하나다. `dev.iamrat.postforge` 아래 기능 패키지를 한 층으로 두고, 요청은 Controller → Service → Repository로만 흐른다. Store·Adapter·Abstract 서비스 같은 중간 계층을 두지 않는다. 스키마 정본은 `app/src/main/resources/db/migration/V1__init.sql`이다.

| 패키지 | 책임 |
|------|------|
| `common` | 오류 코드, 전역 예외 처리, 페이지 응답 |

## 작업 규칙

- 커밋 메시지는 한국어, `type(scope): 요약`.
- 문서가 코드보다 오래 살아남지 않게 한다 — 동작을 바꾸면 해당 문서를 같은 커밋에서 고친다.
- 삭제 결정의 근거만 ADR로 남기고, 코드 보존을 위한 주석·비활성 코드는 남기지 않는다.

```

`README.md` 전체:

````markdown
# PostForge

CRUD 게시판 백엔드. 가장 단순한 구조로 구현하고, 실서버 부하 테스트로 찾은 병목만 하나씩 고친다. 구현·측정·트러블슈팅 과정은 기술 블로그 글로 남긴다.

학습 플랫폼은 `release/postforge-study` 브랜치에서 따로 진행한다. 지운 기능과 단일 모듈로 바꾼 근거는 [ADR-009](./docs/decisions/adr-009-single-module-board.md)에 있다.

## 구조

Gradle 모듈 하나(`app`)에 기능 패키지를 한 층으로 둔다. 요청은 Controller → Service → Repository로만 흐른다. 패키지 목록은 [CLAUDE.md](./CLAUDE.md), endpoint는 [API 명세](./docs/api/README.md), 스키마는 `app/src/main/resources/db/migration/V1__init.sql`에 있다.

## Tech Stack

| 영역 | 기술 |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.14, Gradle 8.14.3 |
| Data | PostgreSQL 18, Spring Data JPA, Flyway |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Operations | Docker Compose, GitHub Actions, Actuator Prometheus, Grafana, k6 |

## Local Run

필수 도구는 Java 21+, Docker다.

설정은 세 층이다. `application.yml`은 공통이고, `application-local.yml`·`application-prod.yml`에는 DB 주소처럼 환경마다 다른 값만 둔다. 프로필을 지정하지 않으면 `local`이다.

| | local | prod |
| --- | --- | --- |
| 프로필 설정 | `application-local.yml` (커밋) | `application-prod.yml` (커밋 안 함, 배포 호스트에서 마운트) |
| 환경변수 | `.env.local` (커밋 안 함) | `.env` (커밋 안 함) |
| compose | `docker-compose.local.yml` (커밋) | `docker-compose.prod.yml` (커밋 안 함) |

[`.env.example`](./.env.example)은 필요한 변수명만 값 없이 나열한다.

```bash
cp .env.example .env.local                                               # 값 채우기
docker compose --env-file .env.local -f docker-compose.local.yml up -d   # PostgreSQL
./gradlew :app:bootRun                                                   # 프로필 local
```

첫 부팅 때 Flyway가 `V1__init.sql`로 스키마를 만들고, Hibernate는 `validate`로 엔티티와 스키마가 맞는지만 본다. 옛 이력(`V0000`)이 적용된 DB에는 뜨지 않으니 새 DB를 쓴다. 화면은 `http://localhost:8080/`이다.

## Test

```bash
./gradlew test
```

테스트는 Testcontainers로 운영과 같은 PostgreSQL 이미지(`pgvector/pgvector:0.8.2-pg18-trixie`)를 띄우므로 Docker가 필요하다.

## Docker And Deployment

`release/postforge` push와 PR은 테스트만 돌린다. Actions의 APP-CI를 `workflow_dispatch`로 실행하면 테스트 뒤 layered jar 기반 이미지를 만들어 Docker Hub에 `latest`와 commit SHA tag로 게시한다. `Dockerfile.runtime`은 non-root 사용자로 실행한다.
````

`docs/README.md` 전체:

```markdown
# PostForge 문서 안내

PostForge는 CRUD 게시판이다. 가장 단순한 구조로 구현하고 부하 테스트로 찾은 병목만 고친다([ADR-009](./decisions/adr-009-single-module-board.md)).

| 위치 | 정본 책임 |
| --- | --- |
| [`api/`](./api/README.md) | endpoint, 요청·응답, HTTP status, 인증 조건 |
| [`decisions/`](./decisions/) | 선택 이유, 대안, 트레이드오프. 대체된 결정은 당시 기록으로 남긴다 |
| [`performance/`](./performance/README.md) | 측정 방법과 실행 기록 |
| [`troubleshooting/`](./troubleshooting/) | 실제로 겪은 문제의 기록(당시 코드 기준) |
| [`superpowers/`](./superpowers/) | 설계 문서와 구현 계획 |
| `app/src/main/resources/db/migration/V1__init.sql` | 스키마 |

## 작성 원칙

- 코드·설정·endpoint와 어긋난 설명을 발견하면 정본부터 고친다.
- 측정 문서는 환경, 실행 명령, commit, 원본 위치를 적는다.
- 지난 기록(ADR, troubleshooting, performance/runs)은 당시 사실을 보존하고 현재 동작처럼 쓰지 않는다.
- 내부 IP, credential, token, 운영 사용자 데이터는 문서에 남기지 않는다.
```

- [ ] **Step 10: API 명세 공통 절**

`docs/api/README.md` 전체(각 기능 절은 Task 2~5에서 덧붙인다):

````markdown
# PostForge API 명세

컨트롤러, 요청·응답 record, `SecurityConfig`, `GlobalExceptionHandler`를 기준으로 한 단일 명세다.

## 공통

### 인증

`USER`로 표시한 요청은 `Authorization: Bearer {accessToken}`이 필요하다. 토큰이 없거나 틀리거나 만료됐으면 `401 UNAUTHORIZED`다.

### 오류 응답

모든 오류는 같은 형태다.

```json
{ "code": "POST_NOT_FOUND", "message": "게시글을 찾을 수 없습니다" }
```

| status | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 입력 검증 실패(메시지는 첫 번째 위반 필드의 메시지), 읽을 수 없는 JSON, 잘못된 경로 변수 |
| 400 | `INVALID_COMMENT_PARENT`, `REPLY_DEPTH_EXCEEDED` | 답글 규칙 위반 |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` | 인증 없음·실패, 로그인 실패 |
| 403 | `FORBIDDEN` | 남의 글·댓글 수정·삭제 |
| 404 | `NOT_FOUND`, `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` | 없는 경로·글·댓글 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 메서드. `Allow` 헤더를 붙인다 |
| 409 | `DUPLICATE_USERNAME`, `DUPLICATE_NICKNAME`, `CONFLICT` | 아이디·닉네임 중복, 동시 요청이 제약 조건에 막힘 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | JSON이 아닌 본문 |
| 500 | `INTERNAL_ERROR` | 처리하지 못한 오류 |

### 페이지 응답

목록은 `page`(0부터)와 `size`를 받는다. 음수 `page`는 0으로, `size`는 1~100으로 맞춘다. 정렬은 엔드포인트마다 고정이고 `sort`는 받지 않는다.

`PageResponse`: `content`, `page`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last`, `empty`.
````

- [ ] **Step 11: 성능 리포트 현재 상태**

`docs/performance/README.md` 전체:

````markdown
# 성능 리포트

## 현재 상태

2026-10-19에 게시판을 단일 모듈로 다시 짰다([ADR-009](../decisions/adr-009-single-module-board.md)). 아래 보관된 실행과 [results.md](./results.md)는 모두 이전 구조(7개 모듈, Redis 조회수 버퍼)를 잰 기록이라 지금 코드의 수용량으로 해석하지 않는다. 새 구조의 기준 측정은 아직 없다.

## 문서와 저장 위치

| 역할 | 위치 |
|---|---|
| 실행 기록 | [runs/](./runs/) — 실행마다 디렉터리 하나 |
| 이전 구조의 비교·원인 분석 | [results.md](./results.md) |
| 서버 환경 기록(2026-06-17) | [prod-environment-spec.md](./prod-environment-spec.md) |

## 보관된 실행 (이전 구조)

| 실행 | 내용 | 해석 범위 |
|---|---|---|
| [2026-03-10 guest mixed](./runs/20260310-160406-guest-mixed/) | 로그인·읽기 혼합 | Oracle ARM의 초기 CPU 병목 |
| [2026-03-10 guest split](./runs/20260310-162722-guest-split/) | 로그인·읽기 시나리오 분리 | 요청 구성 변경에 따른 병목 분리 |
| [2026-03-11 auth CRUD](./runs/20260311-171731-auth-crud/) | 인증 후 CRUD | 당시 인증·쓰기 경로의 실행 기록 |
| [2026-03-12 N+1 개선](./runs/20260312-105320-guest-n1-fix/) | 조회 쿼리 개선 후 재측정 | 개선 전후 비교 근거 |
| [2026-03-13 batch size](./runs/20260313-163011-guest-batch-size/) | batch fetch 설정 후 재측정 | 조회 성능 후속 비교 |
| [2026-04-09 Redis 조회수](./runs/20260409-post-cache/) | 상세·목록 before/after | 상세 개선과 목록 악화를 함께 보존 |
| [2026-05-04 prod smoke](./runs/20260504-160737-prod-smoke/) | 자동 생성 k6 리포트 | commit·리소스 관측이 빠진 당시 실행 기록 |
| [2026-05-12 local smoke](./runs/20260512-140100-smoke/run-summary.md) | k6·Bruno public/auth | 로컬 기능·응답 확인 |
| [2026-08-21 local baseline](./runs/20260821-004500-local-baseline/run-summary.md) | 분야 필터·데일리 도입 후 조회 측정 | M-series 로컬 환경, LLM은 mock |

`runs/`의 endpoint·스크립트·migration 이름은 실행 당시 값이다. 측정 원문은 다시 쓰지 않는다.

## 새 실행 기록

```text
docs/performance/runs/YYYYMMDD-HHMMSS-<시나리오>/
  run-summary.md
  summary.json      # k6 --summary-export
  *.png             # Grafana 캡처
```

`run-summary.md`에 남기는 값:

- 목적과 결론
- commit SHA, 컨테이너 CPU·메모리 상한, 커넥션 풀 크기, 데이터 규모
- k6 스크립트, VUs, duration
- 요청 수, 실패율, RPS, 지연 p50/p90/p95/p99/max
- 같은 시간 구간의 CPU·메모리, HikariCP 사용·대기 커넥션, `pg_stat_statements` 상위 쿼리, 잠금 대기
- 다음 조치

## 제외할 값

- Authorization header, cookie, token
- 요청·응답 body 원문
- 운영 DB 데이터나 사용자 식별자
- 내부 IP, 비공개 domain, 계정명

URL은 필요하면 `http://10.x.x.x:8080 -> private-prod`처럼 가린다.
````

- [ ] **Step 12: 전체 테스트와 빌드**

Run: `./gradlew test bootJar`
Expected: BUILD SUCCESSFUL

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
refactor(board): 단일 모듈 골격으로 재구성

- ai·auth·board·core·study·support 모듈 삭제
- app 모듈에 공통 오류 처리와 페이지 응답 추가
- Flyway 이력을 V1__init.sql로 새로 시작
- Redis·Mailpit 로컬 컨테이너와 관련 설정 삭제
- 컨테이너 JVM 시간대를 Asia/Seoul로 지정
- ADR-009 추가, ADR-001·002·003을 대체됨으로 표시
- README·CLAUDE.md·문서 안내·API 명세 공통 절·성능 리포트 갱신
- 옛 구조 문서 삭제
EOF
```

---

### Task 2: 가입·로그인과 JWT 인증

**Files:**
- Modify: `app/build.gradle`, `app/src/main/resources/application.yml`, `app/src/test/resources/application-test.yml`, `.env.example`, `CLAUDE.md`, `README.md`, `docs/api/README.md`
- Create: `app/src/main/java/dev/iamrat/postforge/auth/{Account,AccountRepository,AccountService,JwtProvider,JwtAuthenticationFilter,SecurityConfig,AuthController}.java`
- Test: `app/src/test/java/dev/iamrat/postforge/TestApi.java`, `app/src/test/java/dev/iamrat/postforge/AuthApiTest.java`

**Interfaces:**
- Consumes: `ErrorCode`, `BusinessException`, `ErrorResponse`
- Produces:
  - `AccountService.getNickname(Long accountId): String` — 없는 계정이면 `UNAUTHORIZED`
  - 인증된 요청의 principal은 `Long accountId`. 컨트롤러는 `@AuthenticationPrincipal Long accountId`로 받는다(익명이면 `null`).
  - 보안 규칙: `POST /api/auth/register|login` 공개, `GET /api/posts/**` 공개, 나머지 `/api/**` 인증, 그 외 경로(정적 파일) 공개, `/actuator/health` 공개, 나머지 `/actuator/**` Basic 인증
  - `TestApi`: `PASSWORD`, `uniqueName()`, `bearer(String)`, `json(Object...)`, `register(String, String)`, `login(String, String)`, `signUpAndLogin()`

- [ ] **Step 1: 의존성과 설정**

`app/build.gradle`의 `dependencies`에서 `implementation 'org.springframework.boot:spring-boot-starter-data-jpa'` 다음 줄에 넣는다:

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.7'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.7'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.7'
```

`app/src/main/resources/application.yml`의 `management:` 바로 위에 넣는다:

```yaml
jwt:
  # 32바이트보다 짧으면 jjwt가 WeakKeyException으로 기동을 막는다.
  secret: ${JWT_SECRET}
  access-token-validity: ${JWT_ACCESS_TOKEN_VALIDITY:12h}

monitoring:
  username: ${MONITORING_USERNAME}
  password: ${MONITORING_PASSWORD}

```

`app/src/test/resources/application-test.yml` 끝에 넣는다:

```yaml

JWT_SECRET: test-only-jwt-secret-key-postforge-0123456789abcdef
MONITORING_USERNAME: monitor
MONITORING_PASSWORD: monitor-secret
```

`.env.example` 전체:

```
JWT_SECRET=
MONITORING_PASSWORD=
MONITORING_USERNAME=
POSTGRES_DB=
POSTGRES_PASSWORD=
POSTGRES_USER=
SPRING_PROFILES_ACTIVE=
```

- [ ] **Step 2: 테스트 도우미와 실패하는 테스트**

`app/src/test/java/dev/iamrat/postforge/TestApi.java`:

```java
package dev.iamrat.postforge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** API 테스트가 함께 쓰는 요청 묶음. 테스트끼리 DB를 함께 쓰므로 계정은 호출마다 새 이름으로 만든다. */
public class TestApi {

    public static final String PASSWORD = "Passw0rd!";
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MockMvc mockMvc;

    public TestApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** 아이디는 영문·숫자 4-20자다. 실행마다, 호출마다 달라야 한다. */
    public static String uniqueName() {
        return "u" + Long.toString(System.nanoTime(), 36) + SEQ.incrementAndGet();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    /** json("title", "제목", "parentId", null) 처럼 키와 값을 번갈아 받는다. null 값도 그대로 싣는다. */
    public static String json(Object... keyValues) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }

    public ResultActions register(String username, String nickname) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("username", username, "password", PASSWORD, "nickname", nickname)));
    }

    public String login(String username, String password) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("username", username, "password", password)))
            .andExpect(status().isOk());
        return (String) read(result, "$.accessToken");
    }

    /** 새 계정으로 가입하고 access token을 돌려준다. */
    public String signUpAndLogin() throws Exception {
        String name = uniqueName();
        register(name, name).andExpect(status().isCreated());
        return login(name, PASSWORD);
    }
}
```

`app/src/test/java/dev/iamrat/postforge/AuthApiTest.java`:

```java
package dev.iamrat.postforge;

import static dev.iamrat.postforge.TestApi.bearer;
import static dev.iamrat.postforge.TestApi.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    MockMvc mockMvc;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(mockMvc);
    }

    @Test
    void registerThenLoginIssuesAccessToken() throws Exception {
        String name = TestApi.uniqueName();

        api.register(name, name)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").isNumber());
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("username", name, "password", TestApi.PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        String name = TestApi.uniqueName();
        api.register(name, name).andExpect(status().isCreated());

        api.register(name, TestApi.uniqueName())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    void duplicateNicknameIsConflict() throws Exception {
        String nickname = TestApi.uniqueName();
        api.register(TestApi.uniqueName(), nickname).andExpect(status().isCreated());

        api.register(TestApi.uniqueName(), nickname)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        String name = TestApi.uniqueName();
        api.register(name, name).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("username", name, "password", "Wrong0pass!")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidRegisterReturnsFirstFieldMessage() throws Exception {
        api.register("ab", TestApi.uniqueName())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("아이디는 4-20자여야 합니다"));
    }

    @Test
    void protectedApiWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void brokenTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthIsPublicButOtherEndpointsNeedBasicAuth() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'dev.iamrat.postforge.AuthApiTest'`
Expected: FAIL — 컨텍스트는 뜨지만 `/api/auth/register`가 없어 404, Boot 기본 보안이 모든 요청에 Basic 인증을 요구해 401 등이 섞여 실패한다.

- [ ] **Step 4: 계정 엔티티·리포지토리·JWT**

`app/src/main/java/dev/iamrat/postforge/auth/Account.java`:

```java
package dev.iamrat.postforge.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Account(String username, String encodedPassword, String nickname) {
        this.username = username;
        this.password = encodedPassword;
        this.nickname = nickname;
    }
}
```

`app/src/main/java/dev/iamrat/postforge/auth/AccountRepository.java`:

```java
package dev.iamrat.postforge.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);
}
```

`app/src/main/java/dev/iamrat/postforge/auth/JwtProvider.java`:

```java
package dev.iamrat.postforge.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration validity;

    public JwtProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity}") Duration validity
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = validity;
    }

    public String createToken(Long accountId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(accountId))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(validity)))
            .signWith(key)
            .compact();
    }

    /** 서명이 틀리거나 만료됐으면 JwtException을 던진다. */
    public Long parseAccountId(String token) {
        String subject = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        return Long.valueOf(subject);
    }
}
```

`app/src/main/java/dev/iamrat/postforge/auth/JwtAuthenticationFilter.java`:

```java
package dev.iamrat.postforge.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 빈으로 등록하지 않는다. 빈이면 Boot가 서블릿 필터로도 등록해 보안 체인 밖에서 한 번 더 돈다. */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            try {
                Long accountId = jwtProvider.parseAccountId(header.substring(BEARER.length()));
                SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(accountId, null, List.of()));
            } catch (JwtException | IllegalArgumentException e) {
                // 틀리거나 만료된 토큰은 익명으로 둔다. 인증이 필요한 경로면 진입점이 401로 답한다.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 5: 보안 설정·서비스·컨트롤러**

`app/src/main/java/dev/iamrat/postforge/auth/SecurityConfig.java`:

```java
package dev.iamrat.postforge.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.postforge.common.ErrorCode;
import dev.iamrat.postforge.common.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    // Prometheus 수집 경로는 API 토큰이 아니라 모니터링 계정의 Basic 인증으로 막는다.
    @Bean
    @Order(1)
    SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/actuator/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, e) -> writeError(response, ErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) -> writeError(response, ErrorCode.FORBIDDEN)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    // 이 빈이 있으면 Boot가 임의 비밀번호 계정을 따로 만들지 않는다.
    @Bean
    UserDetailsService monitoringUsers(
        @Value("${monitoring.username}") String username,
        @Value("${monitoring.password}") String password,
        PasswordEncoder passwordEncoder
    ) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("MONITOR")
            .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
    }
}
```

`app/src/main/java/dev/iamrat/postforge/auth/AccountService.java`:

```java
package dev.iamrat.postforge.auth;

import dev.iamrat.postforge.common.BusinessException;
import dev.iamrat.postforge.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 앞선 검사는 흔한 중복을 구분된 코드로 막는다. 같은 값으로 동시에 들어온 요청은 UNIQUE 제약이 막고 409 CONFLICT가 된다.
    @Transactional
    public Long register(String username, String password, String nickname) {
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (accountRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        return accountRepository.save(new Account(username, passwordEncoder.encode(password), nickname)).getId();
    }

    // 아이디가 없든 비밀번호가 틀리든 같은 응답이다. 어느 쪽이 틀렸는지 알려 주면 가입된 아이디를 찾아낼 수 있다.
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        Account account = accountRepository.findByUsername(username)
            .filter(found -> passwordEncoder.matches(password, found.getPassword()))
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        return jwtProvider.createToken(account.getId());
    }

    @Transactional(readOnly = true)
    public String getNickname(Long accountId) {
        return accountRepository.findById(accountId)
            .map(Account::getNickname)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
```

`app/src/main/java/dev/iamrat/postforge/auth/AuthController.java`:

```java
package dev.iamrat.postforge.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return new RegisterResponse(accountService.register(request.username(), request.password(), request.nickname()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new TokenResponse(accountService.login(request.username(), request.password()));
    }

    public record RegisterRequest(
        @NotBlank(message = "아이디는 필수입니다")
        @Size(min = 4, max = 20, message = "아이디는 4-20자여야 합니다")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 가능합니다")
        String username,

        // BCrypt는 72바이트까지만 쓰므로 길이와 문자 종류를 함께 묶는다.
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 64, message = "비밀번호는 8-64자여야 합니다")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다")
        String password,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자여야 합니다")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 밑줄만 가능합니다")
        String nickname
    ) {
    }

    public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다") String username,
        @NotBlank(message = "비밀번호는 필수입니다") String password
    ) {
    }

    public record RegisterResponse(Long accountId) {
    }

    public record TokenResponse(String accessToken) {
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (ApplicationTest 2, AuthApiTest 8)

- [ ] **Step 7: 문서**

`CLAUDE.md` 패키지 표의 `common` 행 위에 넣는다:

```markdown
| `auth` | 가입, 로그인(access token 하나), JWT 필터, 보안 설정 |
```

`README.md` Tech Stack 표의 `Data` 행 다음에 넣는다:

```markdown
| Security | Spring Security, JWT(access token 하나) |
```

`docs/api/README.md` 끝에 덧붙인다:

```markdown

## Auth

| method | path | 인증 | 요청 | 성공 응답 |
|---|---|---|---|---|
| POST | `/api/auth/register` | PUBLIC | `{username, password, nickname}` | `201 {accountId}` |
| POST | `/api/auth/login` | PUBLIC | `{username, password}` | `200 {accessToken}` |

- `username`: 영문·숫자 4-20자. `password`: 8-64자, 영문 대소문자·숫자·특수문자(`@$!%*?&`)만 쓰고 각각 하나 이상. `nickname`: 한글·영문·숫자·밑줄 2-20자.
- 아이디·닉네임이 이미 있으면 `409 DUPLICATE_USERNAME`·`DUPLICATE_NICKNAME`. 같은 값으로 동시에 가입하면 늦은 쪽이 `409 CONFLICT`.
- 로그인 실패는 아이디가 없든 비밀번호가 틀리든 `401 INVALID_CREDENTIALS`.
- access token 하나만 발급한다. 만료는 `jwt.access-token-validity`(기본 12시간)다. 로그아웃은 클라이언트가 토큰을 버리는 것이고, 서버에는 세션도 토큰 저장소도 없다.

## 운영

| path | 인증 |
|---|---|
| `/actuator/health` | PUBLIC |
| `/actuator/prometheus` | Basic 인증(`MONITORING_USERNAME`, `MONITORING_PASSWORD`) |
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
feat(auth): 가입·로그인과 JWT 인증 추가

- 아이디·비밀번호 가입과 access token 하나만 쓰는 로그인
- JWT 필터와 API·actuator 보안 체인
- 인증 실패 401, 중복 가입 409 응답
- API 명세 Auth·운영 절 추가
EOF
```

---

### Task 3: 글 작성·목록·검색·상세·수정·삭제

**Files:**
- Create: `app/src/main/java/dev/iamrat/postforge/post/{Post,PostRepository,PostService,PostController,PostResponse,PostSummary}.java`
- Modify: `app/src/test/java/dev/iamrat/postforge/TestApi.java`, `CLAUDE.md`, `docs/api/README.md`
- Test: `app/src/test/java/dev/iamrat/postforge/PostApiTest.java`

**Interfaces:**
- Consumes: `AccountService.getNickname(Long)`, `PageResponse.from(Page)`, `BusinessException`
- Produces:
  - `PostRepository extends JpaRepository<Post, Long>` — `Page<Post> search(String keyword, Pageable)`, `List<Object[]> countComments(List<Long> postIds)`
  - `PostService.read(Long postId, Long accountId): PostResponse`
  - `PostService.toResponse(Post, Long accountId): PostResponse` (private, Task 5에서 `isLiked`를 채운다)
  - `PostResponse.of(Post, boolean isLiked)`
  - `TestApi.createPost(String token): long`, `TestApi.createPost(String token, String title, String content): long`

- [ ] **Step 1: 테스트 도우미 확장**

`TestApi.java`에서 import에 `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;`는 이미 있다. 다음 import를 더한다:

```java
import org.springframework.http.HttpHeaders;
```

`signUpAndLogin()` 아래에 넣는다:

```java
    public long createPost(String token) throws Exception {
        return createPost(token, "테스트 제목", "테스트 본문입니다. 열 글자를 넘깁니다.");
    }

    public long createPost(String token, String title, String content) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("title", title, "content", content)))
            .andExpect(status().isCreated());
        return ((Number) read(result, "$.id")).longValue();
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`app/src/test/java/dev/iamrat/postforge/PostApiTest.java`:

```java
package dev.iamrat.postforge;

import static dev.iamrat.postforge.TestApi.bearer;
import static dev.iamrat.postforge.TestApi.json;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostApiTest {

    @Autowired
    MockMvc mockMvc;

    TestApi api;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        api = new TestApi(mockMvc);
        token = api.signUpAndLogin();
    }

    @Test
    void createdPostCanBeRead() throws Exception {
        long postId = api.createPost(token, "읽을 글", "읽을 글의 본문입니다. 열 글자 이상.");

        mockMvc.perform(get("/api/posts/{id}", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("읽을 글"))
            .andExpect(jsonPath("$.likeCount").value(0))
            .andExpect(jsonPath("$.isLiked").value(false));
    }

    @Test
    void listShowsNewestFirstWithoutContent() throws Exception {
        long older = api.createPost(token);
        long newer = api.createPost(token);

        mockMvc.perform(get("/api/posts").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(newer))
            .andExpect(jsonPath("$.content[1].id").value(older))
            .andExpect(jsonPath("$.content[0].commentCount").value(0))
            .andExpect(jsonPath("$.content[0].content").doesNotExist())
            .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void searchMatchesTitleOrContent() throws Exception {
        String keyword = "검색" + TestApi.uniqueName();
        long postId = api.createPost(token, keyword + " 제목", "검색되는 글의 본문입니다.");

        mockMvc.perform(get("/api/posts").param("keyword", keyword.toUpperCase()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(postId));
    }

    @Test
    void outOfRangePagingIsClamped() throws Exception {
        mockMvc.perform(get("/api/posts").param("page", "-3").param("size", "1000").param("sort", "nope,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void missingPostIsNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void ownerCanUpdateButOthersAreForbidden() throws Exception {
        long postId = api.createPost(token);
        String other = api.signUpAndLogin();
        String body = json("title", "고친 제목", "content", "고친 본문입니다. 열 글자 이상.");

        mockMvc.perform(put("/api/posts/{id}", postId)
                .header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(put("/api/posts/{id}", postId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("고친 제목"));
    }

    @Test
    void ownerCanDeleteButOthersAreForbidden() throws Exception {
        long postId = api.createPost(token);
        String other = api.signUpAndLogin();

        mockMvc.perform(delete("/api/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer(other)))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/{id}", postId))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidTitleIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("title", "a", "content", "본문은 열 글자를 넘깁니다.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("제목은 2-100자여야 합니다"));
    }

    @Test
    void unsupportedMethodIs405WithAllowHeader() throws Exception {
        mockMvc.perform(put("/api/posts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void nonJsonBodyIs415() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.TEXT_PLAIN).content("hello"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'dev.iamrat.postforge.PostApiTest'`
Expected: FAIL — `/api/posts`가 없어 `createPost`의 201 기대가 404로 깨진다.

- [ ] **Step 4: 엔티티·리포지토리·응답**

`app/src/main/java/dev/iamrat/postforge/post/Post.java`:

```java
package dev.iamrat.postforge.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(nullable = false, length = 20, updatable = false)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    // 조회수와 좋아요 수는 UPDATE ... SET x = x + 1 문장으로만 바꾼다. JPA가 이 칸을 쓰면
    // 글을 고칠 때 읽어 둔 옛 값으로 덮어써, 그사이 다른 요청이 올린 수를 잃는다.
    @Column(insertable = false, updatable = false)
    private long views;

    @Column(name = "like_count", insertable = false, updatable = false)
    private long likeCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    public Post(Long accountId, String nickname, String title, String content) {
        this.accountId = accountId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public boolean isWrittenBy(Long accountId) {
        return this.accountId.equals(accountId);
    }
}
```

`app/src/main/java/dev/iamrat/postforge/post/PostRepository.java`:

```java
package dev.iamrat.postforge.post;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 앞뒤에 %가 붙어 B-tree 인덱스를 쓰지 못하고 전체를 훑는다. 측정에서 병목인지 확인한다.
    @Query("""
        SELECT p FROM Post p
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<Post> search(@Param("keyword") String keyword, Pageable pageable);

    // 목록 한 쪽의 댓글 수를 한 번에 센다. 행마다 [post_id, count]다.
    @Query(value = "SELECT post_id, count(*) FROM comments WHERE post_id IN (:postIds) GROUP BY post_id",
        nativeQuery = true)
    List<Object[]> countComments(@Param("postIds") List<Long> postIds);
}
```

`app/src/main/java/dev/iamrat/postforge/post/PostResponse.java`:

```java
package dev.iamrat.postforge.post;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    Long accountId,
    String nickname,
    String title,
    String content,
    long views,
    long likeCount,
    boolean isLiked,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static PostResponse of(Post post, boolean isLiked) {
        return new PostResponse(
            post.getId(),
            post.getAccountId(),
            post.getNickname(),
            post.getTitle(),
            post.getContent(),
            post.getViews(),
            post.getLikeCount(),
            isLiked,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}
```

`app/src/main/java/dev/iamrat/postforge/post/PostSummary.java`:

```java
package dev.iamrat.postforge.post;

import java.time.LocalDateTime;

/** 목록 항목. 본문은 싣지 않는다. */
public record PostSummary(
    Long id,
    String title,
    String nickname,
    long views,
    long likeCount,
    long commentCount,
    LocalDateTime createdAt
) {

    public static PostSummary of(Post post, long commentCount) {
        return new PostSummary(
            post.getId(),
            post.getTitle(),
            post.getNickname(),
            post.getViews(),
            post.getLikeCount(),
            commentCount,
            post.getCreatedAt()
        );
    }
}
```

- [ ] **Step 5: 서비스와 컨트롤러**

`app/src/main/java/dev/iamrat/postforge/post/PostService.java`:

```java
package dev.iamrat.postforge.post;

import dev.iamrat.postforge.auth.AccountService;
import dev.iamrat.postforge.common.BusinessException;
import dev.iamrat.postforge.common.ErrorCode;
import dev.iamrat.postforge.common.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final AccountService accountService;

    @Transactional
    public PostResponse create(Long accountId, String title, String content) {
        String nickname = accountService.getNickname(accountId);
        Post post = postRepository.save(new Post(accountId, nickname, title, content));
        return PostResponse.of(post, false);
    }

    public PageResponse<PostSummary> list(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = keyword == null || keyword.isBlank()
            ? postRepository.findAll(pageable)
            : postRepository.search(keyword.trim(), pageable);
        Map<Long, Long> commentCounts = countComments(posts.getContent());
        return PageResponse.from(posts.map(post -> PostSummary.of(post, commentCounts.getOrDefault(post.getId(), 0L))));
    }

    public PostResponse read(Long postId, Long accountId) {
        return toResponse(getPost(postId), accountId);
    }

    @Transactional
    public PostResponse update(Long postId, Long accountId, String title, String content) {
        Post post = getOwnedPost(postId, accountId);
        post.update(title, content);
        // modified_at은 flush 때 정해진다. 응답에 새 값을 싣기 위해 먼저 flush한다.
        postRepository.flush();
        return toResponse(post, accountId);
    }

    // 댓글·좋아요는 FK ON DELETE CASCADE로 DB가 함께 지운다.
    @Transactional
    public void delete(Long postId, Long accountId) {
        postRepository.delete(getOwnedPost(postId, accountId));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Post getOwnedPost(Long postId, Long accountId) {
        Post post = getPost(postId);
        if (!post.isWrittenBy(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    private PostResponse toResponse(Post post, Long accountId) {
        return PostResponse.of(post, false);
    }

    private Map<Long, Long> countComments(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return postRepository.countComments(postIds).stream()
            .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue()));
    }
}
```

`app/src/main/java/dev/iamrat/postforge/post/PostController.java`:

```java
package dev.iamrat.postforge.post;

import dev.iamrat.postforge.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@AuthenticationPrincipal Long accountId, @Valid @RequestBody PostRequest request) {
        return postService.create(accountId, request.title(), request.content());
    }

    // 정렬은 최신순으로 고정한다. sort를 받지 않으므로 없는 속성으로 정렬하려다 500이 나는 경로가 없다.
    @GetMapping
    public PageResponse<PostSummary> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return postService.list(keyword, Math.max(page, 0), Math.clamp(size, 1, 100));
    }

    @GetMapping("/{postId}")
    public PostResponse read(@PathVariable Long postId, @AuthenticationPrincipal Long accountId) {
        return postService.read(postId, accountId);
    }

    @PutMapping("/{postId}")
    public PostResponse update(
        @PathVariable Long postId,
        @AuthenticationPrincipal Long accountId,
        @Valid @RequestBody PostRequest request
    ) {
        return postService.update(postId, accountId, request.title(), request.content());
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId, @AuthenticationPrincipal Long accountId) {
        postService.delete(postId, accountId);
    }

    public record PostRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 2, max = 100, message = "제목은 2-100자여야 합니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        @Size(min = 10, max = 10000, message = "내용은 10-10000자여야 합니다")
        String content
    ) {
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (ApplicationTest 2, AuthApiTest 8, PostApiTest 10)

- [ ] **Step 7: 문서**

`CLAUDE.md` 패키지 표의 `common` 행 위에 넣는다:

```markdown
| `post` | 글 작성·목록·검색·상세·수정·삭제 |
```

`docs/api/README.md` 끝에 덧붙인다:

```markdown

## Posts

| method | path | 인증 | 요청 | 성공 응답 |
|---|---|---|---|---|
| POST | `/api/posts` | USER | `{title, content}` | `201 PostResponse` |
| GET | `/api/posts?keyword=&page=0&size=20` | PUBLIC | | `200 PageResponse<PostSummary>` |
| GET | `/api/posts/{postId}` | PUBLIC | | `200 PostResponse` |
| PUT | `/api/posts/{postId}` | USER, 작성자 | `{title, content}` | `200 PostResponse` |
| DELETE | `/api/posts/{postId}` | USER, 작성자 | | `204` |

- `title` 2-100자, `content` 10-10000자.
- 목록은 최신순(`createdAt` 내림차순) 고정이다. `keyword`가 있으면 제목이나 본문에 그 문자열이 들어간 글만 대소문자 구분 없이 고른다.
- `PostSummary`: `id, title, nickname, views, likeCount, commentCount, createdAt`. 본문은 싣지 않는다.
- `PostResponse`: `id, accountId, nickname, title, content, views, likeCount, isLiked, createdAt, modifiedAt`.
- 남의 글을 고치거나 지우면 `403 FORBIDDEN`. 글을 지우면 댓글과 좋아요도 함께 지워진다.
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
feat(post): 글 작성·목록·검색·상세·수정·삭제 추가

- 최신순 고정 목록과 page·size 범위 보정
- 목록 항목에 댓글 수를 한 번에 세어 싣기
- 조회수·좋아요 수 칸을 JPA 쓰기에서 제외
- 작성자만 수정·삭제, 남의 글은 403
- API 명세 Posts 절 추가
EOF
```

---

### Task 4: 댓글과 답글

**Files:**
- Create: `app/src/main/java/dev/iamrat/postforge/comment/{Comment,CommentRepository,CommentService,CommentController,CommentResponse}.java`
- Modify: `app/src/test/java/dev/iamrat/postforge/TestApi.java`, `CLAUDE.md`, `docs/api/README.md`
- Test: `app/src/test/java/dev/iamrat/postforge/CommentApiTest.java`

**Interfaces:**
- Consumes: `PostRepository.existsById(Long)`, `AccountService.getNickname(Long)`, `PageResponse.from(Page)`
- Produces:
  - `CommentService.list(Long postId, Long accountId, int page, int size): PageResponse<CommentResponse>` (Task 5에서 `isLiked`를 채운다)
  - `CommentService.update(Long postId, Long commentId, Long accountId, String content): CommentResponse`
  - `CommentResponse.of(Comment, boolean isLiked)`
  - `TestApi.createComment(String token, long postId, Long parentId): ResultActions`, `TestApi.createCommentId(String token, long postId, Long parentId): long`

- [ ] **Step 1: 테스트 도우미 확장**

`TestApi.java`의 `createPost(String, String, String)` 아래에 넣는다:

```java
    public ResultActions createComment(String token, long postId, Long parentId) throws Exception {
        return mockMvc.perform(post("/api/posts/{postId}/comments", postId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("parentId", parentId, "content", "테스트 댓글")));
    }

    public long createCommentId(String token, long postId, Long parentId) throws Exception {
        ResultActions result = createComment(token, postId, parentId).andExpect(status().isCreated());
        return ((Number) read(result, "$.id")).longValue();
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`app/src/test/java/dev/iamrat/postforge/CommentApiTest.java`:

```java
package dev.iamrat.postforge;

import static dev.iamrat.postforge.TestApi.bearer;
import static dev.iamrat.postforge.TestApi.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiTest {

    @Autowired
    MockMvc mockMvc;

    TestApi api;
    String token;
    long postId;

    @BeforeEach
    void setUp() throws Exception {
        api = new TestApi(mockMvc);
        token = api.signUpAndLogin();
        postId = api.createPost(token);
    }

    @Test
    void commentsAndRepliesAreListedInWrittenOrder() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        long replyId = api.createCommentId(token, postId, commentId);

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(commentId))
            .andExpect(jsonPath("$.content[1].id").value(replyId))
            .andExpect(jsonPath("$.content[1].parentId").value(commentId));
    }

    @Test
    void postListCountsComments() throws Exception {
        api.createCommentId(token, postId, null);
        api.createCommentId(token, postId, null);

        mockMvc.perform(get("/api/posts").param("size", "1"))
            .andExpect(jsonPath("$.content[0].id").value(postId))
            .andExpect(jsonPath("$.content[0].commentCount").value(2));
    }

    @Test
    void replyToReplyIsRejected() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        long replyId = api.createCommentId(token, postId, commentId);

        api.createComment(token, postId, replyId)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REPLY_DEPTH_EXCEEDED"));
    }

    @Test
    void parentFromAnotherPostIsRejected() throws Exception {
        long otherPostId = api.createPost(token);
        long otherComment = api.createCommentId(token, otherPostId, null);

        api.createComment(token, postId, otherComment)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COMMENT_PARENT"));
    }

    @Test
    void commentOnMissingPostIsNotFound() throws Exception {
        api.createComment(token, Long.MAX_VALUE, null)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void othersCannotUpdateComment() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        String other = api.signUpAndLogin();
        String body = json("content", "고친 댓글");

        mockMvc.perform(put("/api/posts/{postId}/comments/{id}", postId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/posts/{postId}/comments/{id}", postId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("고친 댓글"));
    }

    @Test
    void commentUnderAnotherPostPathIsNotFound() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        long otherPostId = api.createPost(token);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{id}", otherPostId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void deletingCommentRemovesItsReplies() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        api.createCommentId(token, postId, commentId);

        mockMvc.perform(delete("/api/posts/{postId}/comments/{id}", postId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deletingPostRemovesItsComments() throws Exception {
        api.createCommentId(token, postId, null);

        mockMvc.perform(delete("/api/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'dev.iamrat.postforge.CommentApiTest'`
Expected: FAIL — 댓글 엔드포인트가 없어 `createCommentId`의 201 기대가 깨진다.

- [ ] **Step 4: 엔티티·리포지토리·응답**

`app/src/main/java/dev/iamrat/postforge/comment/Comment.java`:

```java
package dev.iamrat.postforge.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// 글·부모 댓글은 연관 관계 대신 id 칸으로만 둔다. 목록에서 지연 로딩이 일어날 곳이 없다.
@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "parent_id", updatable = false)
    private Long parentId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(nullable = false, length = 20, updatable = false)
    private String nickname;

    @Column(nullable = false, length = 500)
    private String content;

    // Post와 같은 이유로 좋아요 수는 JPA가 쓰지 않는다.
    @Column(name = "like_count", insertable = false, updatable = false)
    private long likeCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    public Comment(Long postId, Long parentId, Long accountId, String nickname, String content) {
        this.postId = postId;
        this.parentId = parentId;
        this.accountId = accountId;
        this.nickname = nickname;
        this.content = content;
    }

    public void update(String content) {
        this.content = content;
    }

    public boolean isWrittenBy(Long accountId) {
        return this.accountId.equals(accountId);
    }

    public boolean isReply() {
        return parentId != null;
    }
}
```

`app/src/main/java/dev/iamrat/postforge/comment/CommentRepository.java`:

```java
package dev.iamrat.postforge.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);
}
```

`app/src/main/java/dev/iamrat/postforge/comment/CommentResponse.java`:

```java
package dev.iamrat.postforge.comment;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long postId,
    Long parentId,
    Long accountId,
    String nickname,
    String content,
    long likeCount,
    boolean isLiked,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static CommentResponse of(Comment comment, boolean isLiked) {
        return new CommentResponse(
            comment.getId(),
            comment.getPostId(),
            comment.getParentId(),
            comment.getAccountId(),
            comment.getNickname(),
            comment.getContent(),
            comment.getLikeCount(),
            isLiked,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
```

- [ ] **Step 5: 서비스와 컨트롤러**

`app/src/main/java/dev/iamrat/postforge/comment/CommentService.java`:

```java
package dev.iamrat.postforge.comment;

import dev.iamrat.postforge.auth.AccountService;
import dev.iamrat.postforge.common.BusinessException;
import dev.iamrat.postforge.common.ErrorCode;
import dev.iamrat.postforge.common.PageResponse;
import dev.iamrat.postforge.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AccountService accountService;

    @Transactional
    public CommentResponse create(Long postId, Long accountId, Long parentId, String content) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (parentId != null) {
            validateParent(postId, parentId);
        }
        String nickname = accountService.getNickname(accountId);
        Comment comment = commentRepository.save(new Comment(postId, parentId, accountId, nickname, content));
        return CommentResponse.of(comment, false);
    }

    // 없는 글이면 빈 페이지다. 글 존재 확인 쿼리를 따로 보내지 않는다.
    public PageResponse<CommentResponse> list(Long postId, Long accountId, int page, int size) {
        Page<Comment> comments = commentRepository.findByPostId(
            postId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
        return PageResponse.from(comments.map(comment -> CommentResponse.of(comment, false)));
    }

    @Transactional
    public CommentResponse update(Long postId, Long commentId, Long accountId, String content) {
        Comment comment = getOwnedComment(postId, commentId, accountId);
        comment.update(content);
        commentRepository.flush();
        return CommentResponse.of(comment, false);
    }

    // 답글과 좋아요는 FK ON DELETE CASCADE로 DB가 함께 지운다.
    @Transactional
    public void delete(Long postId, Long commentId, Long accountId) {
        commentRepository.delete(getOwnedComment(postId, commentId, accountId));
    }

    private Comment getOwnedComment(Long postId, Long commentId, Long accountId) {
        Comment comment = commentRepository.findById(commentId)
            .filter(found -> found.getPostId().equals(postId))
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return comment;
    }

    private void validateParent(Long postId, Long parentId) {
        Comment parent = commentRepository.findById(parentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!parent.getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_PARENT);
        }
        if (parent.isReply()) {
            throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
        }
    }
}
```

`app/src/main/java/dev/iamrat/postforge/comment/CommentController.java`:

```java
package dev.iamrat.postforge.comment;

import dev.iamrat.postforge.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
        @PathVariable Long postId,
        @AuthenticationPrincipal Long accountId,
        @Valid @RequestBody CommentRequest request
    ) {
        return commentService.create(postId, accountId, request.parentId(), request.content());
    }

    // 작성 순으로 고정한다. 화면은 답글을 부모 아래에 다시 묶는다.
    @GetMapping
    public PageResponse<CommentResponse> list(
        @PathVariable Long postId,
        @AuthenticationPrincipal Long accountId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return commentService.list(postId, accountId, Math.max(page, 0), Math.clamp(size, 1, 100));
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal Long accountId,
        @Valid @RequestBody CommentRequest request
    ) {
        return commentService.update(postId, commentId, accountId, request.content());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal Long accountId
    ) {
        commentService.delete(postId, commentId, accountId);
    }

    /** 수정할 때 parentId는 무시한다. */
    public record CommentRequest(
        Long parentId,

        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(min = 2, max = 500, message = "댓글은 2-500자여야 합니다")
        String content
    ) {
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (ApplicationTest 2, AuthApiTest 8, PostApiTest 10, CommentApiTest 9)

- [ ] **Step 7: 문서**

`CLAUDE.md` 패키지 표의 `common` 행 위에 넣는다:

```markdown
| `comment` | 댓글·답글(한 단계) |
```

`docs/api/README.md` 끝에 덧붙인다:

```markdown

## Comments

| method | path | 인증 | 요청 | 성공 응답 |
|---|---|---|---|---|
| POST | `/api/posts/{postId}/comments` | USER | `{parentId?, content}` | `201 CommentResponse` |
| GET | `/api/posts/{postId}/comments?page=0&size=50` | PUBLIC | | `200 PageResponse<CommentResponse>` |
| PUT | `/api/posts/{postId}/comments/{commentId}` | USER, 작성자 | `{content}` | `200 CommentResponse` |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | USER, 작성자 | | `204` |

- `content` 2-500자. 목록은 작성 순(`createdAt` 오름차순) 고정이다.
- `parentId`를 주면 답글이다. 부모는 같은 글의 댓글이어야 하고(`400 INVALID_COMMENT_PARENT`), 답글에는 답글을 달 수 없다(`400 REPLY_DEPTH_EXCEEDED`). 없는 부모는 `404 COMMENT_NOT_FOUND`.
- 없는 글에 쓰면 `404 POST_NOT_FOUND`. 목록은 없는 글이면 빈 페이지다.
- 경로의 글에 속하지 않은 댓글을 고치거나 지우면 `404 COMMENT_NOT_FOUND`, 남의 댓글이면 `403 FORBIDDEN`.
- `CommentResponse`: `id, postId, parentId, accountId, nickname, content, likeCount, isLiked, createdAt, modifiedAt`.
- 댓글을 지우면 그 답글과 좋아요도 함께 지워진다.
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
feat(comment): 댓글·답글 작성·목록·수정·삭제 추가

- 한 단계 답글과 같은 글 부모 검사
- 작성 순 고정 목록, 없는 글이면 빈 페이지
- 댓글 삭제 시 답글·좋아요를 FK로 함께 삭제
- API 명세 Comments 절 추가
EOF
```

---

### Task 5: 좋아요·조회수 자리와 동시성 테스트 (사용자 작성 전 준비)

이 Task는 메서드 자리와 테스트만 만든다. 끝나면 테스트가 실패한 상태로 멈추고 커밋하지 않는다. Task 6에서 사용자가 본문을 채운 뒤 함께 커밋한다.

**Files:**
- Create: `app/src/main/java/dev/iamrat/postforge/like/{LikeService,LikeResponse}.java`
- Modify: `post/PostService.java`, `post/PostController.java`, `comment/CommentService.java`, `comment/CommentController.java`
- Test: `app/src/test/java/dev/iamrat/postforge/LikeApiTest.java`, `app/src/test/java/dev/iamrat/postforge/CounterConcurrencyTest.java`

**Interfaces:**
- Consumes: `PostService.read`, `PostService.toResponse`, `CommentService.list`, `CommentService.update`
- Produces:
  - `LikeService.likePost(Long postId, Long accountId): LikeResponse`, `unlikePost(...)`, `likeComment(Long postId, Long commentId, Long accountId)`, `unlikeComment(...)`
  - `LikeService.isPostLiked(Long postId, Long accountId): boolean`, `likedCommentIds(List<Long> commentIds, Long accountId): Set<Long>`
  - 사용자 자리(본문이 `UnsupportedOperationException`): `PostService.increaseViews(Long)`, `LikeService.addPostLike(Long, Long)`, `removePostLike(Long, Long)`, `addCommentLike(Long, Long)`, `removeCommentLike(Long, Long)`
  - `record LikeResponse(boolean isLiked, long likeCount)`

- [ ] **Step 1: 실패하는 동시성 테스트 작성**

`app/src/test/java/dev/iamrat/postforge/CounterConcurrencyTest.java`:

```java
package dev.iamrat.postforge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.postforge.like.LikeResponse;
import dev.iamrat.postforge.like.LikeService;
import dev.iamrat.postforge.post.Post;
import dev.iamrat.postforge.post.PostRepository;
import dev.iamrat.postforge.post.PostService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 조회수·좋아요 수를 동시 요청으로 확인한다. 요청마다 커밋해야 갱신 손실이 드러나므로 테스트 트랜잭션으로 감싸지 않는다.
 * ON CONFLICT와 원자적 UPDATE는 PostgreSQL 동작이라 실제 PostgreSQL(Testcontainers)에서 돌린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounterConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired
    PostService postService;

    @Autowired
    LikeService likeService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PostRepository postRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    List<Long> accountIds;
    long postId;
    long commentId;

    @BeforeEach
    void setUp() {
        accountIds = createAccounts(THREADS);
        Long writer = accountIds.get(0);
        postId = jdbc.queryForObject("""
            INSERT INTO posts (account_id, nickname, title, content, created_at, modified_at)
            VALUES (?, 'writer', '동시성', '동시성 테스트 본문', LOCALTIMESTAMP, LOCALTIMESTAMP)
            RETURNING id
            """, Long.class, writer);
        commentId = jdbc.queryForObject("""
            INSERT INTO comments (post_id, account_id, nickname, content, created_at, modified_at)
            VALUES (?, ?, 'writer', '동시성 댓글', LOCALTIMESTAMP, LOCALTIMESTAMP)
            RETURNING id
            """, Long.class, postId, writer);
    }

    @Test
    void concurrentReadsCountEveryView() throws Exception {
        runConcurrently(50, i -> postService.read(postId, null));

        assertThat(column("posts", "views", postId)).isEqualTo(50);
    }

    @Test
    void concurrentLikesFromDifferentAccountsAreAllCounted() throws Exception {
        runConcurrently(THREADS, i -> likeService.likePost(postId, accountIds.get(i)));

        assertThat(column("posts", "like_count", postId)).isEqualTo(THREADS);
        assertThat(likeRows("post_like", "post_id", postId)).isEqualTo(THREADS);
    }

    @Test
    void sameAccountLikingTwiceAtOnceCountsOnce() throws Exception {
        Long accountId = accountIds.get(1);

        runConcurrently(2, i -> likeService.likePost(postId, accountId));

        assertThat(column("posts", "like_count", postId)).isEqualTo(1);
        assertThat(likeRows("post_like", "post_id", postId)).isEqualTo(1);
    }

    @Test
    void sameAccountUnlikingTwiceAtOnceRemovesOnce() throws Exception {
        Long accountId = accountIds.get(1);
        likeService.likePost(postId, accountId);

        runConcurrently(2, i -> likeService.unlikePost(postId, accountId));

        assertThat(column("posts", "like_count", postId)).isZero();
        assertThat(likeRows("post_like", "post_id", postId)).isZero();
    }

    @Test
    void concurrentCommentLikesAreAllCounted() throws Exception {
        runConcurrently(THREADS, i -> likeService.likeComment(postId, commentId, accountIds.get(i)));

        assertThat(column("comments", "like_count", commentId)).isEqualTo(THREADS);
        assertThat(likeRows("comment_like", "comment_id", commentId)).isEqualTo(THREADS);
    }

    // 글을 읽어 둔 사이 다른 트랜잭션이 조회수를 올려도, 글 수정이 읽어 둔 옛 값(0)으로 덮어쓰지 않는다.
    // views를 JPA 쓰기에서 빼지 않으면 Hibernate의 UPDATE가 views = 0을 함께 실어 이 테스트가 실패한다.
    @Test
    void editingPostKeepsViewsAddedMeanwhile() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate other = new TransactionTemplate(transactionManager);
        other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        outer.executeWithoutResult(status -> {
            Post post = postRepository.findById(postId).orElseThrow();
            other.executeWithoutResult(s -> jdbc.update("UPDATE posts SET views = views + 1 WHERE id = ?", postId));
            post.update("고친 제목", "고친 본문입니다. 열 글자 이상.");
        });

        assertThat(column("posts", "views", postId)).isEqualTo(1);
    }

    @Test
    void repeatedRequestsAreIdempotent() {
        Long accountId = accountIds.get(2);

        assertThat(likeService.likePost(postId, accountId)).isEqualTo(new LikeResponse(true, 1));
        assertThat(likeService.likePost(postId, accountId)).isEqualTo(new LikeResponse(true, 1));
        assertThat(likeService.unlikePost(postId, accountId)).isEqualTo(new LikeResponse(false, 0));
        assertThat(likeService.unlikePost(postId, accountId)).isEqualTo(new LikeResponse(false, 0));
    }

    /** 스레드를 모두 세워 둔 뒤 한꺼번에 출발시킨다. 하나라도 예외를 던지면 테스트가 실패한다. */
    private void runConcurrently(int count, IntConsumer task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Void>> futures = IntStream.range(0, count)
                .mapToObj(i -> pool.submit((Callable<Void>) () -> {
                    start.await();
                    task.accept(i);
                    return null;
                }))
                .toList();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Long> createAccounts(int count) {
        String prefix = "c" + Long.toString(System.nanoTime(), 36);
        return jdbc.queryForList("""
            INSERT INTO accounts (username, password, nickname)
            SELECT ? || g, 'not-used', ? || g FROM generate_series(1, ?) AS g
            RETURNING id
            """, Long.class, prefix, prefix, count);
    }

    private long column(String table, String column, long id) {
        return jdbc.queryForObject("SELECT " + column + " FROM " + table + " WHERE id = ?", Long.class, id);
    }

    private long likeRows(String table, String targetColumn, long targetId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM " + table + " WHERE " + targetColumn + " = ?", Long.class, targetId);
    }
}
```

`app/src/test/java/dev/iamrat/postforge/LikeApiTest.java`:

```java
package dev.iamrat.postforge;

import static dev.iamrat.postforge.TestApi.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LikeApiTest {

    @Autowired
    MockMvc mockMvc;

    TestApi api;
    String token;
    long postId;

    @BeforeEach
    void setUp() throws Exception {
        api = new TestApi(mockMvc);
        token = api.signUpAndLogin();
        postId = api.createPost(token);
    }

    @Test
    void likeAndUnlikePost() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/like", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isLiked").value(true))
            .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(get("/api/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$.isLiked").value(true))
            .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(delete("/api/posts/{id}/like", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isLiked").value(false))
            .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void anonymousDetailShowsNotLiked() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/like", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)));

        mockMvc.perform(get("/api/posts/{id}", postId))
            .andExpect(jsonPath("$.isLiked").value(false))
            .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void everyDetailReadIncreasesViews() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.views").value(1));
        mockMvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.views").value(2));
    }

    @Test
    void commentLikeShowsInList() throws Exception {
        long commentId = api.createCommentId(token, postId, null);

        mockMvc.perform(post("/api/posts/{postId}/comments/{id}/like", postId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(get("/api/posts/{postId}/comments", postId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$.content[0].isLiked").value(true))
            .andExpect(jsonPath("$.content[0].likeCount").value(1));
    }

    @Test
    void likingMissingPostIsNotFound() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/like", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void likingCommentUnderAnotherPostIsNotFound() throws Exception {
        long commentId = api.createCommentId(token, postId, null);
        long otherPostId = api.createPost(token);

        mockMvc.perform(post("/api/posts/{postId}/comments/{id}/like", otherPostId, commentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void likeWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/like", postId))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: LikeResponse와 LikeService(사용자 자리 포함)**

`app/src/main/java/dev/iamrat/postforge/like/LikeResponse.java`:

```java
package dev.iamrat.postforge.like;

public record LikeResponse(boolean isLiked, long likeCount) {
}
```

`app/src/main/java/dev/iamrat/postforge/like/LikeService.java`:

```java
package dev.iamrat.postforge.like;

import dev.iamrat.postforge.common.BusinessException;
import dev.iamrat.postforge.common.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요는 행 INSERT·DELETE와 카운터 UPDATE뿐이라 엔티티 없이 SQL로 다룬다.
 * 응답의 좋아요 수는 like_count 칸에서 읽고, 좋아요 행을 다시 세지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public LikeResponse likePost(Long postId, Long accountId) {
        requirePost(postId);
        addPostLike(postId, accountId);
        return new LikeResponse(true, postLikeCount(postId));
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, Long accountId) {
        requirePost(postId);
        removePostLike(postId, accountId);
        return new LikeResponse(false, postLikeCount(postId));
    }

    @Transactional
    public LikeResponse likeComment(Long postId, Long commentId, Long accountId) {
        requireComment(postId, commentId);
        addCommentLike(commentId, accountId);
        return new LikeResponse(true, commentLikeCount(commentId));
    }

    @Transactional
    public LikeResponse unlikeComment(Long postId, Long commentId, Long accountId) {
        requireComment(postId, commentId);
        removeCommentLike(commentId, accountId);
        return new LikeResponse(false, commentLikeCount(commentId));
    }

    public boolean isPostLiked(Long postId, Long accountId) {
        if (accountId == null) {
            return false;
        }
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM post_like WHERE post_id = :postId AND account_id = :accountId)",
            Map.of("postId", postId, "accountId", accountId), Boolean.class));
    }

    public Set<Long> likedCommentIds(List<Long> commentIds, Long accountId) {
        if (accountId == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(
            "SELECT comment_id FROM comment_like WHERE account_id = :accountId AND comment_id IN (:commentIds)",
            Map.of("accountId", accountId, "commentIds", commentIds), Long.class));
    }

    // TODO(사용자): post_like에 (postId, accountId) 한 행을 넣고, 실제로 들어갔을 때만 posts.like_count를 1 올린다.
    // 이미 누른 상태면 아무것도 바꾸지 않는다. 같은 계정의 요청 두 개가 동시에 와도 수는 1만 올라야 한다.
    private void addPostLike(Long postId, Long accountId) {
        throw new UnsupportedOperationException("사용자 작성");
    }

    // TODO(사용자): post_like에서 (postId, accountId) 행을 지우고, 실제로 지웠을 때만 posts.like_count를 1 내린다.
    private void removePostLike(Long postId, Long accountId) {
        throw new UnsupportedOperationException("사용자 작성");
    }

    // TODO(사용자): addPostLike와 같은 규칙으로 comment_like와 comments.like_count를 다룬다.
    private void addCommentLike(Long commentId, Long accountId) {
        throw new UnsupportedOperationException("사용자 작성");
    }

    // TODO(사용자): removePostLike와 같은 규칙으로 comment_like와 comments.like_count를 다룬다.
    private void removeCommentLike(Long commentId, Long accountId) {
        throw new UnsupportedOperationException("사용자 작성");
    }

    // 없는 글에 INSERT하면 FK 위반(409)이 된다. 먼저 확인해 404로 답한다.
    private void requirePost(Long postId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM posts WHERE id = :postId)", Map.of("postId", postId), Boolean.class);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    private void requireComment(Long postId, Long commentId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM comments WHERE id = :commentId AND post_id = :postId)",
            Map.of("commentId", commentId, "postId", postId), Boolean.class);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private long postLikeCount(Long postId) {
        return jdbc.queryForObject(
            "SELECT like_count FROM posts WHERE id = :postId", Map.of("postId", postId), Long.class);
    }

    private long commentLikeCount(Long commentId) {
        return jdbc.queryForObject(
            "SELECT like_count FROM comments WHERE id = :commentId", Map.of("commentId", commentId), Long.class);
    }
}
```

- [ ] **Step 3: PostService — 조회수 자리와 isLiked**

`post/PostService.java`에 import를 더한다:

```java
import dev.iamrat.postforge.like.LikeService;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
```

필드를 바꾼다:

```java
    private final PostRepository postRepository;
    private final AccountService accountService;
    private final LikeService likeService;
    private final NamedParameterJdbcTemplate jdbc;
```

`read`를 다음으로 바꾼다:

```java
    // 조회수를 쓰므로 쓰기 트랜잭션이다. 읽기 전용 트랜잭션에서는 PostgreSQL이 UPDATE를 거절한다.
    @Transactional
    public PostResponse read(Long postId, Long accountId) {
        increaseViews(postId);
        return toResponse(getPost(postId), accountId);
    }

    // TODO(사용자): posts.views를 1 올린다. 바뀐 행이 없으면 없는 글이므로 POST_NOT_FOUND를 던진다.
    // 동시에 50번 읽으면 정확히 50이 올라야 한다.
    private void increaseViews(Long postId) {
        throw new UnsupportedOperationException("사용자 작성");
    }
```

`toResponse`를 다음으로 바꾼다:

```java
    private PostResponse toResponse(Post post, Long accountId) {
        return PostResponse.of(post, likeService.isPostLiked(post.getId(), accountId));
    }
```

- [ ] **Step 4: PostController 좋아요 엔드포인트**

`post/PostController.java`에 import를 더한다:

```java
import dev.iamrat.postforge.like.LikeResponse;
import dev.iamrat.postforge.like.LikeService;
```

필드 `private final PostService postService;` 아래에 `private final LikeService likeService;`를 넣고, `delete` 메서드 아래에 넣는다:

```java
    @PostMapping("/{postId}/like")
    public LikeResponse like(@PathVariable Long postId, @AuthenticationPrincipal Long accountId) {
        return likeService.likePost(postId, accountId);
    }

    @DeleteMapping("/{postId}/like")
    public LikeResponse unlike(@PathVariable Long postId, @AuthenticationPrincipal Long accountId) {
        return likeService.unlikePost(postId, accountId);
    }
```

- [ ] **Step 5: CommentService·CommentController isLiked와 좋아요 엔드포인트**

`comment/CommentService.java`에 import를 더한다:

```java
import dev.iamrat.postforge.like.LikeService;
import java.util.List;
import java.util.Set;
```

필드 `private final AccountService accountService;` 아래에 `private final LikeService likeService;`를 넣고, `list`와 `update`를 바꾼다:

```java
    // 없는 글이면 빈 페이지다. 글 존재 확인 쿼리를 따로 보내지 않는다. 좋아요 여부는 한 쪽 분량을 한 번에 묻는다.
    public PageResponse<CommentResponse> list(Long postId, Long accountId, int page, int size) {
        Page<Comment> comments = commentRepository.findByPostId(
            postId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
        List<Long> commentIds = comments.getContent().stream().map(Comment::getId).toList();
        Set<Long> liked = likeService.likedCommentIds(commentIds, accountId);
        return PageResponse.from(comments.map(comment -> CommentResponse.of(comment, liked.contains(comment.getId()))));
    }

    @Transactional
    public CommentResponse update(Long postId, Long commentId, Long accountId, String content) {
        Comment comment = getOwnedComment(postId, commentId, accountId);
        comment.update(content);
        commentRepository.flush();
        boolean isLiked = likeService.likedCommentIds(List.of(commentId), accountId).contains(commentId);
        return CommentResponse.of(comment, isLiked);
    }
```

`comment/CommentController.java`에 import를 더한다:

```java
import dev.iamrat.postforge.like.LikeResponse;
import dev.iamrat.postforge.like.LikeService;
```

필드 `private final CommentService commentService;` 아래에 `private final LikeService likeService;`를 넣고, `delete` 메서드 아래에 넣는다:

```java
    @PostMapping("/{commentId}/like")
    public LikeResponse like(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal Long accountId
    ) {
        return likeService.likeComment(postId, commentId, accountId);
    }

    @DeleteMapping("/{commentId}/like")
    public LikeResponse unlike(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal Long accountId
    ) {
        return likeService.unlikeComment(postId, commentId, accountId);
    }
```

- [ ] **Step 6: 실패 확인 후 멈춤**

Run: `./gradlew test`
Expected: 컴파일은 되고, 상세 조회·좋아요를 거치는 테스트가 `UnsupportedOperationException`(500 또는 ExecutionException)으로 FAIL. ApplicationTest·AuthApiTest·CommentApiTest, 상세를 부르지 않는 PostApiTest 테스트, `CounterConcurrencyTest.editingPostKeepsViewsAddedMeanwhile`은 PASS.

여기서 멈추고 사용자에게 Task 6을 넘긴다. 커밋하지 않는다.

---

### Task 6: 핵심 쓰기 경로 (사용자 작성)

**사용자가 직접 쓴다.** 실행자는 코드를 대신 쓰지 않고, 사용자가 다 썼다고 하면 Step 2부터 확인한다.

**Files:**
- Modify: `app/src/main/java/dev/iamrat/postforge/post/PostService.java` (`increaseViews`)
- Modify: `app/src/main/java/dev/iamrat/postforge/like/LikeService.java` (`addPostLike`, `removePostLike`, `addCommentLike`, `removeCommentLike`)

**Interfaces:**
- Consumes: `NamedParameterJdbcTemplate jdbc` — `int update(String sql, Map<String, ?> params)`는 바뀐 행 수를 돌려준다.
- Produces: Task 5의 자리 다섯 개의 본문

- [ ] **Step 1: 다섯 메서드 본문 작성 (사용자)**

지킬 것:
- 조회수: `UPDATE ... SET views = views + 1` 한 문장. 읽은 값을 다시 쓰지 않는다. 바뀐 행이 0이면 `new BusinessException(ErrorCode.POST_NOT_FOUND)`.
- 좋아요 누르기: `INSERT ... ON CONFLICT DO NOTHING`이 돌려준 행 수가 1일 때만 카운터 `+ 1`.
- 좋아요 취소: `DELETE`가 돌려준 행 수가 1일 때만 카운터 `- 1`.
- 생각해 볼 것: 같은 계정의 요청 두 개가 동시에 INSERT하면 늦은 쪽은 무엇을 기다리고, 몇 행을 돌려받는가? 먼저 `SELECT`로 있는지 보고 INSERT하면 무엇이 깨지는가?

<details>
<summary>참고 구현 (막혔을 때만 펼친다)</summary>

```java
    private void increaseViews(Long postId) {
        int updated = jdbc.update("UPDATE posts SET views = views + 1 WHERE id = :postId", Map.of("postId", postId));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }
```

`PostService`에 `import java.util.Map;`이 이미 있다.

```java
    private void addPostLike(Long postId, Long accountId) {
        Map<String, Long> params = Map.of("postId", postId, "accountId", accountId);
        int inserted = jdbc.update(
            "INSERT INTO post_like (post_id, account_id) VALUES (:postId, :accountId) ON CONFLICT DO NOTHING", params);
        if (inserted == 1) {
            jdbc.update("UPDATE posts SET like_count = like_count + 1 WHERE id = :postId", params);
        }
    }

    private void removePostLike(Long postId, Long accountId) {
        Map<String, Long> params = Map.of("postId", postId, "accountId", accountId);
        int deleted = jdbc.update("DELETE FROM post_like WHERE post_id = :postId AND account_id = :accountId", params);
        if (deleted == 1) {
            jdbc.update("UPDATE posts SET like_count = like_count - 1 WHERE id = :postId", params);
        }
    }

    private void addCommentLike(Long commentId, Long accountId) {
        Map<String, Long> params = Map.of("commentId", commentId, "accountId", accountId);
        int inserted = jdbc.update(
            "INSERT INTO comment_like (comment_id, account_id) VALUES (:commentId, :accountId) ON CONFLICT DO NOTHING",
            params);
        if (inserted == 1) {
            jdbc.update("UPDATE comments SET like_count = like_count + 1 WHERE id = :commentId", params);
        }
    }

    private void removeCommentLike(Long commentId, Long accountId) {
        Map<String, Long> params = Map.of("commentId", commentId, "accountId", accountId);
        int deleted = jdbc.update(
            "DELETE FROM comment_like WHERE comment_id = :commentId AND account_id = :accountId", params);
        if (deleted == 1) {
            jdbc.update("UPDATE comments SET like_count = like_count - 1 WHERE id = :commentId", params);
        }
    }
```

</details>

- [ ] **Step 2: 전체 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (ApplicationTest 2, AuthApiTest 8, PostApiTest 10, CommentApiTest 9, LikeApiTest 7, CounterConcurrencyTest 7)

- [ ] **Step 3: 문서**

`CLAUDE.md` 패키지 표의 `common` 행 위에 넣는다:

```markdown
| `like` | 글·댓글 좋아요. 엔티티 없이 SQL로 행과 카운터를 함께 바꾼다 |
```

`docs/api/README.md`의 Posts 절 마지막 줄(`- 남의 글을 고치거나 지우면 ...`) 아래에 넣는다:

```markdown
- 상세 조회마다 `views`가 1 오른다(`UPDATE posts SET views = views + 1`).
- `isLiked`는 토큰을 보낸 경우에만 채운다. 없으면 `false`.
```

`docs/api/README.md` 끝에 덧붙인다:

```markdown

## Likes

| method | path | 인증 | 성공 응답 |
|---|---|---|---|
| POST | `/api/posts/{postId}/like` | USER | `200 {isLiked: true, likeCount}` |
| DELETE | `/api/posts/{postId}/like` | USER | `200 {isLiked: false, likeCount}` |
| POST | `/api/posts/{postId}/comments/{commentId}/like` | USER | `200 {isLiked: true, likeCount}` |
| DELETE | `/api/posts/{postId}/comments/{commentId}/like` | USER | `200 {isLiked: false, likeCount}` |

- 같은 요청을 여러 번 보내도 한 번 보낸 것과 결과가 같다. 좋아요 행을 실제로 넣거나 지운 요청만 수를 바꾼다.
- `likeCount`는 글·댓글의 `like_count` 칸 값이다. 좋아요 행을 다시 세지 않는다.
- 없는 글은 `404 POST_NOT_FOUND`, 경로의 글에 속하지 않은 댓글은 `404 COMMENT_NOT_FOUND`.
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
feat(like): 좋아요와 조회수 증가 추가

- 좋아요 행을 실제로 넣거나 지웠을 때만 카운터 변경
- 상세 조회마다 조회수를 한 문장으로 증가
- 상세·댓글 목록에 isLiked 채우기
- 동시 좋아요·조회 정확성 테스트
- API 명세 Likes 절과 조회수 규칙 추가
EOF
```

---

### Task 7: 화면 인증 호출 정리

**Files:**
- Modify: `app/src/main/resources/static/auth.js`, `app/src/main/resources/static/index.html`

**Interfaces:**
- Consumes: `POST /api/auth/register`, `POST /api/auth/login`
- Produces: 전역 `token()`, `meName()`, `api(path, opt)`, `login(username, password)`, `register(username, password, nickname)`, `logout()`

- [ ] **Step 1: auth.js 전체 교체**

`app/src/main/resources/static/auth.js`:

```js
'use strict';
// index.html이 쓰는 인증. access token 하나만 localStorage(pf.token·pf.name)에 둔다. 서버에는 세션도 refresh token도 없다.
// 인증 상태가 바뀌면 window에 'pf-auth' 이벤트를 보낸다.
const token  = () => localStorage.getItem('pf.token');
const meName = () => localStorage.getItem('pf.name');

const { api, login, register, logout } = (() => {
  const changed = () => window.dispatchEvent(new Event('pf-auth'));
  async function failure(res){
    let msg = res.status + ' 오류';
    try { msg = (await res.json()).message || msg; } catch(e){}
    return new Error(msg);
  }
  function clear(){
    ['pf.token', 'pf.name'].forEach(k => localStorage.removeItem(k));
    changed();
  }

  // 토큰이 만료돼 401이 오면 지우고 다시 로그인하게 한다. 재발급은 없다.
  async function api(path, opt={}){
    const headers = { ...(opt.headers||{}) };
    const sent = token();
    if (opt.body) headers['Content-Type'] = 'application/json';
    if (sent) headers.Authorization = 'Bearer ' + sent;
    const res = await fetch(path, { ...opt, headers });
    if (res.status === 401 && sent && token() === sent) clear();
    if (!res.ok) throw await failure(res);
    return res.status === 204 ? null : res.json();
  }

  async function postJson(path, body){
    const res = await fetch(path, { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(body) });
    if (!res.ok) throw await failure(res);
    return res.json();
  }

  async function login(username, password){
    const r = await postJson('/api/auth/login', { username, password });
    localStorage.setItem('pf.token', r.accessToken);
    localStorage.setItem('pf.name', username);
    changed();
  }

  async function register(username, password, nickname){
    await postJson('/api/auth/register', { username, password, nickname });
  }

  // 서버에 지울 것이 없으므로 브라우저의 토큰만 버린다.
  function logout(){ clear(); }

  // 다른 탭에서 로그인·로그아웃하면 이 탭도 다시 그린다. clear()는 key가 null이다.
  window.addEventListener('storage', e => {
    if (e.key === 'pf.token' || e.key === null) changed();
  });

  return { api, login, register, logout };
})();
```

- [ ] **Step 2: index.html — 가입 폼, 태그 제거**

`dialog .hint{font-size:.76rem;color:var(--ink-3);margin:.2rem 0 0}` 줄을 다음으로 바꾼다:

```css
dialog form + h3{margin-top:1.4rem}
```

`<dialog id="loginDlg">`부터 `</dialog>`까지를 다음으로 바꾼다:

```html
<dialog id="loginDlg">
  <h3>로그인</h3>
  <form id="loginForm">
    <input type="text" id="loginU" placeholder="아이디" autocomplete="username" required>
    <input type="password" id="loginP" placeholder="비밀번호" autocomplete="current-password" required>
    <button class="btn primary" type="submit">로그인</button>
    <button class="btn" type="button" onclick="loginDlg.close()">닫기</button>
  </form>
  <h3>회원가입</h3>
  <form id="registerForm">
    <input type="text" id="regU" placeholder="아이디 (영문·숫자 4-20자)" autocomplete="username" required>
    <input type="password" id="regP" placeholder="비밀번호 (8자 이상, 대소문자·숫자·특수문자)" autocomplete="new-password" required>
    <input type="text" id="regN" placeholder="닉네임 (2-20자)" required>
    <button class="btn" type="submit">가입하고 로그인</button>
  </form>
</dialog>
```

`$('#loginForm').addEventListener('submit', ...` 블록(끝의 `});`까지) 바로 아래에 넣는다:

```js
$('#registerForm').addEventListener('submit', async e=>{
  e.preventDefault();
  try {
    await register($('#regU').value, $('#regP').value, $('#regN').value);
    await login($('#regU').value, $('#regP').value);
    loginDlg.close(); toast('가입되었습니다');
  } catch(err){ toast(err.message); }
});
```

상세 화면에서 다음 줄을 지운다:

```js
        ${p.tags?.length?`<div class="tags">${p.tags.map(t=>`<span class="tag">#${esc(t)}</span>`).join('')}</div>`:''}
```

글쓰기 화면에서 다음 줄을 지운다:

```html
      <label>태그 (쉼표 구분, 선택)</label><input type="text" id="wTags" placeholder="spring, backend">
```

글쓰기 제출의 다음 세 줄을:

```js
      const tags = $('#wTags').value.split(',').map(s=>s.trim()).filter(Boolean);
      const r = await api('/api/posts', { method:'POST',
        body: JSON.stringify({ title:$('#wTitle').value, content:$('#wBody').value, tags }) });
```

다음 두 줄로 바꾼다:

```js
      const r = await api('/api/posts', { method:'POST',
        body: JSON.stringify({ title:$('#wTitle').value, content:$('#wBody').value }) });
```

`.tags{...}`·`.tag{...}` CSS 규칙이 남아 있으면 지운다(`grep -n '\.tag' app/src/main/resources/static/index.html`로 확인).

- [ ] **Step 3: 남은 옛 호출이 없는지 확인**

Run: `grep -nE "reissue|logout'|/api/auth/logout|pf.session|study|wTags|tags" app/src/main/resources/static/*`
Expected: 출력 없음

Run: `node --check app/src/main/resources/static/auth.js`
Expected: 출력 없음(문법 오류 없음)

- [ ] **Step 4: 로컬에서 띄워 확인**

옛 로컬 DB에는 `V0000` 이력이 있어 새 앱이 뜨지 않는다. 기존 DB는 건드리지 않고 새 DB를 만든다.

```bash
docker compose --env-file .env.local -f docker-compose.local.yml up -d
docker exec postforge-db sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "create database postforge_board"'
POSTGRES_DB=postforge_board ./gradlew :app:bootRun
```

`.env.local`에 `JWT_SECRET`(32바이트 이상), `MONITORING_USERNAME`, `MONITORING_PASSWORD`가 없으면 사용자에게 채워 달라고 한다.

다른 터미널에서:

Run: `curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/ && curl -s localhost:8080/auth.js | head -3`
Expected: `200`과 auth.js 첫 줄

사용자에게 브라우저(`http://localhost:8080/`)에서 가입 → 글쓰기 → 댓글·답글 → 좋아요 → 로그아웃을 확인해 달라고 한다. 확인되면 앱을 끈다.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
feat(front): 화면 인증을 access token 하나에 맞춤

- 토큰 재발급·서버 로그아웃 호출 삭제
- 로그인 창에 회원가입 폼 추가
- 태그 입력·표시 삭제
EOF
```

---

### Task 8: 부하 측정 준비 (데이터·k6 스크립트·절차)

**Files:**
- Create: `perf/seed.sql`, `perf/k6/mixed.js`, `perf/k6/hot-post.js`
- Modify: `docs/performance/README.md`

**Interfaces:**
- Consumes: 모든 API, `V1` 스키마
- Produces: 계정 `user1`~`user100000`(비밀번호 `Passw0rd!`), 글 id 1~1000000

- [ ] **Step 1: 데이터 스크립트**

`perf/seed.sql`:

```sql
-- 부하 측정용 데이터. 앱을 한 번 띄워 Flyway V1이 적용된 빈 DB에만 넣는다(수 분 걸린다).
-- 규모: 계정 10만, 글 100만, 댓글 300만, 글 좋아요 300만. 이 규모는 가정이므로 측정 결과에 함께 적는다.
-- 실행: docker exec -i <db 컨테이너> sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < perf/seed.sql
-- 모든 계정의 비밀번호는 Passw0rd! 이다(k6가 로그인에 쓴다). 외부에 열린 동안에는 누구나 이 계정으로 들어올 수 있다.
\timing on
SET TIME ZONE 'Asia/Seoul';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM accounts) THEN
        RAISE EXCEPTION 'accounts가 비어 있지 않다. 새 DB에만 넣는다';
    END IF;
END $$;

BEGIN;

-- 새 DB라 id가 1부터 차례로 붙는다. 아래 글·댓글·좋아요는 이 id(1~100000)를 그대로 쓴다.
INSERT INTO accounts (username, password, nickname, created_at)
SELECT 'user' || g, '$2y$10$hqs78T1PlIko90jcjRyFP.FZzTWO9VE8hC.dKozZIHeglTIN0FD2y', 'nick' || g, LOCALTIMESTAMP
FROM generate_series(1, 100000) AS g;

-- 글 100만: 작성자는 계정 전체에 고르게, 작성 시각은 1초 간격으로 과거부터 지금까지.
INSERT INTO posts (account_id, nickname, title, content, views, created_at, modified_at)
SELECT a, 'nick' || a, '성능 측정용 글 ' || g, repeat('측정용 본문입니다. ', 20) || g, g % 1000,
       LOCALTIMESTAMP - make_interval(secs => 1000000 - g), LOCALTIMESTAMP - make_interval(secs => 1000000 - g)
FROM generate_series(1, 1000000) AS g,
     LATERAL (SELECT 1 + (g % 100000) AS a) AS x;

-- 댓글 300만: 글마다 3개, 모두 최상위 댓글.
INSERT INTO comments (post_id, account_id, nickname, content, created_at, modified_at)
SELECT p, a, 'nick' || a, '측정용 댓글 ' || g,
       LOCALTIMESTAMP - make_interval(secs => 3000000 - g), LOCALTIMESTAMP - make_interval(secs => 3000000 - g)
FROM generate_series(1, 3000000) AS g,
     LATERAL (SELECT 1 + (g % 1000000) AS p, 1 + ((g * 7) % 100000) AS a) AS x;

-- 글 좋아요 300만: 글마다 서로 다른 계정 3명. k(0,1,2)마다 33333씩 떨어진 계정이라 (글, 계정)이 겹치지 않는다.
INSERT INTO post_like (post_id, account_id, created_at)
SELECT 1 + (g % 1000000), 1 + ((g / 1000000) * 33333 + (g % 1000000) * 7) % 100000, LOCALTIMESTAMP
FROM generate_series(0, 2999999) AS g;

UPDATE posts p SET like_count = l.c
FROM (SELECT post_id, count(*) AS c FROM post_like GROUP BY post_id) AS l
WHERE p.id = l.post_id;

COMMIT;

ANALYZE;
```

- [ ] **Step 2: 혼합 트래픽 스크립트**

`perf/k6/mixed.js`:

```js
import http from 'k6/http';
import { check } from 'k6';

// 혼합 트래픽: 목록(대부분 1쪽, 일부 깊은 쪽과 검색), 상세(조회수 증가), 댓글 목록, 소량의 글·댓글 작성.
// 실행: k6 run -e BASE_URL=http://<서버>:8080 -e VUS=100 --summary-export=<run>/summary.json perf/k6/mixed.js
const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const POSTS = Number(__ENV.POSTS || 1000000);
const JSON_HEADERS = { 'Content-Type': 'application/json' };
const ok = { 'status 2xx': r => r.status >= 200 && r.status < 300 };

export const options = {
  setupTimeout: '120s',
  scenarios: {
    mixed: { executor: 'constant-vus', vus: Number(__ENV.VUS || 50), duration: __ENV.DURATION || '3m' },
  },
  // setup의 로그인은 시나리오 밖이라 기준에서 빠진다.
  thresholds: {
    'http_req_duration{scenario:mixed}': ['p(95)<300'],
    'http_req_failed{scenario:mixed}': ['rate<0.01'],
  },
};

const randomPost = () => 1 + Math.floor(Math.random() * POSTS);

// 글·댓글 작성에 쓸 토큰 100개를 미리 받는다. 로그인(BCrypt)은 측정 구간에 넣지 않는다.
export function setup() {
  const requests = [];
  for (let i = 1; i <= 100; i++) {
    requests.push(['POST', `${BASE}/api/auth/login`,
      JSON.stringify({ username: `user${i}`, password: 'Passw0rd!' }), { headers: JSON_HEADERS }]);
  }
  return http.batch(requests).map(r => r.json('accessToken'));
}

export default function (tokens) {
  const r = Math.random();
  if (r < 0.35) {
    check(http.get(`${BASE}/api/posts?page=0&size=20`, { tags: { name: 'list' } }), ok);
  } else if (r < 0.40) {
    const page = Math.floor(Math.random() * 5000);
    check(http.get(`${BASE}/api/posts?page=${page}&size=20`, { tags: { name: 'list-deep' } }), ok);
  } else if (r < 0.42) {
    const keyword = encodeURIComponent(`글 ${randomPost()}`);
    check(http.get(`${BASE}/api/posts?keyword=${keyword}&size=20`, { tags: { name: 'search' } }), ok);
  } else if (r < 0.72) {
    check(http.get(`${BASE}/api/posts/${randomPost()}`, { tags: { name: 'detail' } }), ok);
  } else if (r < 0.92) {
    check(http.get(`${BASE}/api/posts/${randomPost()}/comments?size=50`, { tags: { name: 'comments' } }), ok);
  } else {
    const headers = { ...JSON_HEADERS, Authorization: `Bearer ${tokens[__VU % tokens.length]}` };
    if (r < 0.97) {
      check(http.post(`${BASE}/api/posts/${randomPost()}/comments`,
        JSON.stringify({ content: '부하 측정 댓글입니다' }), { headers, tags: { name: 'comment-write' } }), ok);
    } else {
      check(http.post(`${BASE}/api/posts`,
        JSON.stringify({ title: '부하 측정 글', content: '부하 측정용으로 쓴 글입니다.' }),
        { headers, tags: { name: 'post-write' } }), ok);
    }
  }
}
```

- [ ] **Step 3: 인기 글 몰림 스크립트**

`perf/k6/hot-post.js`:

```js
import http from 'k6/http';
import { check } from 'k6';

// 인기 글 몰림: 계정마다 같은 글 하나를 조회하고 좋아요를 눌렀다 취소한다.
// 세 요청 모두 posts의 같은 행을 UPDATE하므로 행 잠금 대기가 드러난다.
// 실행: k6 run -e BASE_URL=http://<서버>:8080 -e VUS=100 -e HOT_POST_ID=1000000 --summary-export=<run>/summary.json perf/k6/hot-post.js
const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 50);
const POST = __ENV.HOT_POST_ID || '1000000';
const JSON_HEADERS = { 'Content-Type': 'application/json' };
const ok = { 'status 2xx': r => r.status >= 200 && r.status < 300 };

export const options = {
  setupTimeout: '180s',
  scenarios: {
    hot: { executor: 'constant-vus', vus: VUS, duration: __ENV.DURATION || '3m' },
  },
  thresholds: {
    'http_req_duration{scenario:hot}': ['p(95)<300'],
    'http_req_failed{scenario:hot}': ['rate<0.01'],
  },
};

// 좋아요가 실제로 행을 넣고 지우도록 VU마다 다른 계정으로 로그인한다.
export function setup() {
  const requests = [];
  for (let i = 1; i <= VUS; i++) {
    requests.push(['POST', `${BASE}/api/auth/login`,
      JSON.stringify({ username: `user${i}`, password: 'Passw0rd!' }), { headers: JSON_HEADERS }]);
  }
  return http.batch(requests).map(r => r.json('accessToken'));
}

export default function (tokens) {
  const params = { headers: { Authorization: `Bearer ${tokens[(__VU - 1) % tokens.length]}` } };
  check(http.get(`${BASE}/api/posts/${POST}`, { ...params, tags: { name: 'detail' } }), ok);
  check(http.post(`${BASE}/api/posts/${POST}/like`, null, { ...params, tags: { name: 'like' } }), ok);
  check(http.del(`${BASE}/api/posts/${POST}/like`, null, { ...params, tags: { name: 'unlike' } }), ok);
}
```

- [ ] **Step 4: 로컬에서 데이터와 스크립트 확인**

seed는 빈 DB에만 들어가므로 Task 7의 `postforge_board`(가입한 계정이 있다)를 쓰지 않는다. 앱이 떠 있으면 끄고, seed 확인용 DB를 새로 만들어 띄운다.

```bash
docker exec postforge-db sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "drop database if exists postforge_seed" -c "create database postforge_seed"'
POSTGRES_DB=postforge_seed ./gradlew :app:bootRun
```

앱이 뜬 뒤 다른 터미널에서:

```bash
docker exec -i postforge-db sh -c 'psql -U "$POSTGRES_USER" -d postforge_seed -v ON_ERROR_STOP=1' < perf/seed.sql
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"user1","password":"Passw0rd!"}'
k6 run -e VUS=5 -e DURATION=15s perf/k6/mixed.js
k6 run -e VUS=5 -e DURATION=15s perf/k6/hot-post.js
```

Expected: seed가 오류 없이 끝나고, 로그인이 `{"accessToken":"..."}`를 돌려주며, 두 k6 실행 모두 `checks` 100%(로컬 수치는 기록하지 않는다). 확인 후 앱을 끈다.

- [ ] **Step 5: 성능 리포트에 절차 추가**

`docs/performance/README.md`의 `## 새 실행 기록` 바로 위에 넣는다:

````markdown
## 측정 준비 (서버, 사용자)

1. 새 DB로 앱을 한 번 띄워 Flyway `V1`을 적용한다. Redis 컨테이너는 쓰지 않는다.
2. 데이터를 넣는다: `docker exec -i <db 컨테이너> sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < perf/seed.sql`. 계정 10만, 글 100만, 댓글 300만, 글 좋아요 300만이다. 모든 계정의 비밀번호가 `Passw0rd!`이므로 외부에 열린 동안에는 누구나 들어올 수 있다.
3. 컨테이너 상한을 건다(초안: 앱 CPU 2, DB CPU 2). compose의 `deploy.resources.limits`로 걸고 값은 실행 기록에 적는다.
4. PostgreSQL을 `postgres -c shared_preload_libraries=pg_stat_statements`로 띄우고 `CREATE EXTENSION pg_stat_statements;`를 실행한다. 실행마다 시작 전에 `SELECT pg_stat_statements_reset();`.
5. 부하는 맥에서 같은 네트워크로 서버 포트에 직접 보낸다. Cloudflare 터널을 거치지 않는다.

## 실행

```bash
RUN=docs/performance/runs/$(date +%Y%m%d-%H%M%S)-mixed-vu100
mkdir -p "$RUN"
k6 run -e BASE_URL=http://<서버>:8080 -e VUS=100 --summary-export="$RUN/summary.json" perf/k6/mixed.js
```

| 스크립트 | 시나리오 |
|---|---|
| `perf/k6/mixed.js` | 목록 1쪽 35%, 깊은 쪽 5%, 검색 2%, 상세 30%, 댓글 목록 20%, 댓글 작성 5%, 글 작성 3% |
| `perf/k6/hot-post.js` | 계정마다 같은 글을 조회 → 좋아요 → 취소 (`HOT_POST_ID`, 기본 1000000) |

- 동시 사용자 수를 50, 100, 200, 400으로 올리며 기준(p95 300ms 이하, 실패율 1% 미만)이 깨지는 지점을 찾는다.
- 측정 중 `pg_stat_activity`에서 `wait_event_type = 'Lock'`인 행 수와 `pg_stat_statements`의 `total_exec_time` 상위 쿼리를 함께 남긴다.
- 개선은 한 번에 하나만 하고, 같은 스크립트로 다시 잰다.

````

`## 현재 상태`의 마지막 문장 `새 구조의 기준 측정은 아직 없다.`는 그대로 둔다.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -q -F - <<'EOF'
chore(perf): 부하 측정용 데이터와 k6 스크립트 추가

- 계정 10만·글 100만·댓글 300만·좋아요 300만 seed SQL
- 혼합 트래픽과 인기 글 몰림 k6 스크립트
- 성능 리포트에 서버 준비와 실행 절차 추가
EOF
```

---

## 이후 (사용자 몫, 이 계획 밖)

1. 브랜치 push와 APP-CI `workflow_dispatch`로 이미지 빌드.
2. 서버: 새 DB, Redis 컨테이너 제거, `.env`에 `JWT_SECRET`·`MONITORING_*`·`POSTGRES_*`, 컨테이너 상한, `pg_stat_statements`.
3. 데이터 채우기와 기준 측정, 병목 확인, 개선 반복. 실행마다 `docs/performance/runs/`에 기록.
