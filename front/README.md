# Post Forge Front

React + Vite 기반의 간단한 프론트엔드로, `auth-service`와 `board-service`를 대상으로 로그인/회원가입/프로필, 게시글 목록/상세/작성/수정/삭제, 댓글 생성(데모) 기능을 제공합니다.

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

- `VITE_AUTH_BASE_URL` (기본: `http://localhost:8080`)
- `VITE_BOARD_BASE_URL` (기본: `http://localhost:8080`)
- `VITE_USE_PROXY` (기본: `true`) - dev에서 동일 오리진으로 프록시 사용

각 서비스가 실제로 사용하는 포트에 맞게 조정해 주세요.

## 주요 경로

- 로그인: `/login`
- 회원가입: `/signup`
- 프로필: `/profile` (로그인 필요)
- 게시글 목록: `/articles`
- 게시글 작성: `/articles/new` (로그인 필요)
- 게시글 상세: `/articles/:id`
- 게시글 수정: `/articles/:id/edit` (로그인 필요)

## 토큰 처리

- 로그인 성공 시 `accessToken`/`refreshToken`을 `localStorage`에 저장합니다.
- 요청 401 응답 시 자동으로 `/api/auth/reissue`로 재발급을 시도하고, 성공 시 원 요청을 1회 재시도합니다.
- 로그아웃 시 서버 `POST /api/auth/logout` 호출 후 토큰을 제거합니다.

## 디자인/상태 관리

- 디자인 시스템: `src/styles.css`의 CSS 변수 기반 라이트/다크 테마, 카드/버튼/입력 등 기본 컴포넌트 제공
- 테마: `ThemeProvider`에서 전역 관리(`localStorage: theme`), 네비게이션에서 토글 가능
- 토스트: `ToastProvider` + `useToast()`로 성공/에러 메시지 노출
- 로딩 오버레이: `LoadingProvider` + `useLoading().withLoading()`로 비동기 처리 중 화면 오버레이 스피너 표시
- 에러 경계: `ErrorBoundary`로 렌더링 중 오류 차단 및 메시지 표시

## CORS/서버 구성 안내(서버 코드는 변경하지 않음)

- 개발에서는 Vite 프록시로 동일 오리진 호출을 사용하므로 서버 CORS 설정 없이도 동작합니다.
- 운영 배포 시에는 서버에서 CORS를 명시적으로 허용하세요.
- 토큰 재발급: 현재 컨트롤러에서 `/api/auth/reissue`에 `@PreAuthorize` 설정이 있을 경우 만료로 401이 난 상태에서 재발급이 막힐 수 있습니다. 일반적으로 재발급 엔드포인트는 `refreshToken`만으로 접근 가능해야 하므로 보안 정책을 점검해 주세요.
- 응답 포맷 일관성: 일부 엔드포인트는 문자열(plain text)을 반환합니다(`signup`, `logout`, `update`, `delete`). 프론트는 처리하도록 구현되어 있으나, 운영 환경에서는 JSON으로 일관화하는 것을 권장합니다.

## 빌드

```
npm run build
npm run preview
```
