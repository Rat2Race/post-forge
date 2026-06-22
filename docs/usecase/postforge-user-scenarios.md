# PostForge User Scenario Use Cases

## 범위

이 문서는 현재 코드에서 확인되는 PostForge 사용자 시나리오를 기능 단위로 정리한다.

포함 범위:

- `auth`: 이메일 인증, 회원가입, 로그인, OAuth2, JWT 재발급, 계정 관리
- `board`: 게시글, 댓글, 좋아요, 파일 URL, 프로필, 상품 연결 게시글, 출시 뉴스 구매 판단 투표
- `catalog`: 상품, 카테고리, offer, 상품 매칭 후보
- `price`: 가격 스냅샷 이력, 가격 이력 조회, response-only 가격 판정
- `source`: 외부 상품/뉴스 source adapter
- `ingest`: 상품 수집, tracked keyword, collection job, 뉴스/문서 적재, 출시 뉴스 자동 게시 orchestration
- `ai`: AI 채팅, 게시글 초안 생성
- `messaging`: outbox event 저장/relay와 in-process event dispatch
- `core`, `support`, `app`: 공통 계약, 인프라, 실행 조립

제외 범위:

- production load/test orchestration

## 액터

| 액터 | 설명 |
| --- | --- |
| Guest | 로그인하지 않은 사용자. 공개 상품/게시글 조회와 인증 진입만 가능하다. |
| Member | 로그인한 일반 사용자. 게시글/댓글/좋아요/파일/AI 보조 기능을 사용한다. |
| Admin | 운영 사용자. 상품 수집, 상품 upsert, 매칭 후보, 뉴스 문서 수집을 관리한다. |
| System | scheduler, event recorder, relay, in-process publisher처럼 사용자 요청 뒤에서 동작하는 자동화 주체다. |

## 기능 단위 요약

| 기능 ID | 기능 | 대표 액터 | 관련 모듈 |
| --- | --- | --- | --- |
| UC-AUTH-01 | 이메일 인증 후 회원가입 | Guest | `auth`, `support`, `core` |
| UC-AUTH-02 | 로그인, 토큰 재발급, 로그아웃 | Guest, Member | `auth`, `support`, `core` |
| UC-ACCOUNT-01 | 내 계정/프로필 조회와 변경 | Member | `auth`, `board`, `core` |
| UC-PUBLIC-01 | 공개 상품 탐색 | Guest, Member | `catalog`, `price`, `board` |
| UC-PUBLIC-02 | 공개 게시판 탐색 | Guest, Member | `board`, `price`, `catalog` |
| UC-PUBLIC-03 | 신상품 출시 뉴스 탐색 | Guest, Member | `board`, `ingest`, `source` |
| UC-PRICE-01 | 상품 가격 판정 요청 | Member | `price`, `source` |
| UC-BOARD-01 | 게시글 작성/수정/삭제 | Member, Admin | `board`, `core` |
| UC-BOARD-02 | 댓글과 대댓글 작성/수정/삭제 | Member, Admin | `board`, `core` |
| UC-BOARD-03 | 게시글/댓글 좋아요와 취소 | Member | `board`, `support` |
| UC-BOARD-04 | 출시 뉴스 구매 판단 투표 | Member | `board`, `support` |
| UC-FILE-01 | 게시글 첨부 파일 업로드/다운로드 URL 발급 | Member, Admin | `board`, `core` |
| UC-AI-01 | AI 채팅과 게시글 초안 생성 | Member | `ai`, `core` |
| UC-ADMIN-01 | 상품 수동 등록/숨김 | Admin | `catalog` |
| UC-ADMIN-02 | 상품 수집 키워드와 collection job 운영 | Admin, System | `ingest`, `source`, `catalog`, `price` |
| UC-ADMIN-03 | 상품 매칭 후보 검토 | Admin | `catalog`, `ai` |
| UC-ADMIN-04 | 상품 관련 뉴스 문서 수집 | Admin | `ingest`, `source`, `ai` |
| UC-ADMIN-05 | 출시 뉴스 자동 게시 실행 | Admin, System | `ingest`, `source`, `ai`, `board` |
| UC-SYSTEM-01 | 가격 스냅샷 기록 | System | `ingest`, `catalog`, `price` |
| UC-SYSTEM-02 | outbox event relay | System | `messaging`, `core` |

