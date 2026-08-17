# PostForge User Scenario Use Cases

> 분류: 설계·학습용 사용자 흐름 참고 문서다. API/policy/schema의 정본이나 구현 완료·포트폴리오 근거로 사용하지 않는다.
> Canonical endpoint/DTO/status는 `../api/`, invariant는 `../policy/`, schema는 `../database/`를 따른다.

## Actors

| Actor | 설명 |
| --- | --- |
| Guest | 로그인하지 않은 사용자 |
| Member | 로그인한 일반 사용자 |
| Admin | 운영 사용자 |
| System | scheduler, relay 같은 자동화 주체 |

## Scenario Index

| ID | Goal | Actor |
| --- | --- | --- |
| UC-AUTH-01 | 이메일 인증 후 회원가입 | Guest |
| UC-AUTH-02 | 로그인, 토큰 재발급, 로그아웃 | Guest, Member |
| UC-ACCOUNT-01 | 내 계정/프로필 조회와 변경 | Member |
| UC-PUBLIC-01 | 공개 상품 탐색 | Guest, Member |
| UC-PUBLIC-02 | 공개 게시판 탐색 | Guest, Member |
| UC-PUBLIC-03 | 신상품 출시 뉴스 탐색 | Guest, Member |
| UC-PRICE-01 | 상품 가격 판정 요청 | Member |
| UC-BOARD-01 | 게시글 작성/수정/삭제 | Member, Admin |
| UC-BOARD-02 | 댓글과 대댓글 작성/수정/삭제 | Member, Admin |
| UC-BOARD-03 | 게시글/댓글 좋아요와 취소 | Member |
| UC-BOARD-04 | 출시 뉴스 구매 판단 투표 | Member |
| UC-FILE-01 | 첨부 파일 URL 발급 | Member, Admin |
| UC-AI-01 | RAG 기반 AI 채팅 | Member |
| UC-ADMIN-01 | 상품 수동 등록/숨김 | Admin |
| UC-ADMIN-02 | 상품 수집 운영 | Admin, System |
| UC-ADMIN-03 | 상품 매칭 후보 검토 | Admin |
| UC-ADMIN-04 | 상품 관련 뉴스 문서 수집 | Admin |
| UC-ADMIN-05 | 출시 뉴스 자동 게시 실행 | Admin, System |
| UC-SYSTEM-01 | 가격 스냅샷 기록 | System |
| UC-SYSTEM-02 | outbox event relay | System |

## UC-AUTH-01 이메일 인증 후 회원가입

Goal: Guest가 이메일 인증을 완료한 뒤 계정을 만든다.

1. 이메일 인증 메일을 요청한다.
2. 인증 링크의 token으로 이메일 인증을 완료한다.
3. 계정 정보를 제출한다.
4. 서버가 인증 상태와 중복을 확인하고 계정과 기본 역할을 저장한다.

