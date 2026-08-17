# Access Policy

> Current: 공개 게시판의 Guest/Member/Admin 권한, 좋아요/댓글/투표/가격 판정.
> Target: 무료/유료 plan, private workspace/draft, saved trend bundle, AI quota, `UNLISTED` content, 회원 탈퇴 API.

PostForge 접근 정책은 공개 게시판, 개인 작업공간, AI 보조 기능을 분리한다.
권한 판단은 닉네임이나 로그인 `username`이 아니라 인증된 `account_id`를 기준으로 한다.

> 문서 경계: 이 문서는 권한과 노출 invariant만 정의한다. Endpoint/DTO/status는 `../api/`, module ownership은 `../architecture/`, 유스케이스별 read/write 데이터는 `usecase-data-policy.md`를 따른다.

## Current Invariants

- 공개 게시판의 근거 링크, 관련 트렌드, 출처 정보는 인증 여부와 무관하게 공개한다.
- 권한과 소유권은 인증된 `account_id`로 판단한다.
- 게시글 상세와 관련 트렌드 조회는 AI를 호출하지 않는다.

## Guest

- 회원가입과 로그인을 할 수 있다.
- 공개 게시글 목록·상세·댓글을 조회할 수 있다.
- 공개 게시글의 evidence/trend 정보와 출시 뉴스의 출처 링크·구매 판단 투표 집계를 볼 수 있다.
- 좋아요, 댓글, 게시글 작성, 구매 판단 투표, 가격 판정, AI 기능은 사용할 수 없다.

## Member

- 공개 게시글을 작성하고 본인 게시글을 수정·삭제할 수 있다.
- 댓글과 좋아요를 사용할 수 있다.
- `PRODUCT_LAUNCH_NEWS`이면서 `publish_origin=SYSTEM_BATCH`인 게시글에 구매 판단 투표를 할 수 있다.
- `POST /api/price-checks`로 response-only 가격 판정을 받을 수 있다.
- 본인 계정을 조회하고 닉네임·비밀번호를 변경할 수 있다.
- `/api/ai/**` 기능을 명시적으로 실행할 수 있다.

## Admin

- 공개 게시글과 댓글을 수정·삭제할 수 있다.
- 상품 source 수집, tracked keyword, collection job, product matching candidate를 관리할 수 있다.
- gated launch-news 수동/backfill 게시를 실행할 수 있다. 자동 발행은 system batch scheduler가 `SYSTEM_BATCH` origin으로 실행한다.
- PRODUCT_LAUNCH_NEWS 생성 후보를 승인하거나 중지할 수 있다.
- 일반 회원의 개인정보를 변경하는 권한은 이 정책에 포함하지 않는다.

## Read Boundary

- 공개 게시글 목록·상세·댓글과 evidence/trend 정보는 Guest에게 공개한다.
- 삭제되거나 숨김 처리된 리소스는 일반 조회 결과에서 제외한다.
- 회원별 좋아요 여부와 `myVote`는 인증된 회원에게만 계산한다.
- 구매 판단 투표 집계는 공개할 수 있다.

## Write Boundary

- 공개 게시글/댓글/좋아요 쓰기는 Member 이상만 가능하다.
- 구매 판단 투표 쓰기는 Member 이상만 가능하며, `PRODUCT_LAUNCH_NEWS`이면서 `publish_origin=SYSTEM_BATCH`인 게시글에만 허용한다.
- 가격 판정 API는 결과를 저장하지 않지만 상품/가격 입력을 받으므로 Member/Admin write-security bucket에 둔다.
- AI 기능은 인증된 Member/Admin의 명시적 요청에서만 실행한다.
- 상품 source/ingest 운영 액션과 product matching candidate 결정은 Admin만 가능하다.
- PRODUCT_LAUNCH_NEWS 자동 발행은 batch/admin/system 경로로만 가능하고 사용자의 조회 요청에서 실행하지 않는다.

## Error Rules

- 인증이 필요한 요청에 인증 정보가 없거나 유효하지 않으면 `401 Unauthorized`로 처리한다.
- 인증은 되었지만 권한 또는 소유권 조건을 만족하지 못하면 `403 Forbidden`으로 처리한다.
- 삭제되었거나 일반 조회에서 제외된 리소스는 일반 사용자에게 `404 Not Found`와 동일하게 다룬다.

## Target: Plan And Private Workspace

플랜은 공개 게시판의 신뢰 신호가 아니라 개인 생산성 한도에만 영향을 준다.

- Free Member는 제한된 private workspace/draft, saved trend bundle, AI trial을 사용할 수 있다.
- Paid Member는 더 많은 private draft/report와 trend bundle, AI quota, export 기능을 사용할 수 있다.
- private resource는 owner 또는 workspace member만 조회·변경할 수 있다.
- Admin은 moderation 권한만으로 private workspace/draft를 조회하지 않는다.
- `UNLISTED` content는 직접 링크와 권한 조건을 모두 만족할 때만 조회한다.
- AI assist는 plan/quota와 budget window를 통과해야 하며, 초과 응답은 별도의 quota error contract로 정의한다.
- 회원 탈퇴 권한은 [Account Policy](./account-policy.md)의 target policy를 따른다.

## Related Policies

- 삭제와 삭제 후 노출 규칙은 [Delete Policy](./delete-policy.md)를 따른다.
- 회원가입, 로그인, 프로필, 닉네임/비밀번호 변경, 회원 탈퇴는 [Account Policy](./account-policy.md)를 따른다.
- AI/API 비용 제어는 [AI Cost Policy](./ai-cost-policy.md)를 따른다.
- 유스케이스별 Read/Write 데이터 경계는 [Use Case Data Policy](./usecase-data-policy.md)를 따른다.
