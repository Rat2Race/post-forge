# 게시판 1단계: 단일 모듈 재구성과 성능 측정 설계

- 날짜: 2026-10-18
- 브랜치: `release/postforge` (출발점 `aca2fe7b`)

## 목표

게시판 기본 기능을 가장 단순한 구조로 다시 짠다. 실서버에서 부하 테스트로 기준 수치를 재고, 측정으로 확인된 병목만 하나씩 고친다. 결과 지표는 "기준(p95 300ms 이하, 오류율 1% 미만)을 지키는 최대 처리량"이다. 구현·측정·트러블슈팅 과정은 기술 블로그 글로 남긴다. 학습 플랫폼은 `release/postforge-study`에서 따로 진행한다.

글·서류 기준:
- 성능 사례는 이 측정에서 실제로 관측된 병목만 쓴다.
- 사용자가 면접 질문을 넘길 만큼 이해한 것만 쓴다.

## 범위

**포함**: 회원가입·로그인, 글(작성·목록·검색·상세·수정·삭제), 조회수, 댓글(작성·목록·수정·삭제, 대댓글 `parentId` 유지), 글·댓글 좋아요와 취소.

**하지 않음**: 싫어요, 묻힘, 이의제기 재판(LLM), SSE 중계, 3D. 이 브랜치는 도메인 기능을 더하지 않고 CRUD 게시판의 구현과 성능만 다룬다.

**삭제**:

| 대상 | 이유 |
|---|---|
| `study`, `ai` 모듈 | 문서 학습 주제는 `release/postforge-study`로 보관했다 |
| `core`, `support` 모듈 | 단일 모듈에서는 모듈 간 계약이 필요 없다. 쓰는 파일만 `common`으로 옮긴다 |
| 이메일 인증, OAuth, refresh 토큰 교체, 프로필 수정, 관리자 권한 API | 집중 영역 밖이다. SMTP와 OAuth 앱 등록 같은 운영 부담이 따른다 |
| 태그(`post_tags`) | 게시판 기본 범위 밖이다 |
| OpenAPI 문서, ArchUnit 테스트 | 단일 모듈이라 모듈 경계 규칙이 없다 |
| Redis와 Redis를 쓰던 장치(조회수 버퍼, 좋아요 요청 제한, 토큰 저장) | 측정 전에 가정으로 넣은 장치다. 측정에서 필요가 확인되면 다시 넣는다 |

## 구조

Gradle 모듈은 `app` 하나만 남긴다. `settings.gradle`에서 나머지를 지운다.

```
app/src/main/java/dev/iamrat/postforge/
  PostForgeApplication.java
  auth/      AuthController  AccountService  Account  AccountRepository
             JwtProvider  JwtAuthenticationFilter  SecurityConfig
  post/      PostController  PostService  Post  PostRepository
  comment/   CommentController  CommentService  Comment  CommentRepository
  like/      LikeService  PostLike  CommentLike  PostLikeRepository  CommentLikeRepository
  common/    GlobalExceptionHandler  ErrorCode  BusinessException  PageResponse
```

규칙:
- 기능 패키지 안은 한 층으로 둔다. `presentation/application/domain/infrastructure` 하위 패키지를 두지 않는다.
- 요청은 Controller, Service, Repository 순으로만 흐른다. `*Store`, `*PersistenceAdapter`, `Abstract*Service` 같은 중간 계층을 두지 않는다.
- 좋아요 엔드포인트는 지금 경로(`/api/posts/{id}/like`, `/api/posts/{id}/comments/{commentId}/like`)를 유지한다. 그래서 `PostController`와 `CommentController`가 `LikeService`를 부른다.

## API

글·댓글·좋아요 API는 경로와 요청 형식을 지금 그대로 유지한다. `PostRequest`에서만 `tags`를 뺀다.

인증 API:

| 요청 | 본문 | 응답 |
|---|---|---|
| `POST /api/auth/register` | `{username, password, nickname}` | 201 |
| `POST /api/auth/login` | `{username, password}` | `{accessToken}` |

