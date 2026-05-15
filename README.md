# PostForge

> Spring Boot 기반 멀티 모듈 커뮤니티 게시판 + AI 트렌드 분석 게시글 생성 플랫폼

PostForge는 게시글, 댓글/대댓글, 좋아요, 파일 업로드 등 커뮤니티 핵심 기능을 제공하는 백엔드 API 서버입니다. JWT + OAuth2 인증, Redis 기반 토큰/이메일 인증 상태 관리, PostgreSQL + PgVector 기반 RAG, Docker 배포, CI/CD, 모니터링을 함께 구성했습니다.

네이버 뉴스 크롤러는 독립 실행 서비스로 동작하며, 수집한 문서를 메인 앱의 internal ingest API로 전달합니다. 메인 앱은 문서를 PgVector에 저장하고, 조건에 맞는 문서는 AI 트렌드 분석 게시글로 자동 발행합니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Java 21, Spring Boot 3.5.4, Spring Security, Spring Data JPA |
| **API Docs** | SpringDoc OpenAPI, Swagger UI, grouped OpenAPI docs |
| **AI** | Spring AI 1.0.x, OpenAI GPT-4o-mini, PgVector |
| **Auth** | JWT Access Token, Redis Refresh Token, OAuth2(Google, Naver, Kakao), Gmail SMTP |
| **Database/Cache** | PostgreSQL 18 + PgVector, Redis 7 |
| **External API** | 네이버 검색 API(뉴스) |
| **File Storage** | S3 호환 스토리지 + Presigned URL 업로드/다운로드 |
| **Testing** | JUnit 5, Bruno, k6, Agent 기반 smoke/scenario 생성 |
| **Infra** | Docker, Docker Compose, Nginx, Let's Encrypt |
| **CI/CD** | GitHub Actions → Docker Hub → Oracle Cloud |
| **Monitoring** | Spring Actuator, Prometheus, Grafana, Tailscale |
| **Frontend** | Vercel/Vite 클라이언트 연동 |
| **Build** | Gradle Wrapper 8.14.3(메인), 독립 crawl Gradle Wrapper 9.3.1 |

---

## 아키텍처

![PostForge Architecture](./docs/images/PostForge_Architecture_v2.png)

### 요청 흐름

- 사용자의 HTTPS 요청은 **Nginx** 리버스 프록시를 거쳐 **Spring Boot app 모듈**(Port 8080)로 전달됩니다.
- 주요 영속 데이터는 **PostgreSQL + PgVector**에 저장됩니다.
- Refresh Token, 이메일 인증 토큰/검증 상태는 **Redis**에 TTL 기반으로 저장됩니다.
- **crawl 독립 서비스**(Port 8090)는 네이버 뉴스를 수집한 뒤 `POST /internal/crawl/documents`로 메인 앱에 문서를 전달합니다.
- 메인 앱의 **ingest 모듈**은 문서를 벡터 저장소에 저장하고, AI 자동 게시 조건을 만족하면 게시글 생성 흐름을 실행합니다.

### CI/CD 파이프라인

1. GitHub push
2. GitHub Actions에서 `:app:bootJar` 빌드
3. `Dockerfile.runtime`으로 경량 런타임 이미지 생성
4. Docker Hub에 `latest`와 commit SHA 태그 push
5. 운영 서버에서 최신 이미지 pull 후 Docker Compose로 배포

### 모니터링

- `/actuator/health`는 공개 health check로 사용합니다.
- `/actuator/prometheus` 등 Actuator 상세 엔드포인트는 별도 HTTP Basic 인증으로 보호합니다.
- Prometheus/Grafana 대시보드는 Tailscale VPN을 통해 접근하는 구성을 전제로 합니다.

---

## ERD / 데이터 저장소

![PostForge ERD](./docs/images/PostForge_Erd_v1.png)

### 주요 테이블과 저장소