## UC-AUTH-01 이메일 인증 후 회원가입

### 목표

Guest가 이메일 인증을 완료한 뒤 PostForge 계정을 만든다.

### 주요 흐름

1. Guest가 이메일 인증 메일 발송을 요청한다.
2. `auth`는 이메일 중복 여부와 Redis 발송 제한 상태를 확인한다.
3. `auth`는 인증 token을 Redis TTL 상태로 저장하고 메일을 발송한다.
4. Guest가 인증 링크의 token으로 이메일 인증을 완료한다.
5. `auth`는 token을 삭제하고 인증 완료 상태를 Redis에 표시한다.
6. Guest가 username, email, nickname, password로 회원가입한다.
7. `auth`는 이메일 인증 상태와 username/email/nickname 중복을 확인한다.
8. `auth`는 계정과 기본 역할을 저장하고 회원가입 완료 응답을 반환한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/auth/email/send` | 인증 메일 발송 |
| `GET` | `/auth/email/verify?token=...` | 이메일 인증 확인 |
| `POST` | `/auth/register` | 회원가입 |

### 예외/정책

- 이미 사용 중인 email, username, nickname은 거부한다.
- 인증되지 않은 email로 회원가입할 수 없다.
- 이메일 인증 요청은 Redis guard로 과도한 재발송을 제한한다.

## UC-AUTH-02 로그인, 토큰 재발급, 로그아웃

### 목표

Guest가 로그인해 access token과 refresh token cookie를 받고, Member는 token을 회전하거나 로그아웃한다.

### 주요 흐름

1. Guest가 username/password로 로그인한다.
2. `auth`는 사용자/IP별 로그인 시도 제한과 잠금 상태를 확인한다.
3. 인증 성공 시 access token을 응답 body로 반환하고 refresh token을 cookie에 저장한다.
4. access token 만료 시 클라이언트가 refresh token cookie로 재발급을 요청한다.
5. `auth`는 refresh token 저장소를 검증하고 새 access/refresh token을 발급한다.
6. Member가 로그아웃하면 refresh token 저장소와 cookie를 삭제한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/auth/login` | ID/PW 로그인 |
| `POST` | `/auth/token/reissue` | access token 재발급 |
| `POST` | `/auth/logout` | 로그아웃 |
| `POST` | `/auth/oauth2/exchange` | OAuth2 exchange code로 로그인 |

### 예외/정책

- 로그인 실패가 누적되면 사용자별 잠금 상태를 만든다.
- refresh token은 Redis 저장값과 일치해야 한다.
- OAuth2 로그인은 provider profile을 계정으로 연결한 뒤 동일한 token 발급 흐름을 사용한다.

## UC-ACCOUNT-01 내 계정/프로필 조회와 변경

### 목표

Member가 자신의 계정과 프로필 정보를 확인하고 닉네임 또는 비밀번호를 변경한다.

### 주요 흐름

1. Member가 내 계정 또는 프로필 조회를 요청한다.
2. 서버는 인증 principal의 `accountId`로 계정을 조회한다.
3. Member가 닉네임 변경을 요청하면 중복 여부를 확인한 뒤 저장한다.
4. Member가 비밀번호 변경을 요청하면 현재 비밀번호와 local account 조건을 확인한다.
5. 비밀번호 변경 성공 후 기존 refresh token을 폐기한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/user/account` | 내 계정 조회 |
| `PATCH` | `/user/account/nickname` | 계정 닉네임 변경 |
| `PATCH` | `/user/account/password` | 계정 비밀번호 변경 |
| `GET` | `/user/profile` | 내 프로필 조회 |
| `PATCH` | `/user/profile/nickname` | 프로필 닉네임 변경 |
| `PATCH` | `/user/profile/password` | 프로필 비밀번호 변경 |

### 예외/정책

- 권한 판단은 username이 아니라 인증 principal의 `accountId`를 기준으로 한다.
- OAuth 계정은 local password 변경 대상이 아니다.

## UC-PUBLIC-01 공개 상품 탐색

### 목표

Guest 또는 Member가 상품, 카테고리, 가격 이력, 상품 연결 게시글을 탐색한다.

### 주요 흐름

1. 사용자가 상품 목록 또는 카테고리 목록을 조회한다.
2. 사용자가 검색어와 선택적 카테고리로 상품을 검색한다.
3. 사용자가 상품 상세를 확인한다.
4. 사용자가 상품 가격 이력을 조회한다.
5. 사용자가 가격 이력을 그래프로 볼 수 있도록 수집 시점별 snapshot을 조회한다.
6. 사용자가 상품과 연결된 게시글을 조회한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/products` | 활성 상품 목록 |
| `GET` | `/api/products/search` | 상품명 기반 검색 |
| `GET` | `/api/products/{productId}` | 상품 상세 |
| `GET` | `/api/products/categories` | 상품 카테고리 목록 |
| `GET` | `/api/products/categories/{categoryId}` | 카테고리별 상품 |
| `GET` | `/api/products/{productId}/prices` | 가격 이력 |
| `GET` | `/api/products/{productId}/posts` | 상품 연결 게시글 |