- access token 하나만 쓴다. 만료는 설정값이고 기본 12시간이다. 서버는 세션도 토큰 저장소도 두지 않는다.
- 로그아웃은 클라이언트가 토큰을 버리는 것으로 한다. `/api/auth/logout`과 `/api/auth/token/reissue`는 없앤다.
- 가입 본문에서 `email`을 뺀다. 인증하지 않는 이메일은 쓸 곳이 없다.
- `auth.js`와 `index.html`의 가입·로그인·로그아웃 호출을 여기에 맞춘다.
- 한계: 토큰이 탈취되면 만료까지 유효하다. 강제 로그아웃이 필요해지면 refresh 토큰 교체를 다시 넣는다.

## 데이터 모델

Flyway 이력을 새 스키마 하나(`V1__init.sql`)로 시작한다. 이 브랜치는 서버에 새 DB로 배포한다.

```
accounts     id PK, username UK, password(bcrypt), nickname UK, created_at
posts        id PK, account_id FK, nickname, title, content, views, like_count, created_at, modified_at
             INDEX (created_at), INDEX (account_id)
comments     id PK, post_id FK(ON DELETE CASCADE), account_id FK, parent_id FK(ON DELETE CASCADE),
             nickname, content, like_count, created_at, modified_at
             INDEX (post_id, created_at), INDEX (parent_id)
post_like    PK (post_id, account_id), post_id FK(ON DELETE CASCADE), account_id FK, created_at
comment_like PK (comment_id, account_id), comment_id FK(ON DELETE CASCADE), account_id FK, created_at
```

- 지금 스키마에서 빼는 것: `account_roles`, `accounts`의 `email`·`provider`·`provider_id`·`status`·`version`, 좋아요 테이블의 대리키 `id`, 감사 칸(`created_by`, `modified_by`). 읽는 곳이 없기 때문이다.
- 좋아요 테이블은 `(대상 id, 계정 id)` 복합 기본키 하나로 중복을 막는다. 지금은 대리키, UNIQUE, 보조 인덱스 세 개로 나뉘어 있다.
- `posts`와 `comments`의 `nickname`은 작성 시점 값을 복사해 둔다. 프로필 수정이 없어 닉네임이 바뀌지 않으므로, 목록 조회에서 `accounts`를 조인하지 않아도 된다.
- 목록 정렬 인덱스와 FK는 처음부터 둔다. 나중에 "인덱스를 추가해 빨라졌다"는 이야기를 만들려고 필요한 인덱스를 빼지 않는다.

## 조회수와 좋아요 수

동시 요청에도 정확한, 가장 단순한 방법으로 시작한다.

**조회수**: 상세 요청마다 `UPDATE posts SET views = views + 1 WHERE id = ?`.
- DB가 원자적으로 더하므로 동시 요청에도 수가 사라지지 않는다.
- 한 글에 요청이 몰리면 같은 행의 잠금을 기다린다. 이 대기가 병목인지는 "인기 글 몰림" 시나리오로 잰다.

**좋아요**: 한 트랜잭션 안에서 처리한다.
- 누르기: `INSERT INTO post_like … ON CONFLICT DO NOTHING`이 실제로 한 행을 넣었을 때만 `UPDATE posts SET like_count = like_count + 1`.
- 취소: `DELETE`가 실제로 한 행을 지웠을 때만 `like_count - 1`.
- 이미 그 상태면 아무것도 바꾸지 않고 현재 상태를 돌려준다(멱등).
- 응답과 목록의 좋아요 수는 `like_count` 칸에서 읽는다. 지금처럼 `COUNT(*)`로 다시 세지 않는다.
- 댓글 좋아요도 같은 방식이다.

## 오류 처리

`common/GlobalExceptionHandler` 하나가 모든 오류를 `{code, message}` JSON으로 돌려준다.

| 상황 | 상태 코드 |
|---|---|
| 입력 검증 실패 | 400 |
| 로그인 안 함, 토큰 만료 | 401 |
| 남의 글·댓글 수정·삭제 | 403 |
| 없는 글·댓글 | 404 |
| 잘못된 method / Content-Type | 405 / 415 (`260b9496`의 500 수정 유지) |
| 아이디·닉네임 중복 가입 | 409 |

## 테스트

Testcontainers Postgres에서 돌린다. `ON CONFLICT`와 원자적 `UPDATE`는 Postgres 동작이라 H2로는 보장되지 않는다.