| 저장소/테이블 | 설명 |
|---------------|------|
| `members` | 일반 회원 + OAuth2 회원 정보, provider/provider_id로 소셜 계정 구분 |
| `member_roles` | 회원 권한 ElementCollection(`USER`, `ADMIN`, `MANAGER`) |
| `posts` | 게시글 본문, 요약, 카테고리, 조회수, 좋아요 수, 작성자 스냅샷 |
| `post_tags` | 게시글 태그 ElementCollection |
| `comments` | 댓글/대댓글. `parent_id` 자기참조로 1depth 대댓글 지원 |
| `post_file` | 게시글 첨부파일 메타데이터와 S3 object key |
| `post_like` | 게시글 좋아요. `(post_id, user_id)` 유니크 제약 |
| `comment_like` | 댓글 좋아요. `(comment_id, user_id)` 유니크 제약 |
| Spring AI PgVector store | RAG 문서 임베딩 저장소. Spring AI PgVector가 schema를 관리 |
| Redis `refresh_token:*` | Refresh Token 저장 및 rotation 검증 |
| Redis `email_verify_token:*` | 이메일 인증 토큰. 30분 TTL |
| Redis `email_verified:*` | 이메일 인증 완료 상태. 1시간 TTL |

> `refresh_tokens`, `email_verifications` 같은 별도 RDB 테이블은 현재 사용하지 않고 Redis TTL 기반으로 처리합니다.

---

## 멀티 모듈 구조

```text
PostForge/
├── app/                          # 실행 모듈
│   └── src/main/
│       ├── java/.../app/
│       │   └── ApplicationServer.java
│       └── resources/application.yml
│
├── auth/                         # 인증/인가 모듈
│   └── src/main/java/.../auth/
│       ├── email/                # 이메일 인증 발송/검증, Redis token state
│       ├── login/                # ID/PW 로그인, 로그아웃, UserDetails
│       ├── member/               # Member entity/repository/service
│       ├── oauth/                # OAuth2 로그인, code exchange, success/failure handler
│       ├── profile/              # 프로필/닉네임/비밀번호 변경
│       ├── register/             # 회원가입
│       ├── security/             # SecurityFilterChain, CORS, JWT/Internal API key filter
│       └── token/                # JWT 발급/재발급, Redis refresh token 저장
│
├── board/                        # 게시판 모듈
│   └── src/main/java/.../board/
│       ├── post/                 # 게시글 CRUD, 검색, 조회수, BoardPostWriter
│       ├── comment/              # 댓글/대댓글 CRUD
│       ├── like/                 # 게시글/댓글 좋아요
│       ├── file/                 # S3 presigned URL, 첨부파일 메타데이터
│       └── common/               # JPA Auditing, 공통 entity field
│
├── ai/                           # AI 추론 모듈
│   └── src/main/java/.../
│       ├── chat/                 # AI 채팅 API/service
│       ├── config/               # Spring AI 설정
│       ├── post/                 # AI 게시글 생성, output guardrail
│       └── prompt/               # prompt template loader
│
├── ingest/                       # 문서 적재 및 자동 게시 orchestration
│   └── src/main/java/.../ingest/
│       ├── document/             # /ingest/documents 문서 저장 API/service
│       └── internal/             # /internal/crawl/documents, AutoPostOrchestrator
│
├── core/                         # 공통 모듈
│   └── src/main/java/.../core/
│       ├── ai/                   # NewsAnalysisPostPublisher 등 AI port 계약
│       ├── global/dto/           # PageResponse 등 공통 DTO
│       ├── global/error/         # ErrorCode interface, CommonErrorCode, ErrorResponse
│       ├── global/exception/     # CustomException
│       ├── global/security/      # 공통 principal 계약
│       └── board/                # PostWriter 등 모듈 간 port 계약
│
├── support/                      # Spring infrastructure 지원 모듈
│   └── src/main/java/.../
│       └── support/              # Redis, OpenAPI, global web exception handler
│
├── crawl/                        # 독립 Gradle 프로젝트(Port 8090)
│   └── src/main/java/.../
│       ├── common/               # DataSourceCrawler, internal API client, crawl history entity
│       ├── news/                 # Naver News crawler
│       └── common/controller/    # POST /crawl/{source}
│
├── docs/                         # 아키텍처, 성능, 테스트 문서 및 이미지
├── setup/                        # Agent 기반 테스트 도구 설치/환경 세팅 자동화
├── tests/                        # Bruno/k6 smoke 산출물(로컬 재생성, git 제외)
├── Dockerfile                    # BuildKit 기반 app jar build stage
├── Dockerfile.runtime            # CI/CD 런타임 이미지
├── docker-compose.local.yml      # 로컬 PostgreSQL + Redis
├── docker-compose.prod.yml       # 운영 app + PostgreSQL + Redis
└── build.gradle                  # 루트 Gradle 설정
```

### 모듈 의존성

