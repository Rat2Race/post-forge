# PostForge

PostForge는 계정 인증과 게시판 기능만 남긴 Spring Boot MVP 백엔드입니다. 현재 저장소의 기준 문서는 이 README 하나이며, 기존 포트폴리오/성능/외부 연동 문서는 MVP 범위 밖으로 내렸습니다.

## MVP Scope

남긴 기능:

- 회원가입, 로그인, 로그아웃
- JWT access token, refresh token 재발급
- 내 계정 조회, 닉네임 변경, 비밀번호 변경
- `/user/account`와 `/user/profile` 계정/프로필 조회와 수정 API
- 게시글 목록, 검색, 상세, 작성, 수정, 삭제
- 게시글 요약, 태그, 기본 카테고리
- 댓글/대댓글 작성, 조회, 수정, 삭제
- 게시글/댓글 좋아요 등록과 취소
- Redis 기반 조회수 중복 방지와 로그인 시도 보호

내린 기능:

- AI 채팅/초안 생성/벡터 검색
- 외부 상품 수집, 상품 카탈로그, 가격 추적
- 가격 하락 자동 게시글
- OAuth 로그인, 이메일 인증, 메일 발송
- OpenAPI/Swagger, Actuator, Prometheus monitoring
- S3 파일 업로드와 게시글 첨부 파일
- outbox/event relay, 모듈 간 domain event
- Bruno/k6 성능 테스트 자산과 별도 setup 패키지

## Project Layout

```text
app      Spring Boot 실행 모듈, 보안/CORS/스케줄링 조립
auth     계정, 로그인, JWT, refresh token, 로그인 시도 보호
board    게시글, 댓글, 좋아요, 조회수
core     공통 DTO, error, principal, account profile port
support  Redis, JPA auditing, web exception handling
```

Gradle에는 위 5개 모듈만 포함합니다.

## Requirements

- Java 21
- PostgreSQL
- Redis

## Configuration

필수 환경 변수는 `.env.example`에 있습니다. 로컬 실행 전 `.env`에 같은 키를 두고 값을 채웁니다.

```text
APP_CORS_ALLOWED_ORIGINS
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
REDIS_HOST
REDIS_PORT
JWT_SECRET
LOG_LEVEL_SPRING_SECURITY
LOG_LEVEL_APP
```

## Run

로컬 인프라:

```bash
docker compose -f docker-compose.local.yml up -d
```

애플리케이션:

```bash
./gradlew :app:bootRun
```

운영 compose 설정 검증:

```bash
docker compose --env-file .env.example -f docker-compose.prod.yml config --quiet
```

## Test

```bash
./gradlew check --continue
```

전체 클린 빌드와 실행 JAR 생성:

```bash
./gradlew clean test :app:bootJar --continue
```

## API Surface

공개 API:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/token/reissue`
- `GET /posts`
- `GET /posts/{postId}`
- `GET /posts/{postId}/comments`

인증 API:

- `POST /auth/logout`
- `GET /user/account`
- `PATCH /user/account/nickname`
- `PATCH /user/account/password`
- `GET /user/profile`
- `PATCH /user/profile/nickname`
- `PATCH /user/profile/password`
- `POST /posts`
- `PUT /posts/{postId}`
- `DELETE /posts/{postId}`
- `POST /posts/{postId}/like`
- `DELETE /posts/{postId}/like`
- `POST /posts/{postId}/comments`
- `PUT /posts/{postId}/comments/{commentId}`
- `DELETE /posts/{postId}/comments/{commentId}`
- `POST /posts/{postId}/comments/{commentId}/like`
- `DELETE /posts/{postId}/comments/{commentId}/like`
