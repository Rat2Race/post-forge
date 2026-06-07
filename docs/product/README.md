# Product Direction

PostForge의 제품 방향은 상품 중심 커뮤니티다. 외부 쇼핑 API에서 수집한 상품 정보를 정규화하고 가격 이력을 쌓은 뒤, 그 데이터를 상품 게시판, AI/system 자동 게시글, 회원 커뮤니티 활동, 유료 AI assist로 연결한다.

이 문서는 제품 방향 문서다. 현재 구현된 백엔드 기반과 앞으로 만들 제품 경험을 의도적으로 분리해 적는다.

## Product Concept

PostForge는 다음 흐름을 목표로 한다.

```text
external shopping API/source data
-> source request control
-> ingest collection job/raw product
-> catalog product/category normalization
-> price snapshot and lowest-price read model
-> AI/system-authored product posts
-> member-authored community discussion
-> paid AI-assisted product search/comparison
```

핵심은 "상품을 중심에 둔 게시판"이다. 게시글은 단순 자유게시판 글이 아니라 상품, 카테고리, 가격 변화, 구매 고려사항과 연결된다.

## Current Implementation

현재 구현은 목표 제품을 위한 백엔드 기반에 가깝다.

| Area | Current State |
| --- | --- |
| Community core | 게시글, 댓글/대댓글, 좋아요, 조회수, 파일 업로드, 작성자 소유권 검증 |
| Auth core | JWT, Redis refresh token, OAuth2, 이메일 인증, 로그인 보호 |
| Product ingestion | source API 경계, tracked keyword, collection job, raw product 저장 |
| Catalog | 정규화 상품, 카테고리, offer, 상품 embedding, 유사 상품 후보 |
| Price tracking | 가격 snapshot, 최신 최저가 read model, 가격 하락 조회 |
| AI foundation | Spring AI/OpenAI/PgVector 기반 채팅, 문서 검색, draft generation 경계 |
| Product/community link | 상품-게시글 연결과 AI/system 자동 게시 draft 저장 경계 |

현재 구현은 운영 가능한 API와 데이터 경계를 만드는 데 집중한다. 최종 사용자 화면, 결제, plan 관리, 완성된 개인화 AI 검색 UX는 아직 제품 방향에 속한다.

## Future Product Direction

앞으로 만들 제품 경험은 다음을 포함한다.

### Product And Category Boards

- 상품별 게시판에서 가격 변화, 구매 후기, 질문, 관련 게시글을 모아 보여준다.
- 카테고리별 게시판은 비슷한 상품군의 비교와 추천 흐름을 제공한다.
- 회원은 상품과 연결된 게시글, 댓글, 좋아요 같은 일반 커뮤니티 기능을 사용할 수 있다.

### External Collection And Automatic Posting

- 외부 API 수집은 source policy와 collection job을 통해 통제한다.
- 수집된 raw product는 catalog 상품과 offer로 정규화된다.
- 가격 변화나 의미 있는 상품 이벤트가 감지되면 AI/system이 자동 게시글 draft를 만들 수 있다.
- 자동 게시 생성은 batch/admin/event 흐름에서 실행하고, 일반 조회 요청에서 즉시 외부 API나 AI를 호출하지 않는다.

### Price Tracking

- 사용자는 상품 상세에서 가격 변화를 한눈에 볼 수 있어야 한다.
- 최저가, 가격 하락, 가격 snapshot 이력은 상품 탐색의 핵심 근거가 된다.
- 가격 추적 데이터는 회원 게시글과 AI/system 게시글의 근거로 연결될 수 있다.

### AI/System Authorship

콘텐츠 작성 주체는 명확히 구분한다.

| Authorship | Meaning |
| --- | --- |
| Member-authored | 회원이 직접 작성한 게시글/댓글 |
| AI/system-authored | 수집 상품 정보, 가격 변화, 외부 출처를 바탕으로 system job 또는 admin flow가 생성한 게시글 |
| Admin-curated | 운영자가 승인, 숨김, 고정, 수정한 상품/게시글 |

AI/system-authored 콘텐츠는 member-authored 콘텐츠처럼 보이면 안 된다. 응답 metadata, 표시 label, 정책 문서에서 작성 주체를 구분할 수 있어야 한다.

## Membership Access Model

무료/유료 차이는 공개 커뮤니티 신뢰 신호가 아니라 on-demand AI assist 사용 여부에 둔다.

| Actor | Product Access |
| --- | --- |
| Guest | 공개 회원 작성글, 공개 상품 요약, 가입/로그인 진입 |
| Free Member | 공개 커뮤니티 쓰기, 댓글/좋아요, 이미 생성된 AI/system 상품 콘텐츠, 가격 추적 정보 |
| Paid Member | Free 권한 + 원하는 상품/카테고리에 대한 on-demand AI 검색, 비교, 맞춤 정보 분석 |
| Admin/System | 상품 수집 정책, collection job, 자동 게시 draft 생성/승인/숨김 |

Free Member가 볼 수 있는 AI/system 상품 콘텐츠와 Paid Member가 실행하는 on-demand AI assist는 다르다. 전자는 이미 생성되어 공개된 콘텐츠 접근이고, 후자는 사용자의 요청에 따라 새 검색/수집/분석 비용이 발생하는 기능이다.

자세한 권한 경계는 [Access Policy](../policy/access-policy.md)를 따른다.

## Technical Evidence Boundary

성능, Docker, k6, 테스트 결과 문서는 제품 방향 본문이 아니라 포트폴리오 기술 증빙이다. 이 문서는 해당 자료를 다시 쓰지 않는다.

- [Performance Reports](../performance/README.md)
- [Docker Docs](../docker/README.md)
- [Test Summary](../test-summary.md)

제품 방향 문서는 무엇을 만들지 설명하고, 기술 증빙 문서는 그동안 어떤 운영/성능/인프라 검증을 했는지 설명한다.