```text
app     → support, auth, board, ai, ingest
support → core (+ redis config, JPA auditing, springdoc-openapi, global web exception handler)
auth   → core (+ web, validation, jpa, redis, security, springdoc-api, jwt, mail, oauth2)
board  → core (+ web, validation, jpa, redis, security, springdoc-api, aws-s3)
ai     → core (+ web, validation, jdbc, springdoc-api, spring-ai-openai, spring-ai-pgvector)
ingest → core (+ web, validation, spring-tx, springdoc-api, spring-ai-vector-store)
core   → spring-web API, spring-data-commons API, jackson-annotations

crawl  → 독립 Spring Boot 애플리케이션(+ springdoc-ui). 메인 app에는 /internal/crawl/documents로 REST 전송
```

---

## 주요 기능

### 인증/인가

- JWT 기반 Stateless 인증
  - Access Token: 15분
  - Refresh Token: 7일, Redis `refresh_token:{userId}`에 저장
- Refresh Token Rotation 기반 재발급
- OAuth2 소셜 로그인(Google, Naver, Kakao)
- OAuth2 authorization code exchange API
- 이메일 인증
  - 인증 token은 Redis `email_verify_token:*`에 30분 TTL 저장
  - 인증 완료 상태는 Redis `email_verified:*`에 1시간 TTL 저장
- 역할 기반 접근 제어(`USER`, `ADMIN`, `MANAGER`)
- 프로필 조회/수정, 비밀번호 변경
- Internal API Key 인증(`X-Internal-Api-Key`)으로 크롤러/자동화 경로 보호
- Actuator 상세 엔드포인트 별도 Basic 인증

### 게시판

- 게시글 CRUD + 키워드 검색
- 댓글 및 1depth 대댓글
- 게시글/댓글 좋아요 등록/취소 분리(`POST`/`DELETE`)
- Cookie 기반 조회수 중복 방지(24시간)
- JPA Auditing 기반 작성자/수정자/작성일/수정일 기록
- 게시글/댓글 소유자 검증(`@PreAuthorize` + SpEL)

### 파일 관리

- S3 Presigned URL 기반 클라이언트 직접 업로드
- S3 다운로드용 Presigned URL 발급
- 게시글 첨부파일 메타데이터 관리

### AI / RAG / 자동 게시

- `/ai/chat`으로 벡터 문맥 기반 AI 채팅 제공
- `/ingest/documents`로 문서 임베딩 저장
- `/ai/documents`는 기존 클라이언트 호환을 위한 legacy 문서 적재 경로로 유지합니다.
- 크롤러가 `/internal/crawl/documents`로 문서를 전달하면 ingest 모듈이 저장 후 자동 게시 후보를 처리
- OpenAI GPT-4o-mini 기반 트렌드 분석 게시글 생성
- `NewsAnalysisPostPublisher`/`PostWriter` port로 ingest, AI, board 구현을 분리
- Output guardrail과 prompt template loader로 생성 결과 형식 제어

### 뉴스 크롤링(crawl 독립 서비스)

- Port 8090 독립 Spring Boot 애플리케이션
- 네이버 검색 API 기반 뉴스 수집
- H2 file DB에 크롤링 이력 저장 및 중복 기사 필터링
- 스케줄러 기반 주기적 수집
- 수동 트리거: `POST /crawl/naver-news`
- 신규 문서는 메인 앱 internal ingest API로 전송

### API 문서화

- SpringDoc OpenAPI 공통 metadata/security scheme은 `support` 모듈에 배치
- feature별 OpenAPI group은 `auth`, `board`, `ai`, `ingest` 모듈이 직접 소유
- 독립 `crawl` 서비스는 메인 app OpenAPI에 합치지 않고 자체 `crawl` group을 제공
- Controller/DTO에 과도한 문서 어노테이션을 붙이지 않고 Spring MVC signature 기반 schema를 우선 활용
- JWT Bearer와 Internal API Key security scheme 정의
- 메인 app은 `auth`, `board`, `ai`, `ingest`, `internal`, `all` 그룹 문서 제공
- crawl 서비스는 `crawl` 그룹 문서 제공

---

## API 엔드포인트 요약

> 실제 요청/응답 schema와 파라미터는 실행 중인 서버의 OpenAPI 문서를 우선 확인합니다.