### 예외/정책

- 숨김 처리된 상품은 일반 목록/검색에서 제외한다.
- 상품 상세나 가격 조회는 AI 호출을 유발하지 않는다.
- 가격 이력은 상승/하락 여부를 이벤트로 해석하지 않고 저장된 snapshot을 반환한다.

## UC-PUBLIC-02 공개 게시판 탐색

### 목표

Guest 또는 Member가 공개 게시글 목록, 검색 결과, 상세, 댓글을 조회한다.

### 주요 흐름

1. 사용자가 게시글 목록을 조회한다.
2. 사용자가 keyword를 입력하면 게시글 검색을 수행한다.
3. 사용자가 게시글 상세를 조회한다.
4. `board`는 Redis로 중복 조회를 방지하면서 조회수 cache를 갱신한다.
5. 사용자가 댓글 목록을 조회한다.
6. 인증된 Member라면 응답에 본인의 좋아요 여부가 포함될 수 있다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/posts` | 게시글 목록/검색 |
| `GET` | `/api/posts` | 게시글 목록/검색 alias |
| `GET` | `/posts/{postId}` | 게시글 상세 |
| `GET` | `/posts/{postId}/comments` | 댓글 목록 |

### 예외/정책

- 삭제되거나 숨김 처리된 리소스는 일반 조회에서 제외한다.
- 게시글 상세와 댓글 조회는 AI 호출을 유발하지 않는다.

## UC-PUBLIC-03 신상품 출시 뉴스 탐색

### 목표

Guest 또는 Member가 자동 게시된 신상품 출시 뉴스를 읽고 출처, AI 요약, 구매 판단 투표 집계를 확인한다.

### 주요 흐름

1. 사용자가 게시글 목록 또는 검색으로 `PRODUCT_LAUNCH_NEWS` 게시글을 확인한다.
2. 사용자가 출시 뉴스 상세를 조회한다.
3. `board`는 저장된 `post_reference_links`로 원문 출처와 canonical URL evidence를 반환한다.
4. `board`는 `post_purchase_vote` 집계를 함께 반환한다.
5. 인증된 Member라면 응답에 본인의 `myVote`가 포함될 수 있다.
6. 상세 조회는 AI나 외부 뉴스/쇼핑 API를 호출하지 않는다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/posts?category=PRODUCT_LAUNCH_NEWS` | 출시 뉴스 목록/검색 |
| `GET` | `/api/posts/{postId}` | 출시 뉴스 상세 |

### 예외/정책

- 자동 게시 대상은 신상품 출시/예약판매/공식 발표/공식 가격 공개 관련 뉴스로 제한한다.
- 중복 기사, 광고성 기사, 출처 불명, 필수 키워드 미포함, AI 요약 실패, 일일 한도 초과 후보는 게시하지 않는다.

## UC-PRICE-01 상품 가격 판정 요청

### 목표

Member가 구매하려는 상품 가격을 입력하고 Naver Shopping 기준 response-only 가격 판정을 받는다.

### 주요 흐름