Contract: [통합 API 명세의 Auth](../api/README.md#auth) · Policy: [Account Policy](../policy/account-policy.md)

## UC-AUTH-02 로그인, 토큰 재발급, 로그아웃

Goal: Guest가 로그인하고 Member가 token을 회전하거나 로그아웃한다.

1. Guest가 local 또는 OAuth2 로그인을 완료한다.
2. 서버가 access token과 refresh cookie를 발급한다.
3. Member가 refresh cookie로 token 재발급을 요청한다.
4. 로그아웃하면 서버 저장 token과 cookie를 제거한다.

Contract: [통합 API 명세의 Auth](../api/README.md#auth) · Operations: [OAuth2 상태 흐름](../troubleshooting/oauth2-state-flow.md)

## UC-ACCOUNT-01 내 계정/프로필 조회와 변경

Goal: Member가 자신의 계정 정보를 확인하고 닉네임 또는 비밀번호를 변경한다.

1. 인증 principal의 `accountId`로 계정 또는 프로필을 조회한다.
2. 닉네임 변경 시 중복을 확인하고 저장한다.
3. 비밀번호 변경 시 현재 비밀번호와 local account 조건을 확인한다.
4. 비밀번호 변경 후 기존 refresh token을 폐기한다.

Contract: [통합 API 명세의 Auth](../api/README.md#auth), [Board](../api/README.md#board) · Policy: [Account Policy](../policy/account-policy.md)

`/api/user/account/**`와 `/api/user/profile/**`의 alias/deprecation 관계는 현재 정의되어 있지 않다.

## UC-PUBLIC-01 공개 상품 탐색

Goal: Guest 또는 Member가 상품, 카테고리, 가격 이력, 상품 연결 게시글을 탐색한다.

1. 상품 목록·검색·카테고리에서 상품을 찾는다.
2. 상품 상세와 저장된 가격 snapshot을 조회한다.
3. 상품에 연결된 게시글을 조회한다.

Contract: [통합 API 명세의 Catalog](../api/README.md#catalog), [Price](../api/README.md#price), [Board](../api/README.md#board)

## UC-PUBLIC-02 공개 게시판 탐색

Goal: Guest 또는 Member가 공개 게시글과 댓글을 조회한다.

1. 게시글 목록 또는 검색 결과를 조회한다.
2. 게시글 상세를 열면 조회수 상태가 갱신된다.
3. 댓글 목록과 공개 interaction count를 확인한다.
4. 인증된 Member 응답에는 본인 interaction 상태가 포함될 수 있다.

Contract: [통합 API 명세의 Board](../api/README.md#board) · Policy: [Access Policy](../policy/access-policy.md)

## UC-PUBLIC-03 신상품 출시 뉴스 탐색

Goal: Guest 또는 Member가 자동 게시된 출시 뉴스의 본문, 출처, 구매 판단 집계를 확인한다.

1. `PRODUCT_LAUNCH_NEWS` 게시글을 찾는다.
2. 상세에서 저장된 원문 reference와 투표 집계를 조회한다.
3. 인증된 Member라면 본인 vote도 확인한다.

Contract: [통합 API 명세의 Board](../api/README.md#board) · Policy: [Access Policy](../policy/access-policy.md)

## UC-PRICE-01 상품 가격 판정 요청

Goal: Member가 구매 후보를 제출하고 response-only 가격 판정을 받는다.

1. Member가 가격 판정 요청을 제출한다.
2. 서버가 외부 비교 sample을 조회하고 판정 응답을 만든다.
3. 결과는 저장하거나 게시글로 만들지 않는다.

계산·응답 계약: [통합 API 명세의 Price](../api/README.md#price)

## UC-BOARD-01 게시글 작성/수정/삭제

Goal: Member가 게시글을 작성하고 작성자 또는 Admin이 수정·삭제한다.

1. Member가 `title`, `content`, `tags`, `fileIds`를 제출한다.
2. 서버가 작성자 snapshot과 첨부 file을 연결해 게시글을 저장한다.
3. 작성자 또는 Admin이 게시글을 수정하거나 삭제한다.

Contract: [통합 API 명세의 Board](../api/README.md#board) · Policy: [Delete Policy](../policy/delete-policy.md)

## UC-BOARD-02 댓글과 대댓글 작성/수정/삭제

Goal: Member가 댓글 또는 대댓글을 작성하고 작성자 또는 Admin이 수정·삭제한다.

1. Member가 게시글에 댓글을 작성한다.
2. `parentId`가 있으면 같은 게시글의 부모 댓글과 연결한다.
3. 작성자 또는 Admin이 내용을 수정하거나 삭제한다.

Contract: [통합 API 명세의 Board](../api/README.md#board) · Policy: [Delete Policy](../policy/delete-policy.md)

## UC-BOARD-03 게시글/댓글 좋아요와 취소

Goal: Member가 공개 게시글이나 댓글에 좋아요를 남기거나 취소한다.

1. Member가 대상에 좋아요를 요청한다.
2. 서버가 관계와 파생 count를 갱신한다.
3. 취소하면 본인의 관계를 제거하고 count를 갱신한다.

Contract: [통합 API 명세의 Board](../api/README.md#board)

## UC-BOARD-04 출시 뉴스 구매 판단 투표

Goal: Member가 허용된 출시 뉴스에 구매 판단 의견을 남기거나 취소한다.

1. Member가 출시 뉴스에서 vote를 선택한다.
2. 서버가 대상 eligibility를 확인하고 vote를 생성·변경한다.
3. 취소하면 본인의 vote를 제거한다.
4. 응답에서 집계와 본인 vote를 확인한다.

Contract: [통합 API 명세의 Board](../api/README.md#board) · Policy: [Access Policy](../policy/access-policy.md)

## UC-FILE-01 게시글 첨부 파일 URL 발급

Goal: Member 또는 Admin이 게시글 첨부 파일의 업로드·다운로드 URL을 받는다.

1. 업로드 URL과 file id를 요청한다.
2. 발급된 URL로 object를 업로드한다.
3. 게시글 저장 시 file id를 연결한다.
4. 필요할 때 download URL을 요청한다.

Contract: [통합 API 명세의 Board](../api/README.md#board)

## UC-AI-01 RAG 기반 AI 채팅

Goal: Member가 명시적으로 AI 채팅을 실행해 저장된 문서에 기반한 답변을 얻는다.

1. Member가 질문을 제출한다.
2. AI 모듈이 vector 유사 문서를 검색한다.
3. 검색 문맥을 포함해 답변을 생성하고 반환한다.

Contract: [통합 API 명세의 AI](../api/README.md#ai) · Policy: [AI Cost Policy](../policy/ai-cost-policy.md)

## UC-ADMIN-01 상품 수동 등록/숨김

Goal: Admin이 상품을 등록·갱신하거나 일반 조회에서 숨긴다.

1. Admin이 상품 정보를 제출한다.
2. catalog가 상품과 offer를 저장한다.
3. 필요하면 matching 후보를 남긴다.
4. Admin이 상품을 숨김 처리한다.

Contract: [통합 API 명세의 Catalog](../api/README.md#catalog)

## UC-ADMIN-02 상품 수집 운영

Goal: Admin이 tracked keyword와 collection job으로 catalog·price 데이터를 갱신한다.

1. Admin이 tracked keyword를 관리한다.
2. Admin 또는 System이 collection job을 실행한다.
3. ingest가 source adapter로 상품을 수집한다.
4. catalog와 price가 상품, offer, snapshot을 저장한다.
5. job이 실행 결과를 기록한다.

Contract: [통합 API 명세의 Ingest](../api/README.md#ingest)

## UC-ADMIN-03 상품 매칭 후보 검토

Goal: Admin이 유사 상품 매칭 후보를 승인하거나 거절한다.

1. catalog가 pending 후보를 저장한다.
2. Admin이 후보 목록을 조회한다.
3. 후보를 승인 또는 거절한다.

Contract: [통합 API 명세의 Catalog](../api/README.md#catalog)

## UC-ADMIN-04 상품 관련 뉴스 문서 수집

Goal: Admin이 뉴스 문서를 수집해 검색/RAG document store에 적재한다.

1. Admin이 뉴스 문서 수집을 요청한다.
2. ingest가 source adapter에서 문서를 수집하고 중복을 제거한다.
3. pipeline이 문서를 chunking하고 가능한 경우 embedding을 저장한다.
4. 저장 결과를 반환한다.

Contract: [통합 API 명세의 Ingest](../api/README.md#ingest)

## UC-ADMIN-05 출시 뉴스 자동 게시 실행

Goal: Admin 또는 System이 gate를 통과한 뉴스 후보를 출시 뉴스 게시글로 발행한다.

1. Admin 또는 scheduler가 출시 뉴스 게시를 요청한다.
2. ingest가 source 후보에 [통합 API 명세의 Ingest](../api/README.md#ingest) gate를 적용한다.
3. 통과한 후보만 AI draft를 생성한다.
4. board write 경계가 게시글과 reference를 저장한다.
5. 응답에서 게시·제외 결과를 확인한다.

Policy: [AI Cost Policy](../policy/ai-cost-policy.md)

## UC-SYSTEM-01 가격 스냅샷 기록

Goal: 상품 수집 결과의 가격을 원시 이력으로 저장한다.

1. 상품 수집 또는 upsert 결과를 받는다.
2. 가격을 `price_snapshots`에 저장한다.
3. 가격 이력 API가 저장된 snapshot을 반환한다.

Contract: [통합 API 명세의 Price](../api/README.md#price)

## UC-SYSTEM-02 outbox event relay

Goal: transaction 안에서 기록한 event를 relay가 나중에 publish한다.

1. 도메인 모듈이 pending event를 저장한다.
2. relay가 claimable event를 claim한다.
3. publisher에 dispatch한다.
4. 성공 또는 retry 상태를 기록한다.

Data boundary: [Use Case Data Policy](../policy/usecase-data-policy.md)

## Target Or Out Of Scope

| Scenario | Status |
| --- | --- |
| private workspace/draft/report | Target policy, 현재 API 미구현 |
| 키워드 구독과 사용자별 이메일 알림 | Target policy, 현재 API 미구현 |
| 유료 plan/quota 기반 AI assist | Target policy, 현재 enforcement 미구현 |
| 회원 탈퇴 | Target policy, 현재 API 미구현 |
| production load/test orchestration | 이 문서의 범위 밖 |