### 공개 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/auth/register` | 회원가입 |
| `POST` | `/auth/login` | ID/PW 로그인 |
| `POST` | `/auth/token/reissue` | Refresh Token 기반 Access Token 재발급 |
| `POST` | `/auth/oauth2/exchange` | OAuth2 authorization code 교환(JSON body) |
| `POST` | `/auth/email/send` | 인증 메일 발송 |
| `GET` | `/auth/email/verify?token=` | 이메일 인증 |
| `GET` | `/posts` | 게시글 목록/검색 조회 |
| `GET` | `/posts/{postId}` | 게시글 상세 조회 |
| `GET` | `/posts/{postId}/comments` | 댓글 목록 조회 |
| `GET` | `/v3/api-docs`, `/v3/api-docs/{group}` | OpenAPI JSON |
| `GET` | `/swagger-ui.html` | Swagger UI |

### 인증 필요 API

| Method | Endpoint | 권한 | 설명 |
|--------|----------|------|------|
| `POST` | `/auth/logout` | USER, ADMIN | 로그아웃 및 Refresh Token 삭제 |
| `GET` | `/user/profile` | USER, ADMIN | 내 프로필 조회 |
| `PATCH` | `/user/profile/nickname` | USER, ADMIN | 닉네임 변경 |
| `PATCH` | `/user/profile/password` | USER, ADMIN | 비밀번호 변경 |
| `POST` | `/posts` | USER | 게시글 작성 |
| `PUT` | `/posts/{postId}` | 작성자, ADMIN | 게시글 수정 |
| `DELETE` | `/posts/{postId}` | 작성자, ADMIN | 게시글 삭제 |
| `POST` | `/posts/{postId}/like` | USER | 게시글 좋아요 |
| `DELETE` | `/posts/{postId}/like` | USER | 게시글 좋아요 취소 |
| `POST` | `/posts/{postId}/comments` | USER | 댓글/대댓글 작성 |
| `PUT` | `/posts/{postId}/comments/{commentId}` | 작성자, ADMIN | 댓글 수정 |
| `DELETE` | `/posts/{postId}/comments/{commentId}` | 작성자, ADMIN | 댓글 삭제 |
| `POST` | `/posts/{postId}/comments/{commentId}/like` | USER | 댓글 좋아요 |
| `DELETE` | `/posts/{postId}/comments/{commentId}/like` | USER | 댓글 좋아요 취소 |
| `GET` | `/files/s3/presigned-url` | USER, ADMIN | S3 업로드 URL 발급 |
| `GET` | `/files/s3/{fileId}/download-url` | USER, ADMIN | S3 다운로드 URL 발급 |

### AI / Internal / Crawl API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| `POST` | `/ai/chat` | USER, ADMIN | AI 채팅 |
| `POST` | `/ingest/documents` | USER, ADMIN | 벡터 DB 문서 저장 |
| `POST` | `/ai/documents` | USER, ADMIN | legacy 문서 저장 경로 |
| `POST` | `/internal/crawl/documents` | Internal API Key 또는 인증된 관리자 | 크롤러 문서 적재 + 자동 게시 후보 처리 |
| `POST` | `:8090/crawl/naver-news` | crawl 서비스 Internal API Key | 네이버 뉴스 수동 크롤링 트리거 |

---

## API 문서

SpringDoc OpenAPI의 공통 metadata/security scheme은 `support` 모듈에서 제공합니다.
feature별 API group은 각 feature 모듈이 소유하고, `app`은 메인 애플리케이션 endpoint만 포함하는 `all` group과 보안 requirement 조립만 담당합니다.
독립 `crawl` 서비스의 `POST /crawl/{source}`는 메인 app OpenAPI가 아니라 crawl 서비스 OpenAPI에서 확인합니다.

- 메인 app Swagger UI: `http://localhost:8080/swagger-ui.html`
- 메인 app OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- 메인 app 그룹 문서:
  - `http://localhost:8080/v3/api-docs/all`
  - `http://localhost:8080/v3/api-docs/auth`
  - `http://localhost:8080/v3/api-docs/board`
  - `http://localhost:8080/v3/api-docs/ai`
  - `http://localhost:8080/v3/api-docs/ingest`
  - `http://localhost:8080/v3/api-docs/internal`
- crawl 서비스 Swagger UI: `http://localhost:8090/swagger-ui.html`
- crawl 서비스 OpenAPI JSON:
  - `http://localhost:8090/v3/api-docs`
  - `http://localhost:8090/v3/api-docs/crawl`
- 인증 스키마:
  - JWT Bearer: `Authorization: Bearer <accessToken>`
  - Internal API Key: `X-Internal-Api-Key: <key>`