1. Member가 상품명/옵션/판매가/배송비/쿠폰·카드 할인/최종 결제액을 입력한다.
2. `price`는 입력값으로 candidate effective price를 계산한다. `finalPaidPrice`가 있으면 실제 구매가로 우선 사용한다.
3. `price`는 `source`의 Naver Shopping adapter로 비교 sample을 조회한다.
4. `price`는 비교 기준, 샘플 수, confidence, `CHEAP`/`NORMAL`/`EXPENSIVE`/`INSUFFICIENT_INFO` 판정을 반환한다.
5. 배송비 포함 여부를 확정하지 못하면 `배송비 포함 여부 미확인` 경고를 포함한다.
6. 결과는 저장하지 않고 게시글도 만들지 않는다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/price-checks` | Naver Shopping 기준 가격 판정 |

### 예외/정책

- Member 권한이 필요하다.
- 배송비 불확실성이 판정을 뒤집을 수 있으면 `LOW` confidence 또는 `INSUFFICIENT_INFO`로 응답한다.
- 가격 판정은 명시적 사용자 요청에서만 외부 쇼핑 API를 호출한다.

## UC-BOARD-01 게시글 작성/수정/삭제

### 목표

Member가 게시글을 작성하고, 작성자 또는 Admin이 게시글을 수정/삭제한다.

### 주요 흐름

1. Member가 title, content, summary, tags, category, fileIds로 게시글을 작성한다.
2. `board`는 인증 principal의 `accountId`와 nickname snapshot을 게시글에 저장한다.
3. 첨부 file id가 있으면 게시글과 파일 metadata를 연결한다.
4. 작성자 또는 Admin이 게시글을 수정한다.
5. 작성자 또는 Admin이 게시글을 삭제한다.
6. 삭제 시 일반 조회에서 제외되고 관련 파일 연결/조회수 cache가 정리된다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/posts` | 게시글 작성 |
| `PUT` | `/posts/{postId}` | 게시글 수정 |
| `DELETE` | `/posts/{postId}` | 게시글 삭제 |

### 예외/정책

- 게시글 쓰기는 `USER` 권한이 필요하다.
- 수정/삭제는 작성자 또는 Admin만 가능하다.

## UC-BOARD-02 댓글과 대댓글 작성/수정/삭제

### 목표

Member가 게시글에 댓글 또는 1-depth 대댓글을 남기고, 작성자 또는 Admin이 수정/삭제한다.

### 주요 흐름

1. Member가 게시글에 댓글을 작성한다.
2. parentId가 있으면 같은 게시글의 부모 댓글인지 확인하고 대댓글로 저장한다.
3. 작성자 또는 Admin이 댓글 내용을 수정한다.
4. 작성자 또는 Admin이 댓글을 삭제한다.
5. 댓글 목록 조회는 공개 게시글의 댓글을 생성순으로 반환한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/posts/{postId}/comments` | 댓글 작성 |
| `GET` | `/posts/{postId}/comments` | 댓글 목록 |
| `PUT` | `/posts/{postId}/comments/{commentId}` | 댓글 수정 |
| `DELETE` | `/posts/{postId}/comments/{commentId}` | 댓글 삭제 |

### 예외/정책

- 댓글 쓰기는 `USER` 권한이 필요하다.
- 수정/삭제는 작성자 또는 Admin만 가능하다.

## UC-BOARD-03 게시글/댓글 좋아요와 취소

### 목표

Member가 공개 게시글이나 댓글에 좋아요를 누르고 취소한다.

### 주요 흐름

1. Member가 게시글 또는 댓글 좋아요를 요청한다.
2. `board`는 대상이 존재하고 공개적으로 접근 가능한지 확인한다.
3. `board`는 이미 좋아요한 상태인지 확인한다.
4. 성공 시 like row와 count를 갱신한다.
5. 취소 시 like row를 삭제하고 count를 갱신한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/posts/{postId}/like` | 게시글 좋아요 |
| `DELETE` | `/posts/{postId}/like` | 게시글 좋아요 취소 |
| `POST` | `/posts/{postId}/comments/{commentId}/like` | 댓글 좋아요 |
| `DELETE` | `/posts/{postId}/comments/{commentId}/like` | 댓글 좋아요 취소 |

### 예외/정책

- 좋아요 요청은 Redis guard로 짧은 시간의 중복/과다 요청을 제한한다.
- count는 원본 like row에서 다시 계산 가능한 파생 데이터다.

## UC-BOARD-04 출시 뉴스 구매 판단 투표

### 목표

Member가 자동 게시된 출시 뉴스에 `BUYABLE`, `UNSURE`, `WAIT` 중 하나로 구매 판단 의견을 남긴다.

### 주요 흐름

