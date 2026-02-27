# PostForge

> Spring Boot 기반의 멀티 모듈 커뮤니티 게시판

PostForge는 게시글 작성, 댓글/대댓글, 좋아요 등 커뮤니티 핵심 기능을 제공하는 백엔드 API 서버입니다.  
JWT + OAuth2 소셜 로그인, Docker 기반 배포, CI/CD 파이프라인, 모니터링까지 구축했습니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA |
| **Auth** | JWT (Access/Refresh Token), OAuth2 (Google, Naver, Kakao) |
| **Database** | PostgreSQL 18 |
| **Infra** | Docker, Docker Compose, Nginx, Let's Encrypt (SSL) |
| **CI/CD** | GitHub Actions → Docker Hub → Oracle Cloud |
| **Cloud** | Oracle Cloud Infrastructure |
| **Monitoring** | Prometheus + Grafana (Home Cloud) |
| **VPN** | Tailscale |
| **Frontend** | Vercel |
| **Build** | Gradle 8.10 (멀티 모듈) |
| **Docs** | SpringDoc OpenAPI (Swagger UI) |

---

## 아키텍처

![PostForge Architecture](./docs/images/PostForge_Architecture.png)

### 요청 흐름

사용자의 HTTPS 요청은 **Nginx** 리버스 프록시를 거쳐 **Spring Boot 애플리케이션**(Port 8080)으로 전달되고, 데이터는 **PostgreSQL**(Port 5432)에 저장됩니다. 두 컨테이너는 **Docker Compose**로 Oracle Cloud 위에서 함께 운영됩니다.

### CI/CD 파이프라인

개발자가 GitHub에 push → **GitHub Actions**가 Docker 이미지 빌드 → **Docker Hub**에 push → 운영 서버에서 최신 이미지 pull 후 배포

### 모니터링

별도 Home Cloud 서버에서 **Prometheus**가 Actuator 메트릭을 수집하고, **Grafana**로 시각화합니다. 모니터링 대시보드는 **Tailscale VPN**을 통해 접근합니다.

---

## ERD

![PostForge ERD](./docs/images/PostForge_Erd.png)

### 테이블 구조

| 테이블 | 설명 |
|--------|------|
| `members` | 회원 정보 (일반 가입 + OAuth2 소셜 로그인, provider/provider_id로 구분) |
| `member_roles` | 회원 권한 (USER, ADMIN, MANAGER) |
| `refresh_tokens` | JWT Refresh Token 저장 및 갱신 관리 |
| `email_verifications` | 이메일 인증 토큰 (30분 유효, 매일 03시 만료 토큰 자동 정리) |
| `posts` | 게시글 (제목, 내용, 조회수, 좋아요 수) |
| `comments` | 댓글 (대댓글 1depth 지원 — `parent_id` 자기참조) |
| `post_likes` | 게시글 좋아요 (user_id + post_id 유니크 제약) |
| `comment_likes` | 댓글 좋아요 (user_id + comment_id 유니크 제약) |

---

## 멀티 모듈 구조

```
PostForge/
├── app/                          # 실행 모듈 (메인 애플리케이션)
│   └── src/main/
│       ├── java/.../app/
│       │   └── ApplicationServer.java
│       └── resources/
│           └── application.yml
│
├── auth/                         # 인증/인가 모듈
│   └── src/main/java/.../
│       ├── email/                # 이메일 인증 (controller, service, entity, scheduler)
│       ├── login/                # 로그인/로그아웃 (CustomUserDetails, LoginService)
│       ├── member/               # 회원 관리 (entity, repository, service)
│       ├── oauth/                # OAuth2 소셜 로그인 (Google, Naver, Kakao)
│       ├── profile/              # 프로필 조회
│       ├── register/             # 회원가입
│       ├── security/             # Security 설정 (CORS, SecurityFilterChain)
│       └── token/                # JWT 관리 (provider, filter, service)
│
├── board/                        # 게시판 모듈
│   └── src/main/java/.../
│       ├── post/                 # 게시글 CRUD, 좋아요, 조회수
│       ├── comment/              # 댓글/대댓글 CRUD, 좋아요
│       ├── common/               # AuditingFields, AuditorAware 설정
│       └── file/                 # 파일 서비스 (확장 예정)
│
├── core/                         # 공통 모듈
│   └── src/main/java/.../
│       └── global/exception/     # CustomException, ErrorCode, GlobalExceptionHandler
│
├── docs/                         # 문서 및 다이어그램
├── Dockerfile                    # 멀티스테이지 빌드 + BuildKit 캐싱
└── build.gradle                  # 루트 Gradle 설정
```

### 모듈 의존성

```
app → auth, board, core
auth → core
board → core
```

---

## 주요 기능

### 인증/인가
- JWT 기반 Stateless 인증 (Access Token 15분 + Refresh Token 7일)
- Token 재발급 (Refresh Token Rotation)
- OAuth2 소셜 로그인 (Google, Naver, Kakao) — 자동 회원 생성
- 이메일 인증 (Gmail SMTP + HTML 템플릿, 30분 유효, 스케줄러로 만료 토큰 자동 정리)
- 역할 기반 접근 제어 (USER, ADMIN, MANAGER)
- Actuator 엔드포인트 별도 HTTP Basic 인증