- **동시성**:
  - 서로 다른 계정 N명이 같은 글에 동시에 좋아요를 누르면 `like_count`와 좋아요 행 수가 모두 N이다.
  - 같은 계정이 동시에 두 번 누르면 1이다.
  - 동시 조회 N번이면 `views`가 N 늘어난다.
- **API**: 가입 후 로그인, 401, 403, 404, 409.
- ArchUnit과 계층별 단위 테스트 묶음은 두지 않는다.

## 측정

**환경**
- 앱과 Postgres는 기존 서버(Intel N100, 4코어, 15GiB, Docker Compose)에서 돌린다.
- 컨테이너마다 CPU·메모리 상한을 건다(초안: 앱 2코어, DB 2코어).
- 부하는 맥에서 k6로, Cloudflare 터널을 거치지 않고 같은 네트워크로 서버 포트에 직접 보낸다.

**도구**
- k6: 스크립트는 `perf/k6/*.js`, 실행은 `k6 run`. 감싸개 스크립트는 두지 않는다.
- 앱 지표: Actuator Prometheus(요청 지연 히스토그램, HikariCP 사용·대기 커넥션, JVM). Prometheus·Grafana는 서버에 둔다(ADR-006 구성).
- DB: `pg_stat_statements`(시간을 가장 많이 쓴 쿼리), `pg_stat_activity`의 대기 이벤트(잠금 대기).

**데이터**: SQL `generate_series`로 계정 10만, 글 100만, 댓글 300만, 좋아요 300만을 채운다. 이 규모는 가정이므로 결과에 테스트 데이터 규모로 적는다.

**시나리오**
1. 혼합 트래픽: 목록(대부분 1쪽, 일부 깊은 쪽과 검색), 상세(조회수 증가 포함), 댓글 목록, 소량의 글·댓글 작성.
2. 인기 글 몰림: 여러 계정이 같은 글을 동시에 조회하고 좋아요를 누른다.

**절차**
- 동시 사용자 수를 단계적으로 올려(예: 50, 100, 200, 400) 기준이 깨지는 지점을 찾는다.
- 실행마다 커밋 SHA, 컨테이너 상한, 데이터 규모, 스크립트, 커넥션 풀 크기를 k6 요약과 함께 `docs/performance/runs/<날짜시각>-<이름>/`에 남긴다. 기존 runs 형식을 따른다.
- 개선은 한 번에 하나만 하고, 같은 스크립트로 다시 재서 비교한다.

## 진행 순서와 역할

1. 구조 재구성(Claude): 단일 모듈 패키지, 최소 auth, 게시판 이동, 모듈 삭제, `V1__init.sql`, 프론트 인증 호출 수정.
2. 핵심 쓰기 경로(사용자): 좋아요 "넣었을 때만 +1"과 조회수 증가. Claude가 메서드 자리와 동시성 테스트를 먼저 만들고, 사용자가 본문을 쓴다.
3. 테스트 통과 확인.
4. 문서 갱신(같은 커밋): `CLAUDE.md`, `docs/architecture/module-dependencies.md`, API 명세, `docs/performance`의 옛 문서. 삭제 결정의 근거는 ADR-009(단일 모듈 전환과 범위 축소)로 남긴다.
5. 서버 배포와 데이터 채우기(사용자). Redis 컨테이너 제거와 컨테이너 상한 설정을 포함한다.
6. 기준 측정, 병목 확인, 개선 반복.

## 결정 기록

- **단일 모듈**: 모듈을 몇 개로 나누든 jar 하나, JVM 하나로 실행되므로 처리량과 무관하다. 혼자 하는 프로젝트에서 멀티모듈은 실익이 작다. "나중에 분리"는 가정이라 근거가 되지 못한다.
- **Redis 제거**: 조회수 버퍼는 10/15에 "가정한 부하에 맞춘 설계"로 서류에서 뺀 장치다. 단순 `UPDATE`로 시작해 측정으로 필요를 확인한다.
- **카운터 칸 읽기**: 게시판은 읽기가 훨씬 많아 읽기 비용을 쓰기로 옮기는 편이 낫다. 대가는 인기 글에서의 행 잠금 경합이고, 이것을 측정 대상으로 삼는다.
- **좋아요 요청 제한 제거**: 성능이 아니라 남용 방지 장치다. 필요해지면 Cloudflare 속도 제한 규칙으로 코드 없이 건다.