1. Member가 `PRODUCT_LAUNCH_NEWS` 게시글 상세에서 투표를 선택한다.
2. `board`는 게시글 category와 `publish_origin=SYSTEM_BATCH` 조건을 확인한다.
3. 기존 투표가 있으면 vote type을 변경하고, 없으면 새 row를 만든다.
4. Member가 취소하면 본인의 vote row를 삭제한다.
5. 응답은 집계와 본인 vote를 반환한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `PUT` | `/api/posts/{postId}/purchase-vote` | 구매 판단 투표 등록/변경 |
| `DELETE` | `/api/posts/{postId}/purchase-vote` | 구매 판단 투표 취소 |

### 예외/정책

- 투표는 Member 이상만 가능하다.
- `PRODUCT_LAUNCH_NEWS` 및 `SYSTEM_BATCH` 게시글이 아니면 거절한다.
- 투표 집계는 공개할 수 있지만 `myVote`는 인증된 회원에게만 계산한다.

## UC-FILE-01 게시글 첨부 파일 업로드/다운로드 URL 발급

### 목표

Member 또는 Admin이 S3 presigned URL을 받아 게시글 첨부 파일을 업로드하거나 다운로드한다.

### 주요 흐름

1. 사용자가 fileName과 contentType으로 업로드 URL을 요청한다.
2. `board`는 파일 정책을 확인하고 S3 업로드 URL과 file id를 발급한다.
3. 사용자는 해당 URL로 파일을 업로드한다.
4. 게시글 작성/수정 시 file id를 전달해 게시글과 연결한다.
5. 사용자가 다운로드 URL을 요청하면 `board`가 저장된 file metadata로 presigned download URL을 생성한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/files/presigned-url` | 업로드 URL 발급 |
| `GET` | `/files/s3/presigned-url` | 업로드 URL 발급 alias |
| `GET` | `/files/{fileId}/download-url` | 다운로드 URL 발급 |
| `GET` | `/files/s3/{fileId}/download-url` | 다운로드 URL 발급 alias |

### 예외/정책

- 파일 URL 발급은 인증된 사용자만 가능하다.
- 실제 object 저장은 S3가 담당하고, 애플리케이션은 metadata와 접근 URL만 관리한다.

## UC-AI-01 AI 채팅과 게시글 초안 생성

### 목표

Member가 명시적으로 AI 기능을 실행해 질문 답변 또는 게시글 초안을 얻는다.

### 주요 흐름

1. Member가 `/ai/chat`에 메시지를 보낸다.
2. `ai`는 text generation client를 통해 답변을 생성한다.
3. Member가 `/ai/generate`에 topic, prompt, tags, category 등을 전달한다.
4. `ai`는 게시글 초안 응답을 생성한다.
5. 사용자가 초안을 게시글로 저장하려면 별도의 게시글 작성 API를 호출한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/ai/chat` | AI 채팅 |
| `POST` | `/ai/generate` | AI 게시글 초안 생성 |

### 예외/정책

- AI 호출은 사용자가 명시적으로 실행한 요청에서만 발생해야 한다.
- 공개 게시글/상품 조회는 AI 비용을 발생시키지 않는다.

## UC-ADMIN-01 상품 수동 등록/숨김

### 목표

Admin이 외부 수집과 별개로 상품을 수동 등록하거나 숨김 처리한다.

### 주요 흐름

1. Admin이 상품 source, externalProductId, 이름, 카테고리, 가격, 이미지, URL, mallName을 입력한다.
2. `catalog`는 category를 찾거나 생성한다.
3. 같은 source/externalProductId offer가 있으면 기존 상품/offer를 갱신한다.
4. 기존 상품과 유사한 후보가 있으면 자동 매칭하거나 pending candidate를 남긴다.
5. Admin이 상품을 숨김 처리하면 일반 상품 목록에서 제외된다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/admin/products` | 상품 수동 upsert |
| `PATCH` | `/api/admin/products/{productId}/hide` | 상품 숨김 |

### 예외/정책

- Admin 권한이 필요하다.
- 상품 matching/embedding은 optional이며 실패해도 product upsert 자체는 유지된다.

## UC-ADMIN-02 상품 수집 키워드와 collection job 운영

### 목표

Admin이 tracked keyword를 관리하고 수동/스케줄 상품 수집으로 catalog와 price 데이터를 갱신한다.

### 주요 흐름

1. Admin이 source, keyword, intervalMinutes, displayCount로 tracked keyword를 등록한다.
2. Admin이 tracked keyword 목록을 조회하거나 비활성화한다.
3. Admin 또는 System이 collection job을 실행한다.
4. `ingest`는 `source` adapter로 외부 상품을 검색한다.
5. `ingest`는 raw product payload를 저장한다.
6. `catalog`는 상품과 offer를 upsert한다.
7. `price`는 상승/하락 판단 없이 수집 시점별 price snapshot을 기록한다.
8. job은 성공/실패와 수집 count를 기록한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/admin/tracked-keywords` | 수집 키워드 등록 |
| `GET` | `/api/admin/tracked-keywords` | 수집 키워드 목록 |
| `PATCH` | `/api/admin/tracked-keywords/{id}/disable` | 수집 키워드 비활성화 |
| `POST` | `/api/admin/collection-jobs/manual` | 수동 상품 수집 |
| `GET` | `/api/admin/collection-jobs` | collection job 목록 |