### 게시판
- 게시글 CRUD + 키워드 검색
- 댓글 및 대댓글 (1depth 제한)
- 게시글/댓글 좋아요 토글 (중복 방지)
- Cookie 기반 조회수 중복 방지 (24시간)
- JPA Auditing (작성자/수정자/작성일/수정일 자동 기록)
- 게시글/댓글 소유자 검증 (`@PreAuthorize` + SpEL)

### 인프라
- Docker 멀티스테이지 빌드 + BuildKit 캐싱 (재빌드 83% 단축)
- Docker Compose 멀티 컨테이너 운영 (App + PostgreSQL)
- Nginx Let's Encrypt SSL 자동 갱신
- GitHub Actions CI/CD 자동화
- Prometheus + Grafana 실시간 모니터링

---

## API 엔드포인트

### 공개 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/auth/register` | 회원가입 |
| `POST` | `/auth/login` | 로그인 |
| `POST` | `/auth/reissue` | 토큰 재발급 |
| `POST` | `/auth/email/send` | 인증 메일 발송 |
| `GET` | `/auth/email/verify?token=` | 이메일 인증 |
| `GET` | `/posts` | 게시글 목록 조회 (검색: `?keyword=`) |
| `GET` | `/posts/{id}` | 게시글 상세 조회 |
| `GET` | `/posts/{id}/comments` | 댓글 목록 조회 |

### 인증 필요 API

| Method | Endpoint | 권한 | 설명 |
|--------|----------|------|------|
| `POST` | `/auth/logout` | USER, ADMIN | 로그아웃 |
| `GET` | `/user/profile` | USER, ADMIN | 내 프로필 조회 |
| `POST` | `/posts` | USER | 게시글 작성 |
| `PUT` | `/posts/{id}` | 본인/ADMIN | 게시글 수정 |
| `DELETE` | `/posts/{id}` | 본인/ADMIN | 게시글 삭제 |
| `POST` | `/posts/{id}/like` | USER | 게시글 좋아요 토글 |
| `POST` | `/posts/{id}/comments` | USER | 댓글 작성 |
| `PUT` | `/posts/{id}/comments/{id}` | 본인/ADMIN | 댓글 수정 |
| `DELETE` | `/posts/{id}/comments/{id}` | 본인/ADMIN | 댓글 삭제 |
| `POST` | `/posts/{id}/comments/{id}/like` | USER | 댓글 좋아요 토글 |

---

## Docker 최적화

멀티스테이지 빌드와 BuildKit 캐싱을 적용하여 빌드 성능을 개선했습니다.

| 버전 | 변경사항 | 재빌드 시간 | 이미지 크기 | 개선율 |
|------|---------|------------|------------|--------|
| v0 | 단일 스테이지 | 1m 32s | 1.77GB | 기준 |
| v1 | 멀티 스테이지 | 1m 29s | 556MB | 이미지 69% 감소 |
| v2 | 의존성 캐싱 | 1m 6s | 556MB | 재빌드 28% 단축 |
| v4 | BuildKit + Alpine | **16s** | 556MB | **재빌드 83% 단축** |

---

## 테스트

테스트는 태그 기반으로 분류하여 관리합니다.

| 태그 | 설명 | 주요 대상 |
|------|------|----------|
| `unit` | 단위 테스트 | Service, Provider, Filter |
| `webmvc` | 컨트롤러 슬라이스 테스트 | Controller (MockMvc) |
| `integration` | 통합 테스트 | 전체 흐름 검증 |

```bash
# 전체 테스트
gradle test

# 특정 태그 제외
gradle test -PexcludeTags=integration
```

---

## 실행 방법

### 사전 요구사항
- Java 21+
- Docker & Docker Compose
- PostgreSQL 18 (또는 Docker로 실행)

### 로컬 실행

```bash
# 저장소 클론
git clone https://github.com/Rat2Race/PostForge.git
cd PostForge

# 환경변수 설정
cp docs/cloud-env.md .env
# .env 파일에 실제 값 입력

# Docker Compose로 실행
docker-compose up -d
```

### 환경변수

```env
# Frontend
FRONT_URI=http://localhost:3000

# Database
POSTGRES_DB=postforge
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

# Email (Gmail SMTP)
GMAIL_USERNAME=your_gmail
GMAIL_PASSWORD=your_app_password

# OAuth2 - Google
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=

# OAuth2 - Naver
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
NAVER_REDIRECT_URI=

# OAuth2 - Kakao
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_REDIRECT_URI=

# JWT
JWT_SECRET=your_secret_key_at_least_32_characters
JWT_ACCESS_TOKEN_VALIDITY=15
JWT_REFRESH_TOKEN_VALIDITY=7
```

---

## 향후 계획

- [ ] Spring AI를 활용한 AI 기능 통합
- [ ] 검색 기능 고도화 (Elasticsearch)
- [ ] 캐싱 전략 적용 (Redis)
- [ ] 테스트 커버리지 확대
- [ ] 파일 업로드 기능 구현

---

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
