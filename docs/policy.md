# 정책

권한·계정·삭제·AI 비용의 invariant를 이 문서가 소유한다. Endpoint/DTO/status는 [API 명세](./api/README.md), schema column ownership은 [DB Schema Ownership](./database/schema-ownership.md), 모듈 경계와 요청 흐름은 [architecture](./architecture/module-dependencies.md)·[요청 흐름](./architecture/flows.md)을 따른다.

현재 PostForge는 뉴스를 수집하고 분류한 뒤 LLM으로 가공한 초안을 자동 게시하는 정보 서비스다. 현재 개별 자동 게시 범위인 출시 뉴스는 `PRODUCT_LAUNCH_NEWS`, 전날 출시 뉴스를 분야별로 종합한 글은 매일 06:00(Asia/Seoul)에 `DAILY_DIGEST`로 게시한다. 메일 구독은 향후 범위이며 현재 권한이나 데이터 보장을 갖지 않는다.

## Access

> Current: 공개 게시판의 Guest/Member/Admin 권한, 좋아요/댓글, 시스템 자동 게시.
> Target: 회원 탈퇴 API와 분야별 메일 구독.

PostForge 접근 정책은 공개 뉴스·데일리 조회, 회원의 게시판 참여·AI 채팅, 운영자의 수집·게시 작업을 구분한다.
권한 판단은 닉네임이나 로그인 `username`이 아니라 인증된 `account_id`를 기준으로 한다.

### Current Invariants

- 출시 뉴스 게시글의 출처(reference) 링크는 인증 여부와 무관하게 공개한다.
- 권한과 소유권은 인증된 `account_id`로 판단한다.
- 게시글 조회는 AI를 호출하지 않는다.

### Guest

- 회원가입과 로그인을 할 수 있다.
- 공개 게시글 목록·상세·댓글을 조회할 수 있다.
- 출시 뉴스 게시글의 출처(reference) 링크를 볼 수 있다.
- 좋아요, 댓글, 게시글 작성, AI 기능은 사용할 수 없다.

### Member

- 공개 게시글을 작성하고 본인 게시글을 수정·삭제할 수 있다.
- 댓글과 좋아요를 사용할 수 있다.
- 본인 계정을 조회하고 닉네임·비밀번호를 변경할 수 있다.
- `/api/ai/**` 기능을 명시적으로 실행할 수 있다.

### Admin

- 공개 게시글과 댓글을 수정·삭제할 수 있다.
- gated launch-news 수동/backfill 게시를 실행할 수 있다. 자동 발행은 system batch scheduler가 `SYSTEM_BATCH` origin으로 실행한다.
- launch-news 수동 게시를 실행하고, tracked_keywords를 DB에서 직접 관리해 자동 수집을 켜고 끌 수 있다.
- 일반 회원의 개인정보를 변경하는 권한은 이 정책에 포함하지 않는다.

### Read Boundary

- 공개 게시글 목록·상세·댓글과 출시 뉴스 게시글의 출처(reference) 링크는 Guest에게 공개한다.
- 삭제되거나 숨김 처리된 리소스는 일반 조회 결과에서 제외한다.
- 회원별 좋아요 여부는 인증된 회원에게만 계산한다.

### Write Boundary

- 공개 게시글/댓글/좋아요 쓰기는 Member 이상만 가능하다.
- 댓글 수정은 작성자(`comments.account_id`) 또는 Admin만 할 수 있다.
- AI 기능은 인증된 Member/Admin의 명시적 요청에서만 실행한다.
- ingest 운영 액션은 Admin만 가능하다.
- PRODUCT_LAUNCH_NEWS 자동 발행은 batch/admin/system 경로로만 가능하고 사용자의 조회 요청에서 실행하지 않는다.

### Target

