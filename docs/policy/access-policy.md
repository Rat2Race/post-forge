# Access Policy

PostForge 접근 정책은 상품 중심 커뮤니티, AI/system 생성 콘텐츠, 유료 AI assist를 분리한다.
권한 판단은 닉네임이나 로그인 `userId`가 아니라 인증된 `account_id`를 기준으로 한다.

## Core Rule

무료/유료 플랜은 공개 커뮤니티 신뢰 신호를 나누지 않는다. 공개 게시글의 근거 링크, 관련 상품, 출처 정보는 플랜에 따라 숨기지 않는다.

플랜 차이는 **on-demand AI assist** 사용 여부와 quota에 둔다. 예를 들어 원하는 상품의 최저가를 AI로 찾거나, 상품 주의사항/사용법/구매 고려사항을 AI가 수집해 맞춤 분석하는 기능은 Paid Member 영역이다.

AI/system이 batch/admin/event 흐름으로 생성한 공개 상품 콘텐츠와 회원이 직접 작성한 콘텐츠는 작성 주체를 구분해 표시한다.

## Guest

비회원은 공개 게시판을 읽고 가입/로그인을 시작할 수 있다.

- 회원가입과 로그인을 할 수 있다.
- 공개 게시글 목록을 조회할 수 있다.
- 공개 회원 작성글 상세를 조회할 수 있다.
- 제품 정책에 따라 공개로 노출된 상품 요약과 가격 정보 일부를 조회할 수 있다.
- 댓글, 좋아요, 게시글 작성, AI assist, 개인화 검색은 사용할 수 없다.

## Free Member

무료 회원은 공개 커뮤니티 활동과 이미 생성된 상품 콘텐츠에 접근할 수 있다.

- 공개 게시글을 작성, 수정, 삭제할 수 있다.
- 댓글과 좋아요를 사용할 수 있다.
- 본인 계정을 조회/변경/탈퇴할 수 있다.
- AI/system이 생성해 공개된 상품 게시글에 접근할 수 있다.
- 상품 가격 추적 페이지와 가격 변동 이력을 조회할 수 있다.
- on-demand AI assist, 맞춤 상품 검색, 카테고리 비교 분석은 기본적으로 사용할 수 없다.

## Paid Member

유료 회원은 on-demand AI assist를 사용할 수 있다.

- Free Member의 모든 권한을 가진다.
- 원하는 상품의 정보를 검색/수집해 최저가와 가격 변화를 분석할 수 있다.
- 상품의 주의사항, 사용법, 구매 시 고려사항 같은 맞춤 정보를 AI로 정리할 수 있다.
- 원하는 카테고리의 상품들을 비교하고 상품 관련 질문에 대한 AI 답변을 받을 수 있다.
- AI assist는 plan/quota, budget window, source policy를 통과해야 실행된다.

## Admin

관리자와 system job은 운영 목적의 예외 권한을 가진다.

- 상품 수집 source policy와 collection job을 관리할 수 있다.
- AI/system 자동 게시 후보를 생성, 승인, 중지, 숨김 처리할 수 있다.
- 공개 게시글과 댓글을 숨기거나 삭제할 수 있다.
- 일반 회원의 private 리소스는 별도 운영/감사 정책 없이는 조회하지 않는다.

## Read Boundary

- 공개 게시글 목록과 상세는 Guest에게 공개한다.
- 공개 게시글의 근거 링크, 관련 상품, 출처 정보는 Guest에게 공개할 수 있다.
- AI/system-authored 콘텐츠와 member-authored 콘텐츠는 조회 응답이나 표시 metadata에서 구분 가능해야 한다.
- Free Member는 공개 AI/system 상품 콘텐츠와 가격 추적 정보를 조회할 수 있다.
- Paid Member는 AI assist 결과와 개인화 검색/분석 결과를 조회할 수 있다.
- private post/report/draft는 owner 또는 workspace member만 조회한다.
- `UNLISTED` content는 직접 링크와 권한 조건을 만족할 때만 조회한다.
- 삭제되거나 숨김 처리된 리소스는 일반 조회 결과에서 제외한다.
- 회원별 좋아요 여부는 인증된 회원에게만 계산한다.
- 게시글 상세와 상품 상세 조회는 기본적으로 AI를 호출하지 않는다.

## Write Boundary

- 공개 게시글/댓글/좋아요 쓰기는 Member 이상만 가능하다.
- private draft/report 쓰기는 workspace 권한이 필요하다.
- AI assist 실행은 인증, plan/quota, budget window를 모두 통과해야 한다.
- 상품 수집 정책 변경은 Admin만 가능하다.
- 상품 수집, 가격 스냅샷 갱신, AI/system 자동 게시 생성은 batch/admin/system 경로로만 가능하고 사용자의 일반 조회 요청에서 실행하지 않는다.

## Error Rules

- 인증이 필요한 요청에 인증 정보가 없거나 유효하지 않으면 `401 Unauthorized`로 처리한다.
- 인증은 되었지만 권한, 소유권, workspace membership 조건을 만족하지 못하면 `403 Forbidden`으로 처리한다.
- plan/quota 초과는 `402 Payment Required` 또는 제품에서 정한 quota error로 처리한다.
- 삭제되었거나 일반 조회에서 제외된 리소스는 일반 사용자에게 `404 Not Found`와 동일하게 다룬다.

## Related Policies

- 삭제와 삭제 후 노출 규칙은 [delete-policy.md](./delete-policy.md)를 따른다.
- 회원가입, 로그인, 프로필, 닉네임/비밀번호 변경, 회원 탈퇴는 [account-policy.md](./account-policy.md)를 따른다.
