# PostForge Frontend

당근마켓 스타일의 중고거래 커뮤니티 프론트엔드

## 기술 스택

- **Framework**: React 18
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **Routing**: React Router DOM
- **State Management**: Zustand
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Form Handling**: React Hook Form

## 주요 기능

### 인증 기능
- 회원가입 (유효성 검증 포함)
- 로그인/로그아웃
- JWT 토큰 기반 인증
- 자동 토큰 갱신

### 게시글 기능
- 게시글 목록 조회 (페이징)
- 게시글 상세 조회 (조회수 자동 증가)
- 게시글 작성/수정/삭제
- 게시글 좋아요

### 댓글 기능
- 댓글 작성/수정/삭제
- 대댓글 (2단계까지)
- 댓글 좋아요

## 시작하기

### 환경 변수 설정

`.env` 파일을 생성하고 백엔드 API URL을 설정하세요:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 설치 및 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 프로덕션 빌드
npm run build

# 프로덕션 빌드 미리보기
npm run preview
```

개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.

## 프로젝트 구조

```
front/
├── src/
│   ├── api/              # API 클라이언트
│   │   ├── axios.js      # Axios 인스턴스 설정
│   │   ├── auth.js       # 인증 API
│   │   ├── posts.js      # 게시글 API
│   │   └── comments.js   # 댓글 API
│   ├── components/       # 재사용 가능한 컴포넌트
│   │   ├── Header.jsx
│   │   ├── Layout.jsx
│   │   ├── PostCard.jsx
│   │   └── CommentSection.jsx
│   ├── pages/           # 페이지 컴포넌트
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── PostList.jsx
│   │   ├── PostDetail.jsx
│   │   └── PostForm.jsx
│   ├── store/           # 상태 관리
│   │   └── authStore.js
│   ├── utils/           # 유틸리티 함수
│   │   └── dateUtils.js
│   ├── App.jsx          # 라우팅 설정
│   ├── main.jsx         # 엔트리 포인트
│   └── index.css        # 글로벌 스타일
├── .env                 # 환경 변수
├── tailwind.config.js   # Tailwind 설정
└── package.json
```

## API 엔드포인트

### 인증
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `POST /api/auth/reissue` - 토큰 갱신

### 게시글
- `GET /api/posts` - 게시글 목록 조회
- `GET /api/posts/:id` - 게시글 상세 조회
- `POST /api/posts` - 게시글 작성
- `PUT /api/posts/:id` - 게시글 수정
- `DELETE /api/posts/:id` - 게시글 삭제
- `POST /api/posts/:id/like` - 게시글 좋아요 토글

### 댓글
- `GET /api/posts/:postId/comments` - 댓글 목록 조회
- `POST /api/posts/:postId/comments` - 댓글 작성
- `PUT /api/posts/:postId/comments/:id` - 댓글 수정
- `DELETE /api/posts/:postId/comments/:id` - 댓글 삭제
- `POST /api/posts/:postId/comments/:id/like` - 댓글 좋아요 토글

## 디자인 시스템

### 컬러 팔레트

당근마켓에서 영감을 받은 따뜻한 오렌지 컬러를 사용합니다:

- **Primary (Carrot)**: `#ff6f0f`
- **Primary Light**: `#ffe8d6`
- **Primary Dark**: `#cc4900`
- **Background**: `#f9fafb` (Gray 50)
- **Text**: `#111827` (Gray 900)

### 컴포넌트 스타일

Tailwind CSS의 `@layer components`를 활용한 재사용 가능한 클래스:

- `.btn-primary` - 주요 액션 버튼 (오렌지)
- `.btn-secondary` - 보조 버튼 (화이트)
- `.card` - 카드 컨테이너
- `.input-field` - 입력 필드

## 주요 기능 설명

### 인증 상태 관리

Zustand를 사용하여 전역 인증 상태를 관리하며, localStorage와 동기화됩니다:

```javascript
const { user, isAuthenticated, login, logout } = useAuthStore();
```

### 자동 토큰 갱신

Axios 인터셉터를 통해 401 에러 발생 시 자동으로 토큰을 갱신합니다.

### 보호된 라우트

`ProtectedRoute` 컴포넌트를 통해 인증이 필요한 페이지를 보호합니다.

### 댓글 계층 구조

댓글과 대댓글(최대 2단계)을 트리 구조로 표시합니다.

## 개발 가이드

### 새로운 페이지 추가

1. `src/pages/` 에 컴포넌트 생성
2. `src/App.jsx`에 라우트 추가
3. 필요시 `ProtectedRoute`로 감싸기

### 새로운 API 추가

1. `src/api/` 에 API 함수 추가
2. Axios 인스턴스(`apiClient`) 사용
3. 에러 핸들링 구현

### 스타일링

Tailwind CSS 유틸리티 클래스를 우선 사용하고, 재사용 가능한 스타일은 `index.css`의 `@layer components`에 추가합니다.

## 트러블슈팅

### CORS 에러

백엔드 서버에서 CORS 설정이 필요합니다:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
```

### 토큰 만료

토큰이 만료되면 자동으로 갱신을 시도하며, 실패 시 로그인 페이지로 리다이렉트됩니다.

## 라이선스

MIT