OpenAPI 문서는 Bruno/k6 smoke 생성의 우선 입력으로 사용할 수 있습니다. 정적 `openapi.*` 또는 `swagger.*` 파일이 있으면 setup이 해당 paths를 먼저 사용하고, 없으면 Controller scan으로 fallback합니다.

---

## Agent 기반 테스트 자동화

`setup/`은 이 프로젝트뿐 아니라 다른 Spring API 프로젝트에서도 재사용할 수 있는 테스트 환경 세팅 진입점입니다. Agent에게 아래처럼 요청하면 OpenAPI/Controller 분석 → policy 동기화 → Bruno/k6 테스트 생성 순서로 테스트 산출물을 준비합니다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅해줘.
```

도구 설치까지 필요하면 다음처럼 요청합니다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅하고 도구 설치까지 해줘.
```

### 핵심 흐름

| 단계 | 명령 | 역할 |
|------|------|------|
| 환경 진단 | `./setup/run.sh assess` | OS/stage/tool 상태 확인 |
| 프로젝트 분석 | `./setup/run.sh analyze-project` | `openapi.*`/`swagger.*` 우선, 없으면 Controller route 후보 수집 |
| 정책 동기화 | `./setup/run.sh sync-policy` | endpoint별 smoke/scenario/draft/manual 분류 초안 생성 |
| 테스트 생성 | `./setup/run.sh generate-tests` | Bruno/k6 generated 테스트와 JPA SQL draft 생성 |
| smoke 실행 | `BASE_URL=http://127.0.0.1:8080 ./setup/run.sh run-smoke` | 실행 중인 서버를 대상으로 Bruno + k6 smoke 실행 |

### 생성 산출물

생성 산출물은 로컬에서 재생성 가능한 파일로 취급하며 Git에 올리지 않습니다.

- `tests/testing-policy.yml` — endpoint별 smoke/scenario/manual 정책
- `tests/bruno/api/generated/` — Bruno generated request
- `tests/k6/generated/` — k6 smoke/performance script
- `tests/sql/generated/` — JPA entity 기반 검토용 SQL draft
- `tests/sql/manual/` — 수동으로 다듬어 보관하는 SQL
- `setup/state/**` — 실행 상태 JSON
- `setup/reports/**` — setup/run report

---

## 테스트

### Gradle 테스트

테스트는 JUnit Platform tag 기준으로 분류할 수 있습니다.

| 태그 | 설명 | 주요 대상 |
|------|------|----------|
| `unit` | 단위 테스트 | Service, Provider, Filter |
| `webmvc` | 컨트롤러 슬라이스 테스트 | Controller(MockMvc) |
| `integration` | 통합 테스트 | 전체 흐름 검증 |

```bash
# 전체 테스트
./gradlew test

# 통합 테스트 제외
./gradlew test -PexcludeTags=integration

# app bootJar 빌드
./gradlew :app:bootJar -PexcludeTags=integration
```

### Smoke 테스트

로컬 DB와 앱 서버가 실행 중일 때 generated smoke만 실행합니다.
generated smoke는 `tests/testing-policy.yml`에서 `class: smoke`이고 `reviewRequired: false`인 요청만 대상으로 하며, k6 smoke는 그중에서도 path variable이 없는 `GET` 요청만 포함합니다.
로그인, 쓰기, 댓글/좋아요, 내부 API처럼 fixture와 cleanup이 필요한 흐름은 Bruno `generated/scenario` 또는 `tests/k6/manual`에서 별도로 실행합니다.

```bash
./setup/run.sh generate-tests

# Bruno + k6 smoke 실행
BASE_URL=http://127.0.0.1:8080 ./setup/run.sh run-smoke

# 수동 성능 시나리오 예시
BASE_URL=http://127.0.0.1:8080 k6 run tests/k6/manual/performance.js
```

---

## Docker / 배포

### 로컬 인프라

`docker-compose.local.yml`은 로컬 개발용 PostgreSQL + Redis만 실행합니다.

```bash
cp .env.example .env
# .env 값을 채운 뒤

docker compose -f docker-compose.local.yml up -d
```

### 운영 Compose

`docker-compose.prod.yml`은 app + PostgreSQL + Redis 구성을 실행합니다.

```bash
docker compose -f docker-compose.prod.yml up -d
```

### 이미지 빌드 흐름