- 회원 탈퇴 권한은 [Account](#target-withdrawal-and-retention)의 target policy를 따른다.
- 향후 메일 구독을 구현하면 Member는 본인의 구독만 읽고 쓴다. 아직 구독 API와 저장 schema는 없다.
- 구독 방향과 현재 자동 게시 범위는 [ADR-005](./decisions/adr-005-subscription-information-service.md)를 따른다.

## Account

> Current status: 계정/로그인/프로필/닉네임/비밀번호 변경은 현재 구현 범위다.
> 회원 탈퇴는 target policy다. 탈퇴 API는 아직 구현되지 않았고, 계정 상태 enum은 `ACTIVE`/`SUSPENDED`/`DELETED`로 준비되어 있다.

PostForge 계정 정책은 회원가입, 로그인, 프로필, 닉네임 변경, 비밀번호 변경, 회원 탈퇴를 다룬다.
외부 API의 로그인 식별자 이름은 `username`, 저장 column은 `accounts.username`이다. 인증 이후 계정 식별과 소유권 판단은 `accounts.id` 기반 `accountId`를 기준으로 한다. 인증 runtime 구조는 [Authentication Architecture](./architecture/authentication.md)를 따른다.

### Current: Registration

- 비회원은 이메일 인증을 완료한 뒤 회원가입할 수 있다.
- `username`, 이메일, 닉네임은 중복될 수 없다.
- 일반 계정의 비밀번호는 단방향 해시로 저장한다.
- OAuth 계정은 provider와 provider user id 조합으로 식별한다.
- 가입한 계정은 기본 회원 권한을 가진다.

### Current: Login

- 비회원은 일반 로그인 또는 OAuth 로그인을 할 수 있다.
- 로그아웃은 서버에 저장된 refresh token을 무효화한다.
- 반복 실패와 과도한 로그인 시도는 rate limit과 잠금 정책으로 제한한다.

### Current: Profile

- 회원은 본인 프로필만 조회할 수 있다.
- 프로필 응답 필드 집합의 정본은 [API 명세 Auth](./api/README.md#auth)의 `AccountResponse`·`ProfileResponse`다.
- 비밀번호 해시, refresh token, provider access token 같은 민감 정보는 응답하지 않는다.

### Current: Nickname

- 회원은 본인 닉네임을 변경할 수 있다.
- 닉네임은 전체 계정에서 중복될 수 없다.
- 닉네임 변경은 이후 계정 응답과 새 토큰 발급에 반영한다.
- 기존 게시글과 댓글에 저장된 작성자 닉네임 스냅샷은 변경하지 않는다.
- 닉네임은 표시 이름이며 권한 판단 기준이 아니다.

### Current: Password

- 일반 계정은 현재 비밀번호 검증 후 비밀번호를 변경할 수 있다.
- 새 비밀번호는 저장 전에 단방향 해시로 암호화한다.
- OAuth 전용 계정은 로컬 비밀번호가 없으므로 비밀번호 변경을 허용하지 않는다.
- 비밀번호 변경 후에는 기존 refresh token을 무효화하고 재로그인을 요구한다.

### Target: Withdrawal And Retention

- 회원은 본인 계정만 탈퇴할 수 있다.
- 탈퇴는 계정을 `AccountStatus.DELETED` 상태로 바꾸는 soft delete로 처리한다.
- 탈퇴 시 저장된 refresh token을 삭제한다.
- 탈퇴한 계정의 `username`, 이메일, 닉네임, provider identity는 재사용하지 않는다.
- 탈퇴 계정은 로그인, 토큰 재발급, 프로필 조회, 게시글/댓글 작성, 좋아요 변경을 할 수 없다.
- 탈퇴 후에도 기존 게시글과 댓글의 작성자 스냅샷은 보존한다.
- 보존 의무가 끝난 개인정보는 별도 정리 작업에서 익명화하거나 삭제한다.

## Delete

> Current status: 게시글/댓글/좋아요 삭제는 현재 구현과 연결된다. 다만 현재 구현은 게시글/댓글을 물리 삭제(hard delete)하며, soft delete 전환은 target policy다.
> 계정 탈퇴는 `AccountStatus.DELETED` soft delete로 설계하지만 탈퇴 API는 아직 구현되지 않았다.

PostForge 삭제 정책은 일반 사용자에게 리소스를 더 이상 노출하지 않는 것을 기준으로 한다.

### Post

- 게시글 삭제는 작성자 또는 관리자만 할 수 있다.
- 현재 구현은 게시글 row를 물리 삭제하며, 댓글/태그는 cascade로 함께 삭제되고 파일 연결과 Redis 조회수 캐시는 삭제 시점에 정리한다.
- 삭제된 게시글은 목록, 검색, 정렬, 상세 조회에서 제외한다.
- 삭제된 게시글의 댓글과 좋아요도 일반 사용자에게 노출하지 않는다.
- 삭제된 게시글의 `post_reference_links`는 일반 응답에서 사용하지 않는다.
- 게시글에 연결된 파일은 게시글에서 분리하고, 참조되지 않는 파일은 별도 정리 작업의 대상이 된다.
- 게시글의 조회수, 좋아요 수, 댓글 수 같은 파생 데이터는 삭제 후 일반 응답에 사용하지 않는다.

### Comment

- 댓글 삭제는 작성자 또는 관리자만 할 수 있다.
- 삭제된 댓글은 댓글 조회 결과에서 제외한다.
- 부모 댓글을 삭제하면 하위 댓글도 일반 조회에서 제외한다.
- 댓글 삭제는 게시글 자체를 삭제하지 않는다.
- 댓글 수와 댓글 좋아요 수는 삭제 상태를 반영해 다시 계산하거나 갱신한다.

### Like

- 좋아요 취소는 인증된 회원만 할 수 있다.
- 회원은 본인이 생성한 게시글 좋아요 또는 댓글 좋아요만 취소할 수 있다.
- 좋아요 취소는 좋아요 관계를 제거하고 대상의 `like_count` 파생 데이터를 갱신한다.
- 이미 취소된 좋아요 취소 요청은 멱등하게 처리한다.

### Visibility

- 삭제된 리소스는 식별자를 재사용하지 않는다.
- 삭제된 리소스는 일반 사용자에게 존재하지 않는 리소스처럼 보인다(404).
- 운영 감사나 복구를 위한 내부 조회는 일반 사용자 API와 분리한다.
- soft delete로 전환하면 물리 삭제는 별도 보존 기간과 정리 작업으로 처리한다.

## AI Cost

> Current: AI chat, 뉴스·데일리 요약 draft 생성, public read path AI 호출 금지, deterministic pre-gate, shared `TextGenerationClient` metric/log.

PostForge의 AI 정책은 기능보다 비용 통제를 우선한다. AI는 모든 요청의 기본 동작이 아니라 명시적으로 실행되는 작업이다.

### Current Invariants

- 게시글 목록·상세·댓글 조회는 AI를 호출하지 않는다.
- 사용자 AI 기능은 인증된 사용자가 채팅을 명시적으로 요청할 때만 실행한다.
- 출시 뉴스 AI draft는 [통합 API 명세의 Ingest](./api/README.md#ingest) gate를 통과한 batch/admin/system write flow에서만 실행한다. 운영 키워드의 분야 분류는 `board_category`에 기록한다.
- 데일리 요약 AI draft는 전날 `posts`(`PRODUCT_LAUNCH_NEWS`)를 분야별로 읽어 `posts`(`DAILY_DIGEST`)로 쓰는 system/admin write flow에서만 실행한다. 기본 자동 실행 시각은 매일 06:00(Asia/Seoul)이다.
- 수집된 모든 item을 AI 호출이나 공개 게시글로 연결하지 않는다.
- 현재 호출은 shared `TextGenerationClient`의 metric/log 대상이다.

### Current AI Boundary

AI를 사용하지 않는 검색과 모델 생성 요청을 구분한다.

- DB/vector 검색, 관련 자료 조회, 출처 링크 추천은 AI 호출이 아니다.
- 채팅 답변, 내부 출시 뉴스 draft 생성, 데일리 요약 draft 생성은 AI 호출이다.
- 출시 뉴스 생성 결과는 별도의 admin/system gate와 write flow를 통과해야 공개 게시글이 된다.
