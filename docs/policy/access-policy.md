# Access Policy

PostForge 접근 정책은 공개 게시판, 개인 작업공간, AI 보조 기능을 분리한다.
권한 판단은 닉네임이나 로그인 `userId`가 아니라 인증된 `account_id`를 기준으로 한다.

## Core Rule

무료/유료 플랜은 공개 게시판의 신뢰 신호를 나누지 않는다.
공개 게시글의 근거 링크, 관련 트렌드, 출처 정보는 플랜에 따라 숨기지 않는다.

플랜 차이는 다음 영역에만 둔다.

- 개인 작업공간 용량
- 비공개 draft/report 개수
- 저장 가능한 trend bundle 개수
- AI 작성 보조 quota
- export 같은 생산성 기능

## Guest

비회원은 공개 게시판을 읽고 가입/로그인을 시작할 수 있다.

- 회원가입과 로그인을 할 수 있다.
- 공개 게시글 목록을 조회할 수 있다.
- 공개 게시글 상세를 조회할 수 있다.
- 공개 게시글의 공개 evidence/trend 정보는 볼 수 있다.
- 개인 workspace, draft, AI assist는 사용할 수 없다.
- 좋아요, 댓글, 게시글 작성은 할 수 없다.

## Free Member

무료 회원은 공개 커뮤니티 활동과 제한된 개인 작업공간을 사용할 수 있다.

- 공개 게시글을 작성, 수정, 삭제할 수 있다.
- 댓글과 좋아요를 사용할 수 있다.
- 본인 계정을 조회/변경/탈퇴할 수 있다.
- 기본 private workspace와 제한된 draft를 사용할 수 있다.
- DB 기반 관련 트렌드/출처 추천을 사용할 수 있다.
- AI 작성 보조는 기본적으로 사용할 수 없거나 매우 제한된 quota만 가진다.

## Paid Member

유료 회원은 공개 게시판에서 더 높은 신뢰 권한을 얻는 것이 아니라, 개인 생산성 기능을 더 많이 사용할 수 있다.

- Free Member의 모든 권한을 가진다.
- 더 많은 private draft/report를 저장할 수 있다.
- 더 많은 saved trend bundle을 사용할 수 있다.
- quota 내에서 AI 작성 보조를 명시적으로 실행할 수 있다.
- report export 같은 고급 기능을 사용할 수 있다.

## Admin

관리자는 운영 목적의 예외 권한을 가진다.

- 공개 게시글과 댓글을 숨기거나 삭제할 수 있다.
- 상품 수집 job, 상품 트렌드 계산, 자동 게시 생성을 관리할 수 있다.
- 상품 트렌드 게시글 생성 후보를 승인하거나 중지할 수 있다.
- 일반 회원의 개인 workspace/draft 내용을 기본적으로 조회하지 않는다.
- 개인 정보 변경 권한은 이 정책에 포함하지 않는다.

## Read Boundary

- 공개 게시글 목록과 상세는 Guest에게 공개한다.
- 공개 게시글의 evidence/trend 정보도 Guest에게 공개한다.
- private post/report/draft는 owner 또는 workspace member만 조회한다.
- `UNLISTED` content는 직접 링크와 권한 조건을 만족할 때만 조회한다.
- 삭제되거나 숨김 처리된 리소스는 일반 조회 결과에서 제외한다.
- 회원별 좋아요 여부는 인증된 회원에게만 계산한다.
- 게시글 상세와 관련 트렌드 조회는 AI를 호출하지 않는다.

## Write Boundary

- 공개 게시글/댓글/좋아요 쓰기는 Member 이상만 가능하다.
- private draft/report 쓰기는 workspace 권한이 필요하다.
- AI assist 실행은 인증, plan/quota, budget window를 모두 통과해야 한다.
- 상품 수집 정책 변경은 Admin만 가능하다.
- 상품 트렌드 자동 발행은 batch/admin/system 경로로만 가능하고 사용자의 조회 요청에서 실행하지 않는다.

## Error Rules

- 인증이 필요한 요청에 인증 정보가 없거나 유효하지 않으면 `401 Unauthorized`로 처리한다.
- 인증은 되었지만 권한, 소유권, workspace membership 조건을 만족하지 못하면 `403 Forbidden`으로 처리한다.
- plan/quota 초과는 `402 Payment Required` 또는 제품에서 정한 quota error로 처리한다.
- 삭제되었거나 일반 조회에서 제외된 리소스는 일반 사용자에게 `404 Not Found`와 동일하게 다룬다.

## Related Policies

- 삭제와 삭제 후 노출 규칙은 [delete-policy.md](./delete-policy.md)를 따른다.
- 회원가입, 로그인, 프로필, 닉네임/비밀번호 변경, 회원 탈퇴는 [account-policy.md](./account-policy.md)를 따른다.