### 예외/정책

- Admin 권한이 필요하다.
- source가 없으면 manual collection은 `MOCK` source를 기본값으로 사용한다.
- 외부 source 호출 실패는 job 실패 상태로 기록한다.

## UC-ADMIN-03 상품 매칭 후보 검토

### 목표

Admin이 유사 상품 매칭 후보를 검토해 승인하거나 거절한다.

### 주요 흐름

1. `catalog`가 상품 upsert 중 자동 매칭 기준에 못 미치는 유사 후보를 pending 상태로 저장한다.
2. Admin이 후보 목록을 status별로 조회한다.
3. Admin이 후보를 승인하면 승인 상태로 변경한다.
4. Admin이 후보를 거절하면 거절 상태로 변경한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/admin/product-match-candidates` | 매칭 후보 목록 |
| `PATCH` | `/api/admin/product-match-candidates/{candidateId}/approve` | 후보 승인 |
| `PATCH` | `/api/admin/product-match-candidates/{candidateId}/reject` | 후보 거절 |

### 예외/정책

- Admin 권한이 필요하다.
- 후보 생성에는 product embedding과 similarity search가 사용될 수 있다.

## UC-ADMIN-04 상품 관련 뉴스 문서 수집

### 목표

Admin이 상품 관련 뉴스 문서를 수집해 검색/RAG에 사용할 문서 store에 적재한다.

### 주요 흐름

1. Admin이 keyword, optional productId, displayCount, topics를 입력한다.
2. `ingest`는 keyword와 topics를 조합해 뉴스 검색 query를 만든다.
3. `source`의 Naver News adapter가 뉴스를 검색한다.
4. `ingest`는 link 기준으로 중복을 제거한다.
5. `ingest`는 뉴스 내용을 `DocumentIngestCommand`로 변환한다.
6. `IngestPipelineService`가 문서를 chunking하고 가능한 경우 vector embedding을 저장한다.
7. 응답은 수집 query, 저장 count, embedding 저장 여부를 반환한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/admin/news-documents/manual` | 상품 관련 뉴스 문서 수집 |
| `POST` | `/ingest/documents` | 문서 직접 적재 |

### 예외/정책

- 뉴스 수집은 Admin 권한이 필요하다.
- `/ingest/documents`는 JWT 보안 정책이 적용되는 문서 적재 API다.
- vector store가 불가한 경우 문서를 임베딩 없이 접수할 수 있다.

## UC-ADMIN-05 출시 뉴스 자동 게시 실행

### 목표

Admin 또는 System이 Naver News 후보를 필터링하고 AI 초안을 생성해 `PRODUCT_LAUNCH_NEWS` 게시글로 발행한다.

### 주요 흐름

1. Admin 또는 scheduler가 keyword/productId/displayCount/dailyCap/topics로 출시 뉴스 게시를 요청한다.
2. `ingest`는 Naver News source adapter로 후보 뉴스를 검색한다.
3. `ingest`는 canonical URL, 광고성 문구, 출처, 필수 키워드, 같은 상품/키워드 daily cap을 검사한다.
4. 통과한 후보만 AI draft generation port로 요약/초안을 요청한다.
5. AI 초안 생성이 성공하면 `board`의 post writer 경계로 `PRODUCT_LAUNCH_NEWS` 게시글과 `post_reference_links`를 저장한다.
6. 실패/제외된 후보는 skip reason으로 응답한다.

### API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/admin/launch-news/manual` | 출시 뉴스 수동 자동게시 실행 |

