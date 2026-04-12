# PostForge - AI 기반 한국 주식 분석 커뮤니티

PostForge는 AI 기반 한국 주식 분석 커뮤니티 플랫폼입니다. 실시간 공시 분석, AI 생성 리포트, 사용자 간 토론을 통해 데이터 기반 투자 인사이트를 제공합니다.

## 🎨 디자인 시스템

### 컬러 팔레트
- **Background**: 따뜻한 종이 느낌 (#faf9f6)
- **Foreground**: 깊은 잉크 색상 (#1a1613)
- **Positive**: 차분한 에메랄드 (#4a7c59) - 상승 시그널
- **Negative**: 차분한 코랄 (#c76b5e) - 하락 시그널
- **Brass**: 황동 악센트 (#b8956a) - AI 및 프리미엄 요소

### 타이포그래피
- **본문/UI**: Noto Sans KR - 깔끔한 가독성
- **제목**: Noto Serif KR - 편집자적 권위감

### 디자인 컨셉
- 금융 저널과 데이터 인텔리전스의 조화
- 깔끔하지만 차갑지 않은, 프리미엄하지만 과장되지 않은
- 미묘한 그리드와 질감으로 편집적 느낌

## 📁 프로젝트 구조

```
/src/app
├── App.tsx                    # 루트 컴포넌트
├── routes.tsx                 # 라우팅 설정
├── layouts/
│   └── root-layout.tsx        # 헤더/푸터 레이아웃
├── pages/
│   ├── home.tsx               # 공개 피드
│   ├── post-detail.tsx        # 게시글 상세
│   ├── new-post.tsx           # 새 글 작성
│   ├── edit-post.tsx          # 글 수정
│   ├── login.tsx              # 로그인
│   ├── register.tsx           # 회원가입
│   ├── oauth-callback.tsx     # OAuth 콜백
│   ├── profile.tsx            # 프로필 관리
│   ├── ai-chat.tsx            # AI 대화
│   ├── ai-generate.tsx        # AI 분석 생성
│   └── not-found.tsx          # 404 페이지
└── components/
    ├── post-card.tsx          # 게시글 카드
    ├── comment-thread.tsx     # 댓글 스레드
    ├── attachment-pill.tsx    # 첨부파일 표시
    ├── signal-badge.tsx       # 시그널 배지
    └── source-chip.tsx        # 출처 칩
```

## 🛣️ 라우팅 구조

### 공개 라우트
- `/` - 홈 및 검색 가능한 게시글 피드
- `/login` - 로그인
- `/register` - 회원가입
- `/oauth2/callback` - OAuth 처리

### 인증 필요 라우트
- `/posts/:id` - 게시글 상세 (비로그인 시 로그인 유도)
- `/posts/new` - 새 글 작성
- `/posts/:id/edit` - 글 수정
- `/profile` - 프로필 관리
- `/ai/chat` - AI 대화
- `/ai/generate` - AI 분석 생성

## 🎯 주요 기능

### 게시글 시스템
- **공개 피드**: 제목, 요약, 태그, 작성자, 조회수, 댓글 수, 좋아요 수
- **상세 페이지**: 마크다운 본문, 첨부파일, 출처, 시그널 배지
- **파일 첨부**: JPG, JPEG, PNG, GIF, PDF (최대 10MB)
- **태그 시스템**: 종목명, 산업, 분석 유형
- **시그널**: 상승/하락/중립/경고

### 댓글 시스템
- 최상위 댓글 + 1단계 답글만 지원
- AI 게시글의 최상위 댓글에 "AI에게 답글 요청" 기능
- 좋아요 기능

### 인증 시스템
- 아이디/비밀번호 로그인
- 이메일 회원가입 + 이메일 인증
- 소셜 로그인: Google, Naver, Kakao
- 리프레시 쿠키 기반 인증

### 프로필 관리
- userId, nickname, provider, roles
- 닉네임 변경
- 비밀번호 변경 (OAuth 사용자는 비활성화)
- 활동 통계

### AI 기능
- **AI 대화**: 주식/공시 관련 질문
- **AI 분석 생성**: 
  - 종목 코드 입력
  - 진행 상태 표시
  - 성공 시 게시글로 발행
  - 작성자: "AI 분석가"

### AI 생성 게시글 구조
- 핵심 요약
- 공시 분석
- 시장 반응
- 뉴스 동향
- 방향성 판단
- 상충 신호
- 주의점
- 출처

## 🎨 재사용 가능한 컴포넌트

### PostCard
게시글 미리보기 카드 - 피드에서 사용

### CommentThread
댓글 스레드 - 최상위 + 1단계 답글, AI 답글 요청

### AttachmentPill
첨부 파일 표시 - 업로드 진행 상태, 다운로드, 삭제

### SignalBadge
시그널 표시 - positive/negative/neutral/warning

### SourceChip
출처 링크 - 공시/뉴스/보고서/기타

## 📱 반응형 디자인

- **Desktop**: 최대 너비 레이아웃, 사이드바, 풀 네비게이션
- **Mobile**: 전체 너비, 햄버거 메뉴, 스택 레이아웃
- 모든 페이지 모바일 최적화

## 🚫 제외된 기능 (명시적으로 구현하지 않음)

- 관심 종목 목록 (watchlist)
- 포트폴로오 추적
- 실시간 차트
- 다이렉트 메시지
- 알림 센터
- 관리자 콘솔
- CMS 도구
- 아바타 업로드
- AI 초안/미리보기 토글 (생성 즉시 발행)

## 🎭 데모 데이터

프로젝트는 실제 백엔드 없이 작동하도록 모든 페이지에 모의 데이터가 포함되어 있습니다:
- 샘플 게시글 (삼성전자, SK하이닉스, NAVER, 카카오 등)
- 샘플 댓글 및 답글
- AI 생성 콘텐츠 예시
- OAuth 흐름 시뮬레이션

## 🚀 시작하기

이 프로젝트는 Vite + React + TypeScript로 구성되어 있습니다.

```bash
# 개발 서버 시작
npm run dev

# 프로덕션 빌드
npm run build
```

## 📝 참고사항

- 모든 날짜는 한국어로 표시 (예: "2시간 전", "2026.04.11")
- 숫자는 한국 로케일로 포맷 (예: 12,847)
- 모든 UI 텍스트는 한국어
- AI 관련 면책 조항 포함