- `Dockerfile`은 BuildKit cache mount를 사용하는 app jar build stage입니다.
- GitHub Actions에서는 먼저 `./gradlew :app:bootJar`로 `app.jar`를 만들고, `Dockerfile.runtime`으로 경량 JRE 이미지를 빌드합니다.
- 런타임 이미지는 `rat2hub/postforge:latest`와 commit SHA 태그로 Docker Hub에 push됩니다.

---

## 실행 방법

### 사전 요구사항

- Java 21+
- Docker & Docker Compose
- PostgreSQL/Redis는 로컬 Compose 사용 권장
- OpenAI, Gmail, OAuth2, AWS S3 등 외부 연동은 필요한 기능에 맞게 env 설정

### 메인 앱 로컬 실행

```bash
# 환경 파일 준비
cp .env.example .env

# PostgreSQL + Redis 실행
docker compose -f docker-compose.local.yml up -d

# Spring Boot app 실행
set -a
source .env
set +a
./gradlew :app:bootRun
```

### crawl 서비스 로컬 실행

```bash
cd crawl
set -a
source ../.env
set +a
./gradlew bootRun

# 별도 터미널에서 수동 크롤링
curl -X POST http://localhost:8090/crawl/naver-news
```

---

## 환경변수

`.env.example`을 기준으로 `.env`를 생성합니다.

```env
# Frontend / OAuth redirect / email link
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
APP_OAUTH2_REDIRECT_URL=http://localhost:5173
APP_EMAIL_VERIFICATION_BASE_URL=http://localhost:5173

# Database
POSTGRES_DB=postforge
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Email(Gmail SMTP)
GMAIL_USERNAME=
GMAIL_PASSWORD=

# OAuth2
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=

# JWT / Internal API
JWT_SECRET=your-secret-key-at-least-32-characters
INTERNAL_API_KEY=

# Monitoring
MONITORING_USERNAME=
MONITORING_PASSWORD=

# S3 compatible storage
AWS_REGION=
AWS_BUCKET=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=

# AI / Crawl
OPENAI_API_KEY=
NAVER_NEWS_CLIENT_ID=
NAVER_NEWS_CLIENT_SECRET=
# crawl 독립 실행 시 기본값은 http://localhost:8080
INTERNAL_API_BASE_URL=http://localhost:8080
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [Performance Reports](./docs/performance/README.md) | 성능 테스트/리포트 문서 |
| [API Maintainability Feedback](./docs/api-maintainability-feedback.md) | REST API와 core Java port/API 계약 유지보수 피드백 |
| [Docker Docs](./docs/docker/README.md) | Docker 빌드/이미지/캐시/compose 문서 |
| [Redis Key Design](./docs/redis-key-design.md) | Redis key 설계 |
| [Authentication Architecture](./docs/architecture/authentication.md) | 인증 구조 정리 초안 |
| [Module Dependency Policy](./docs/architecture/module-dependencies.md) | 멀티 모듈 의존성 배치 기준 |
| [Gradle Dependency Rationale](./docs/architecture/gradle-dependency-rationale.md) | 모듈별 `build.gradle` 의존성 배치 이유 |
| [Modular Monolith Rationale](./docs/architecture/modular-monolith-rationale.md) | MSA 전환 비용을 낮추기 위한 modular monolith 설계 근거 |
| [Modular Architecture Review](./docs/architecture/modular-architecture-review.md) | 실무 리뷰 관점의 모듈 구조 평가, 리스크, 트레이드오프, 면접 답변 |
| [MSA Migration Concepts](./docs/architecture/msa-migration-concepts.md) | DB ownership, outbox, versioning, observability, PgVector ownership 개념 정리 |

---

## 향후 계획

- [x] AI 기반 게시글 자동 생성(RAG + Spring AI)
- [x] 네이버 뉴스 자동 크롤링 및 internal ingest 연동
- [x] API 문서화(SpringDoc OpenAPI)
- [x] Agent 기반 Bruno/k6 smoke 테스트 생성
- [x] 성능 측정 및 N+1 쿼리 최적화
- [ ] 검색 기능 고도화(Elasticsearch 등)
- [ ] 게시글/조회수/검색 캐싱 전략 고도화
- [ ] 정적 OpenAPI export(`docs/openapi/openapi.json`) 자동화

---

## 라이선스

현재 저장소에는 별도 `LICENSE` 파일이 없습니다. 배포/재사용 조건은 프로젝트 소유자 기준으로 확인해야 합니다.
