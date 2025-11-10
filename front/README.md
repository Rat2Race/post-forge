# PostForge Frontend

PostForge 프로젝트의 프론트엔드 애플리케이션입니다. React + TypeScript + Vite 기반으로, 인증 시스템과 게시판 기능을 제공합니다.

## 기술 스택

- React 18.2.0
- TypeScript 5.4.2
- Vite 5.2.0
- React Router DOM 6.22.1
- Axios 1.12.2
- Zustand 5.0.8 (상태 관리)
- React Hook Form 7.63.0
- Tailwind CSS 3.4.17
- Lucide React (아이콘)

## 개발 서버 실행

1) 환경 변수 파일 생성

```
cp .env.sample .env
# 필요 시 포트/호스트 수정
```

2) 의존성 설치 및 실행 (로컬에서)

```
npm install
npm run dev
```

기본 개발 서버 주소는 `http://localhost:3000` 입니다.

개발 환경에서는 Vite 프록시로 CORS를 우회합니다. `.env`에서 `VITE_USE_PROXY=true`로 두고, 서버가 `http://localhost:8080`에서 동작 중이어야 합니다.

## 환경 변수

`.env` 파일:
```
VITE_AUTH_BASE_URL=http://localhost:8080
VITE_BOARD_BASE_URL=http://localhost:8081
VITE_USE_PROXY=true
```

- `VITE_AUTH_BASE_URL` - Auth Service 주소 (포트 8080)
- `VITE_BOARD_BASE_URL` - Board Service 주소 (포트 8081)
- `VITE_USE_PROXY` - 개발 모드에서 프록시 사용 여부

## 주요 기능

### 인증 시스템
- 이메일 인증 기반 회원가입 (name, id, pw, email, nickname 필수)
- JWT 토큰 기반 로그인/로그아웃
- 자동 토큰 갱신 (Refresh Token)
- 인증 상태 관리 (Zustand)

### 게시판
- 게시글 CRUD (생성, 읽기, 수정, 삭제)
- 게시글 페이지네이션
- 게시글 좋아요 기능
- 조회수 추적
- 댓글 CRUD
- 댓글 페이지네이션
- 댓글 좋아요 기능

## 주요 경로

- 홈: `/`
- 이메일 인증: `/verify-email`
- 회원가입: `/register` (이메일 인증 필요)
- 로그인: `/login`
- 게시글 목록: `/posts`
- 게시글 작성: `/posts/create` (로그인 필요)
- 게시글 상세: `/posts/:id`

## 토큰 처리

- 로그인 성공 시 `accessToken`/`refreshToken`을 `localStorage`에 저장합니다.
- 요청 401 응답 시 자동으로 `/api/auth/reissue`로 재발급을 시도하고, 성공 시 원 요청을 1회 재시도합니다.
- 로그아웃 시 서버 `POST /api/auth/logout` 호출 후 토큰을 제거합니다.

## API 엔드포인트

### Auth Service (http://localhost:8080)
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `POST /api/auth/reissue` - 토큰 재발급
- `POST /api/auth/email/send` - 이메일 인증 코드 전송
- `GET /api/auth/email/verify` - 이메일 인증

### Board Service (http://localhost:8081)
- `GET /posts` - 게시글 목록 (페이지네이션)
- `POST /posts` - 게시글 작성
- `GET /posts/{id}` - 게시글 상세
- `PUT /posts/{id}` - 게시글 수정
- `DELETE /posts/{id}` - 게시글 삭제
- `POST /posts/{id}/like` - 게시글 좋아요 토글
- `GET /posts/{postId}/comments` - 댓글 목록
- `POST /posts/{postId}/comments` - 댓글 작성
- `PUT /posts/{postId}/comments/{commentId}` - 댓글 수정
- `DELETE /posts/{postId}/comments/{commentId}` - 댓글 삭제
- `POST /posts/{postId}/comments/{commentId}/like` - 댓글 좋아요 토글

## 프록시 설정 (vite.config.ts)

개발 모드에서 CORS 문제를 해결하기 위한 프록시 설정:

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',    // Auth Service
    changeOrigin: true
  },
  '/posts': {
    target: 'http://localhost:8081',   // Board Service
    changeOrigin: true
  }
}
```

## 서버 실행 전제조건

프론트엔드를 실행하기 전에 백엔드 서버가 실행되어 있어야 합니다:

1. Auth Service (포트 8080)
2. Board Service (포트 8081)

백엔드 실행 방법:
```bash
# 프로젝트 루트에서
./gradlew :app:bootRun
```

## 주요 변경사항 (최신 업데이트)

### 1. 타입 정의 업데이트
- `SignupRequest`에 `nickname`, `email` 필드 추가
- `PostResponse`, `CommentResponse` 타입 추가
- `Page<T>` 타입 추가 (페이지네이션)

### 2. API 클라이언트 업데이트
- `/articles` → `/posts` 엔드포인트 변경
- 페이지네이션 지원
- 좋아요 기능 추가
- 댓글 CRUD API 추가

### 3. 새 페이지 추가
- `PostsPage.tsx` - 게시글 목록
- `PostDetailPage.tsx` - 게시글 상세 + 댓글
- `CreatePostPage.tsx` - 게시글 작성

### 4. 회원가입 폼 업데이트
- nickname 입력 필드 추가
- 유효성 검사 추가

## 빌드

```
npm run build
npm run preview
```