### 예외/정책

- Admin 또는 property-gated System batch 경로에서만 실행한다.
- skip reason은 `DUPLICATE_ARTICLE`, `ADVERTISING`, `UNKNOWN_SOURCE`, `MISSING_LAUNCH_KEYWORD`, `AI_GENERATION_FAILED`, `DAILY_CAP_EXCEEDED`를 사용한다.
- 공개 조회 경로에서는 AI를 호출하지 않는다.

## UC-SYSTEM-01 가격 스냅샷 기록

### 목표

상품 수집 결과의 가격을 상승/하락 이벤트로 해석하지 않고 원시 가격 이력으로 저장한다.

### 주요 흐름

1. 상품 수집 또는 수동 upsert 후 `price`가 price snapshot을 기록한다.
2. 같은 상품의 가격이 오르거나 내려도 모두 `price_snapshots`에 별도 행으로 저장한다.
3. `/api/products/{productId}/prices`는 저장된 snapshot을 반환한다.
4. 프론트는 반환된 이력을 기반으로 그래프와 가격 변동성 지표를 계산한다.
5. 이 흐름은 AI 게시글 생성이나 outbox event 발행을 유발하지 않는다.

### 관련 모듈

| 단계 | 모듈 |
| --- | --- |
| 상품 수집 | `ingest`, `source` |
| 상품/offer 정규화 | `catalog` |
| 가격 snapshot 기록 | `price` |
| 가격 이력 조회 | `price`, `app` |

### 예외/정책

- 가격 변동 이벤트와 AI 게시글은 이 흐름에서 생성하지 않는다.
- 가격 이력은 프론트 그래프를 위한 원천 데이터이며, backend volatility 지표 계산은 하지 않는다.

## UC-SYSTEM-02 outbox event relay

### 목표

도메인 이벤트를 transaction boundary 안에서 저장하고, relay가 나중에 안전하게 publish한다.

### 주요 흐름

1. 도메인 모듈이 이벤트를 기록한다.
2. `messaging`은 `outbox_events`에 pending event를 저장한다.
3. relay가 enabled이고 publisher가 있으면 claimable event를 processing 상태로 claim한다.
4. relay가 event type을 지원하는 publisher에 dispatch한다.
5. 성공하면 published 상태로 표시한다.
6. 실패하면 retry count, availableAt, lastError를 갱신한다.

### 예외/정책

- relay가 비활성화되어 있거나 publisher가 없으면 아무 event도 publish하지 않는다.
- `messaging`은 이벤트의 업무 의미를 해석하지 않고 envelope 상태와 전달만 관리한다.

## 현재 구현 기준에서 제외하거나 향후로 남길 시나리오

| 시나리오 | 이유 |
| --- | --- |
| 개인 workspace/draft/report 관리 | 정책 문서에는 있으나 현재 controller/API 구현 표면에서는 확인되지 않는다. |
| 키워드 구독과 사용자별 이메일 알림 | 정책 문서에는 있으나 현재 구현은 admin tracked keyword/product collection 중심이다. |
| 유료 플랜/quota 기반 AI assist | 접근/비용 정책에는 있으나 현재 API는 plan/quota enforcement를 직접 노출하지 않는다. |
| production load/test orchestration | 이 문서의 제외 범위다. |

## 우선 구현/검증 관점의 사용자 여정

1. Guest가 이메일 인증 후 회원가입하고 로그인한다.
2. Guest/Member가 신상품 출시 뉴스와 출처 evidence, 구매 판단 투표 집계를 확인한다.
3. Member가 출시 뉴스에 `BUYABLE`/`UNSURE`/`WAIT` 투표를 남긴다.
4. Member가 `POST /api/price-checks`로 구매 후보 가격을 입력하고 Naver Shopping 기준 판정을 받는다.
5. Admin/System이 출시 뉴스 후보를 필터링해 `PRODUCT_LAUNCH_NEWS` 게시글로 발행한다.
6. 상품 수집/가격 snapshot history는 보조 foundation으로 유지된다.

## 문서 근거

- `README.md`의 API Overview와 Implemented Features
- `docs/architecture/module-dependencies.md`의 모듈 책임
- `docs/policy/access-policy.md`와 `docs/policy/usecase-data-policy.md`의 접근/데이터 경계
- 각 모듈의 `presentation` controller와 핵심 `application` service
