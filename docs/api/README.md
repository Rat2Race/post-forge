# PostForge API 명세

현재 `@RestController`, 요청·응답 DTO, `PostForgeAuthorizationRules`, 전역 예외 처리기를 기준으로 한 단일 명세다. Spring Security가 소유하는 `/oauth2/**`, `/login/oauth2/**`와 Swagger/static 경로는 제외한다.

- AI 채팅 실행 예시: [ai-chat-smoke.http](./ai-chat-smoke.http)
- Naver source 실행 예시: [naver-source-smoke.http](./naver-source-smoke.http)

## 공통 계약

### 인증 표기

| 표기 | 호출 조건 |
| --- | --- |
| `PUBLIC` | 인증 없이 호출 가능. 일부 조회는 JWT가 있으면 개인화하며 엔드포인트 설명에 별도 표기 |
| `USER` | access token이 필요한 사용자 영역. 실제 `ROLE_USER` 전용, `ROLE_USER/ROLE_ADMIN` 공용, 소유자/ADMIN 조건은 각 엔드포인트 설명에 별도 표기 |
| `ADMIN` | `ROLE_ADMIN` 필요 |

인증이 필요한 요청은 `Authorization: Bearer {accessToken}`을 사용한다. 이 값은 서버가 계정 ID와 역할을 복원해 소유권·권한·개인화 상태를 판단하는 데 필요하다.

### 공통 요청 헤더

| 파라미터 | 필요 이유 |
| --- | --- |
| `Content-Type: application/json` | JSON body를 DTO로 역직렬화하고 Bean Validation을 적용하기 위해 필요 |
| `Authorization` | 인증 계정과 역할이 필요한 요청에 사용. PUBLIC 조회에서도 개인화가 필요하면 선택적으로 전달 |

### 페이지 조회 계약

다음 파라미터는 모든 요청의 공통값이 아니라, 아래 엔드포인트 표에서 `pageable`로 표시한 목록 조회에만 적용한다.

| 파라미터 | 필요 이유 |
| --- | --- |
| `page` | 조회할 0-based 페이지를 지정해 전체 데이터를 한 번에 읽지 않기 위해 필요 |
| `size` | 한 페이지의 항목 수를 제한하기 위해 필요 |
| `sort=field,direction` | 동일한 필터에서도 결과 순서를 재현하기 위해 필요. 컨트롤러 기본값이 있으면 아래 엔드포인트 표에 표기 |

`PageResponse<T>`는 Board 전용이 아니라 `core`에 둔 공용 페이지 응답이다. Board·Catalog·Ingest의 pageable 조회가 `content`, `page`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last`, `empty`를 같은 형태로 반환한다.

Spring pageable resolver는 일부 잘못된 `page`/`size` 값을 `0`, 엔드포인트 기본값 또는 최대값으로 보정한다. 반면 지원하지 않는 `sort` property는 현재 repository 단계에서 `500`이 될 수 있다.

### Success

| 공통 DTO | 필드 |
| --- | --- |
| `MessageResponse` | `message` |
| `UrlResponse` | `url` |
| `PageResponse<T>` | 위 pagination 필드와 `content` |

### Fail

전역·보안 handler가 처리하는 애플리케이션 오류는 다음 형태를 사용한다. `validation`은 Bean Validation 실패일 때만 포함된다. Spring 자체의 `405 Method Not Allowed`, `415 Unsupported Media Type` 응답은 이 형태가 보장되지 않는다.

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "입력값 검증 실패",
  "validation": {
    "field": "검증 메시지"
  },
  "timestamp": "2026-08-12T12:00:00"
}
```

| HTTP | 대표 `error` | 발생 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR`, `INVALID_INPUT` | DTO 제약 위반, body 파싱 실패, 필수 query 누락, enum·날짜·숫자 변환 실패 |
| `401` | `UNAUTHORIZED`, `INVALID_TOKEN`, `EXPIRED_TOKEN`, `INVALID_CREDENTIALS` | 인증 정보 없음, 토큰 또는 자격 증명 오류 |
| `403` | `FORBIDDEN`, `ACCESS_DENIED`, `ACCOUNT_NOT_ACTIVE` | 역할·소유권 부족 또는 비활성 계정. Security filter의 직접 응답은 `FORBIDDEN`, MVC advice 경로는 `ACCESS_DENIED` |
| `404` | `RESOURCE_NOT_FOUND` 또는 모듈별 `*_NOT_FOUND` | 대상 리소스 없음 |
| `409` | `DATA_INTEGRITY_VIOLATION`, `CONCURRENT_MODIFICATION` 또는 중복 코드 | unique 제약이나 동시 수정 충돌 |
| `429` | `TOO_MANY_REQUESTS` | 이메일·로그인·좋아요 요청 보호 한도 초과 또는 보호 저장소 장애 시 fail-closed |
| `500` | `INTERNAL_SERVER_ERROR` 또는 모듈별 오류 | 처리되지 않은 내부·외부 연동 오류 |
| `503` | `EXTERNAL_SERVICE_UNAVAILABLE`, `DOCUMENT_STORE_UNAVAILABLE` | AI vector 검색, 가격 판정용 Naver Shopping source, 문서 vector store 장애. 수동 상품 수집 source 장애는 `200`과 `status=FAILED`로 반환 |

각 엔드포인트의 `Fail`에는 공통 인증 실패를 반복해서 쓰지 않는다. 인증 표기가 `PUBLIC`이 아니면 `401`, 역할·소유권 조건이 있으면 `403`이 함께 적용된다.

<a id="ai"></a>
## AI

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `POST /api/ai/chat` | USER | body `ChatRequest` — 질문, 안전 검사, 유사 문서 검색, LLM 입력에 사용 | `200 ChatResponse`; 안전 정책 거절과 LLM 장애 fallback도 `answer`에 담긴 정상 응답 | `400 VALIDATION_ERROR/INVALID_INPUT`; vector 검색 장애 `503 EXTERNAL_SERVICE_UNAVAILABLE`; 인증 `401/403` |

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `ChatRequest.message` | 필수, blank 불가 | 사용자의 질문이자 안전 검사·검색·생성의 원문 |

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `ChatResponse` | `answer` |

클라이언트가 호출하는 AI 기능은 이 RAG 채팅 하나다. 별도 검색 endpoint는 없고, `ChatService`가 내부적으로 vector 유사 문서 검색을 수행한다. 출시뉴스 초안 생성은 공개 API가 아닌 system batch 내부 port다.

<a id="auth"></a>
## Auth

<a id="token-cookie"></a>
### Token / Cookie

| 항목 | 계약과 필요한 이유 |
| --- | --- |
| Access token | `Authorization: Bearer {accessToken}`. stateless 요청에서 계정·역할을 식별 |
| Refresh cookie | `refresh_token`; access token을 다시 발급할 장기 자격 증명 |
| `HttpOnly`, `Secure`, `Path=/api/auth` | JavaScript 접근과 평문 전송을 막고 cookie 전송 경로를 인증 API로 제한 |
| `SameSite=Lax` | cross-site subrequest에는 cookie를 보내지 않고 top-level safe navigation에는 허용해 일반적인 CSRF 노출을 줄임. CSRF 방어 전체를 대신하지는 않음 |
| `Cache-Control: no-store` | 브라우저·중간 cache가 token 응답을 저장하지 못하게 하는 주 지시자 |
| `Pragma: no-cache` | 현대 HTTP 응답에서는 독립적인 cache 방지 수단이 아니어서 `no-store`와 기능상 중복에 가깝다. 다만 OAuth 2.0 RFC 6749 token response가 두 header를 함께 요구하므로 구형 client/proxy 호환과 규격 준수를 위해 유지 |
| `Max-Age` | refresh cookie 유효 시간을 초 단위로 지정. 발급 시 refresh token 설정 일수×86400, 로그아웃 시 `0`으로 즉시 제거 |
| Rotation | 로그인·OAuth 교환·재발급 시 refresh cookie 발급 또는 교체 |

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `POST /api/auth/email/send` | PUBLIC | body `SendEmailRequest` — 인증 대상 이메일과 중복 여부를 확인하고 일회용 링크를 발송 | `200 MessageResponse` | `400 VALIDATION_ERROR`; `409 DUPLICATE_EMAIL`; `429 TOO_MANY_REQUESTS`; `500 EMAIL_SEND_FAILED` |
| `GET /api/auth/email/verify` | PUBLIC | query `token` 필수 — 발송된 일회용 토큰을 이메일과 연결하고 사용 후 제거 | `200 EmailVerificationResponse` | 누락 `400 INVALID_INPUT`; 만료·없음·이미 사용된 토큰 `404 EMAIL_CODE_NOT_FOUND` |
| `POST /api/auth/register` | PUBLIC | body `RegisterRequest` — local 계정 생성과 이메일 인증 여부·중복 검사에 사용 | `201 RegisterResponse` | `400 VALIDATION_ERROR/EMAIL_NOT_VERIFIED`; `409 DUPLICATE_USERNAME/DUPLICATE_NICKNAME/DATA_INTEGRITY_VIOLATION` |
| `POST /api/auth/login` | PUBLIC | body `LoginRequest` — 자격 증명 검증. 원격 IP는 서버가 로그인 보호에 사용 | `200 AccessTokenResponse` + refresh cookie | `400 VALIDATION_ERROR`; `401 INVALID_CREDENTIALS`; `403 ACCOUNT_NOT_ACTIVE`; `429 TOO_MANY_REQUESTS` |
| `POST /api/auth/logout` | USER | body 없음. JWT의 account ID로 저장된 refresh token을 삭제 | `200 MessageResponse` + refresh cookie 제거 | 인증 `401/403`; 처리되지 않은 저장소 예외 `500 INTERNAL_SERVER_ERROR` |
| `POST /api/auth/token/reissue` | PUBLIC | cookie `refresh_token` 필수 — route는 공개지만 저장된 refresh 자격 증명과 대조해 탈취·폐기된 토큰의 재사용을 막음 | `200 AccessTokenResponse` + refresh cookie 회전 | 누락 `401 UNAUTHORIZED`; `401 INVALID_TOKEN/EXPIRED_TOKEN`; `403 ACCOUNT_NOT_ACTIVE`; `404 USER_NOT_FOUND` |
| `POST /api/auth/oauth2/exchange` | PUBLIC | body `OAuth2ExchangeRequest` — OAuth 성공 후 받은 일회용 code를 계정 토큰으로 교환 | `200 AccessTokenResponse` + refresh cookie | `400 VALIDATION_ERROR/INVALID_INPUT`; `401 INVALID_TOKEN`; `403 ACCOUNT_NOT_ACTIVE`; `404 USER_NOT_FOUND` |
| `GET /api/user/account` | USER | body 없음. JWT account ID로 현재 계정 조회 | `200 AccountResponse` | 인증 `401/403`; `404 USER_NOT_FOUND` |
| `PATCH /api/user/account/nickname` | USER | body `AccountUpdateRequest` — 새 공개 닉네임 지정 | `200 MessageResponse` | `400 VALIDATION_ERROR`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND`; `409 DUPLICATE_NICKNAME` |
| `PATCH /api/user/account/password` | USER | body `PasswordUpdateRequest` — 현재 비밀번호로 본인 확인 후 새 비밀번호 저장 | `200 MessageResponse`; 기존 refresh token 폐기 | `400 VALIDATION_ERROR/INVALID_PASSWORD/OAUTH_PASSWORD_UPDATE_NOT_ALLOWED`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `SendEmailRequest.email` | 필수, email 형식; trim·lowercase 정규화 | 인증 링크의 수신자이자 verified-email key |
| `RegisterRequest.username` | 필수, 4~20자 영문·숫자 | local 로그인 식별자 |
| `RegisterRequest.password` | 필수, 최소 8자, 대·소문자·숫자·`@$!%*?&` 포함 | local 계정 자격 증명 강도 보장 |
| `RegisterRequest.email` | 필수, email 형식 | 사전 인증 완료 여부와 계정 연락처 확인 |
| `RegisterRequest.nickname` | 필수, 2~20자 한글·영문·숫자·`_` | 게시글·댓글에 노출할 고유 표시명 |
| `LoginRequest.username` | 필수, 4~20자 영문·숫자 | 인증할 local 계정 조회 |
| `LoginRequest.password` | 필수 | 저장된 password hash와 대조 |
| `OAuth2ExchangeRequest.code` | 필수 | OAuth 성공 handler가 만든 일회용 교환 자격 증명 |
| `AccountUpdateRequest.nickname` | 필수, 2~20자 한글·영문·숫자·`_` | 변경할 공개 표시명 |
| `PasswordUpdateRequest.currentPassword` | 필수 | 변경 요청자 본인 확인 |
| `PasswordUpdateRequest.newPassword` | 필수, 8~100자 | 새 password hash의 원문 |

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `EmailVerificationResponse` | `message`, `email` |
| `RegisterResponse` | `accountId`, `message` |
| `AccessTokenResponse` | `grantType`, `accessToken` |
| `AccountResponse` | `username`, `nickname`, `provider`, `isOAuthUser`, `roles` |

<a id="board"></a>
## Board

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `GET /api/posts` | PUBLIC | `keyword?` 제목·본문 검색, `category?` 게시글 종류 필터, `publishOrigin?` 사용자·batch·backfill 출처 필터, pageable 기본 `size=20`, `sort=createdAt,DESC`; 선택 JWT는 `isLiked`, `myVote` 계산에 사용 | `200 PageResponse<PostDetailResponse>` | 잘못된 enum `400 INVALID_INPUT`; 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts` | USER | body `PostRequest` — 일반 게시글 본문·태그·첨부 지정; JWT account ID는 작성자 | `201 PostSummaryResponse` | `400 VALIDATION_ERROR`; 인증·계정 상태 `401/403`; 작성자 `404 USER_NOT_FOUND`; 충돌 `409` |
| `GET /api/posts/{postId}` | PUBLIC | path `postId` — 조회할 게시글 식별. 선택 JWT가 있으면 개인화 및 사용자별 조회수 증가 | `200 PostDetailResponse` | `404 POST_NOT_FOUND`; 숫자가 아닌 경로 `404 RESOURCE_NOT_FOUND` |
| `PUT /api/posts/{postId}` | USER | path `postId` 대상 식별, body `PostRequest` 새 상태. 작성자 또는 ADMIN만 허용 | `200 PostSummaryResponse` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `DELETE /api/posts/{postId}` | USER | path `postId` — 삭제할 게시글과 연결 파일·조회수 정리 대상 식별. 작성자 또는 ADMIN만 허용 | `200 MessageResponse` | 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `POST /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID 중복 방지·개인 상태 | `200 LikeResponse` | 인증 `401/403`; `404 POST_NOT_FOUND`; cooldown·분당 한도·guard 장애 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID의 좋아요만 제거 | `200 LikeResponse` | 인증 `401/403`; `404 POST_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `PUT /api/posts/{postId}/purchase-vote` | USER | path `postId` 대상, body `PurchaseVoteRequest` 선택, JWT account ID별 1표 저장 | `200 PurchaseVoteResponse` | `400 VALIDATION_ERROR/PURCHASE_VOTE_NOT_ALLOWED`; 인증 `401/403`; `404 POST_NOT_FOUND` |
| `DELETE /api/posts/{postId}/purchase-vote` | USER | path `postId` 대상, JWT account ID의 표만 제거 | `200 PurchaseVoteResponse` | `400 PURCHASE_VOTE_NOT_ALLOWED`; 인증 `401/403`; `404 POST_NOT_FOUND` |
| `GET /api/posts/{postId}/comments` | PUBLIC | path `postId` 댓글 묶음, pageable 기본 `size=50`, `sort=createdAt,ASC`; 선택 JWT는 `isLiked` 계산 | `200 PageResponse<CommentDetailResponse>`; 댓글이 없으면 빈 page | 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts/{postId}/comments` | USER | path `postId` 작성 대상, body `CommentRequest`, JWT account ID 작성자 | `201 CommentSummaryResponse` | `400 VALIDATION_ERROR/INVALID_COMMENT_PARENT/MAX_COMMENT_DEPTH_EXCEEDED`; 인증·계정 `401/403`; `404 POST_NOT_FOUND/COMMENT_NOT_FOUND` |
| `PUT /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 수정 대상, body `CommentRequest`의 `content` 사용. 작성자 또는 ADMIN만 허용 | `200 CommentSummaryResponse` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `DELETE /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 삭제 대상. 작성자 또는 ADMIN만 허용 | `200 MessageResponse` | 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `POST /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID 개인 상태 | `200 LikeResponse` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID의 좋아요 제거 | `200 LikeResponse` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `GET /api/products/{productId}/posts` | PUBLIC | path `productId` — `post_product_links` 연결이 만들어진 최신순으로 게시글을 전부 조회. pageable이 아니며 익명 조회라 `isLiked=false` | `200 List<PostDetailResponse>`; 연결이 없거나 알 수 없는 product ID면 빈 배열. 현재 운영 write flow는 이 테이블을 채우지 않아 별도 seed가 없으면 빈 배열 | 숫자가 아닌 경로 `404 RESOURCE_NOT_FOUND`; 내부 조회 오류 `500` |
| `GET /api/user/profile` | USER | body 없음. JWT account ID로 상세 프로필 조회 | `200 ProfileResponse` | 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `PATCH /api/user/profile/nickname` | USER | body `ProfileNicknameUpdateRequest` — 새 공개 닉네임 | `200 MessageResponse` | `400 VALIDATION_ERROR`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND`; `409 DUPLICATE_NICKNAME` |
| `PATCH /api/user/profile/password` | USER | body `ProfilePasswordUpdateRequest` — 현재 비밀번호 확인 후 변경 | `200 MessageResponse`; refresh token 폐기 | `400 VALIDATION_ERROR/INVALID_PASSWORD/OAUTH_PASSWORD_UPDATE_NOT_ALLOWED`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `GET /api/files/presigned-url` | USER | query `fileName` 확장자·저장명 생성, `contentType` 실제 업로드 MIME 서명. `/api/files/s3/presigned-url` 별칭도 동일 | `200 FileUploadResponse`; URL TTL 5분 | 누락 `400 INVALID_INPUT`; `400 FILE_EXTENSION_NOT_ALLOWED/FILE_TYPE_MISMATCH`; 인증 `401/403`; S3/DB 오류 `500` |
| `GET /api/files/{fileId}/download-url` | USER | path `fileId` — 저장 object key를 조회. `/api/files/s3/{fileId}/download-url` 별칭도 동일 | `200 UrlResponse`; URL TTL 5분 | 타입 오류 `400 INVALID_INPUT`; 인증 `401/403`; `404 FILE_NOT_FOUND`; S3 오류 `500` |

### HTTP method 선택 기준

| Method | 의미 | 이 프로젝트의 예 |
| --- | --- | --- |
| `POST` | 대상 collection이나 처리기에 payload를 맡긴다. 보통 서버가 새 ID를 정하며 같은 요청을 반복하면 새 결과가 생길 수 있다 | `POST /api/posts`, 로그인, 수동 수집 |
| `PUT` | client가 수정 권한을 가진 target 표현의 원하는 전체 상태를 설정한다. 같은 요청을 반복해도 최종 상태가 같아야 한다 | 게시글 편집 상태 설정, 사용자별 구매 투표 설정 |
| `PATCH` | 기존 target에 변경분만 적용한다. 요청에 없는 속성을 보존하도록 서버가 merge해야 하며 method 자체가 이를 자동 보장하지 않는다 | nickname 변경, 고정 상태 전이 |
| `DELETE` | target resource 또는 관계를 제거한다 | 게시글 삭제, 좋아요 관계 삭제, 구매 투표 삭제 |

`/api/posts`와 중첩 `/comments`는 collection URI다. `GET`은 collection 조회, `POST`는 그 안에 단일 resource 생성이므로 `/posts/create`처럼 동사까지 넣으면 `POST`와 URI가 모두 `create`를 말하는 의미 중복이 생긴다. 요청이 두 번 실행된다는 뜻은 아니다.

좋아요의 별도 `GET`도 DB가 같은 요청을 두 번 실행한다는 뜻이 아니다. 게시글·댓글 상세가 이미 `isLiked`와 `likeCount`를 반환하므로, `/like` 조회를 추가하면 같은 상태를 노출하는 API 계약이 하나 더 생긴다는 뜻이다. 좋아요만 가볍게 polling할 실제 요구가 생기기 전에는 추가하지 않는다.

현재 좋아요의 `POST`/`DELETE`는 관계 생성/삭제로 해석할 수 있다. 다만 관계 key가 `(targetId, accountId)`로 이미 결정되므로 신규 계약은 `PUT /like`와 `DELETE /like`가 더 정확하다. 이 경우 현재 cooldown이 동일한 `PUT` 재호출을 `429`로 막지 않도록 함께 바꿔야 진짜 idempotent 계약이 된다. 구매 판단은 사용자별 한 개의 알려진 상태를 생성하거나 교체하므로 `PUT`을 사용하고 조회값은 `PostDetailResponse.purchaseVote`에 포함한다.

### PUT과 PATCH의 현재 구현 범위

`PATCH`의 “부분 수정”은 요청에서 지정한 값만 바꾸고, 빠진 값은 기존 값을 유지한다는 뜻이다. 예를 들어 현재 값이 `{nickname: "rat", email: "a@example.com", roles: ["USER"]}`일 때 `{ "nickname": "newRat" }`를 적용하면 email과 roles는 그대로 남는다. 반면 full-replacement `PUT`이라면 빠진 email과 roles를 삭제·기본화하거나 불완전한 요청으로 거절하는 것이 일반적인 계약이다.

PUT/PATCH의 기준은 ID·작성자·생성일 같은 서버 관리 필드를 보존하느냐가 아니다. client가 관리하는 편집 필드를 매번 완전한 상태로 보내면 PUT이고, 일부 필드만 보내며 누락 필드를 유지해야 하면 PATCH다. PATCH로 바꾸려면 annotation만 교체할 것이 아니라 DTO가 `누락`과 `명시적 null/빈 값`을 구분하고 service가 기존 값과 merge하도록 바꿔야 한다.

현재 `PUT /api/posts/{postId}`는 client가 편집 가능한 `title`, `content`, `tags`, `fileIds` 묶음을 교체한다. `tags`를 생략하면 빈 목록이 되고 `fileIds`를 생략하면 기존 첨부가 모두 해제되므로, edit client는 네 필드를 전체 전송해야 한다. `summary`, `publishOrigin`은 서버 관리 필드로 유지하지만 category는 무조건 `GENERAL`로 바꾼다. 따라서 ADMIN이 system 출시뉴스를 수정하면 category가 바뀌는 결함이 있다. 댓글 `PUT`도 생성용 `CommentRequest`를 재사용해 `parentId`를 받지만 수정에서는 무시하고 `content`만 바꾼다. 게시글 category는 보존하고, 댓글 수정에는 `content`만 가진 별도 DTO를 쓰는 것이 현재 의도에 맞다.

`PATCH /hide`, `/approve`, `/reject`, `/disable`은 request body를 merge하는 범용 PATCH가 아니라 미리 정해진 상태로 바꾸는 command형 endpoint다. 단순 상태 수정으로 통일하려면 `PATCH /api/admin/products/{id}`와 `{ "status": "HIDDEN" }`처럼 만들고, command를 강조하려면 `POST /.../{id}/hide`가 더 솔직하다.

### 좋아요·구매 투표 동시성 경계

두 기능 모두 `(post_id, account_id)` unique constraint가 중복 row 자체는 막는다. 그러나 현재 vote는 `조회 -> 없으면 insert`라서 동시 최초 요청 중 하나가 unique 충돌로 실패할 수 있다. 좋아요도 충돌을 `save` 주변에서 catch하지만, 예외가 난 transaction은 rollback 대상이라 이어지는 count 갱신까지 안전하게 복구하지 못한다. 서로 다른 사용자가 동시에 좋아요할 때 `count -> 절대값 저장` 사이에 lost update가 날 가능성도 있다.

재사용할 것은 `AbstractLikeService` 상속이 아니라 DB의 원자적 쓰기 방식이다. 좋아요는 `INSERT ... ON CONFLICT DO NOTHING RETURNING ...`의 반환 여부로 실제 생성됐을 때만 `like_count = like_count + 1`을 원자 갱신하고, unlike도 `DELETE ... RETURNING ...`으로 실제 삭제됐을 때만 감소시킨다. 투표는 `INSERT ... ON CONFLICT (post_id, account_id) DO UPDATE SET vote_type = EXCLUDED.vote_type`으로 처리한다. like는 boolean 관계와 cached count, vote는 enum 상태·자격 검증·3종 집계를 다루므로 공통 추상화는 만들지 않는다.

`GET /api/products/{productId}/posts`의 원래 제품 의도는 자동 생성한 가격 하락·상품 소개 게시글을 특정 상품에 안정적으로 연결해 상품 상세 화면에서 보여주는 것이었다. `ProductMatchingService`의 Top-K나 의미 검색 결과를 분류해 반환하는 API가 아니라 `post_product_links`의 명시적 관계만 읽는다. 검색은 제목·본문의 유사 결과를 찾는 데 적합하지만, 특정 상품 귀속·일일 중복 방지·상품명이 바뀌어도 유지되는 연결을 보장하지 못해 당시에는 별도 관계가 필요했다.

그러나 그 관계를 쓰던 production writer는 제거됐고 현재 endpoint는 사실상 항상 빈 배열이다. 실제 출시뉴스 흐름은 `post_reference_links.product_id`에 연결을 저장한다. 출시뉴스를 보여줄 목적이면 기존 reference를 조회하도록 바꾸고, 요구가 아직 없다면 endpoint와 사용되지 않는 연결 model을 제거하는 편이 맞다. 두 테이블을 동시에 쓰지는 않는다.

Board의 게시글·댓글·상품 연결 path ID는 `\d+` 경로만 매핑한다. 숫자가 아니면 controller에 도달하지 않고 `404 RESOURCE_NOT_FOUND`다. `PUT/DELETE` 댓글 경로의 `postId`는 현재 서비스 계층에서 `commentId`와의 소속 관계 검증에 사용되지 않는다. 호출자는 실제 댓글의 게시글 ID를 넣어야 한다.

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `PostRequest.title` | 필수, 2~100자, `<`·`>` 금지 | 목록·상세 제목과 기본 검색 대상 |
| `PostRequest.content` | 필수, 10~10000자 | 게시글 본문 |
| `PostRequest.tags` | 선택, 최대 20개·각 50자 | 게시글 분류·표시용 태그 |
| `PostRequest.fileIds` | 선택 | presigned URL 발급 때 만든 파일 레코드를 게시글에 연결. 현재 저장소 조회 결과에 없는 ID는 연결되지 않음 |
| `CommentRequest.parentId` | 선택 | 대댓글의 바로 위 댓글 식별. 없으면 최상위 댓글 |
| `CommentRequest.content` | 필수, 2~500자, 위험 HTML 패턴 차단 | 댓글 본문 |
| `PurchaseVoteRequest.voteType` | 필수, `BUYABLE/UNSURE/WAIT` | 사용자의 구매 판단 한 가지를 저장 |
| `ProfileNicknameUpdateRequest.nickname` | 필수, 2~20자 한글·영문·숫자·`_` | 변경할 공개 표시명 |
| `ProfilePasswordUpdateRequest.currentPassword` | 필수 | 본인 확인 |
| `ProfilePasswordUpdateRequest.newPassword` | 필수, 8~100자 | 새 password hash 입력 |

사용자 게시글 생성·수정은 요청에서 category를 받지 않고 항상 `GENERAL`, `publishOrigin=USER`로 저장한다. 구매 판단 투표는 `PRODUCT_LAUNCH_NEWS`이면서 `publishOrigin=SYSTEM_BATCH`인 게시글에만 허용한다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `PostDetailResponse` | `id`, `title`, `content`, `summary`, `tags`, `category`, `publishOrigin`, `accountId`, `nickname`, `views`, `commentCount`, `likeCount`, `isLiked`, `purchaseVote`, `references`, `files`, `createdAt`, `modifiedAt` |
| `PostSummaryResponse` | `id`, `title`, `summary`, `tags`, `category`, `publishOrigin`, `accountId`, `nickname`, `createdAt`, `modifiedAt` |
| `PostReferenceLinkResponse` | `id`, `keyword`, `productId`, `provider`, `canonicalUrl`, `originalUrl`, `sourceName`, `publishedAt`, `titleSnapshot`, `publishOrigin` |
| `FileInfoResponse` | `fileId`, `originalFileName`, `fileType` |
| `CommentDetailResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `replyCount`, `likeCount`, `isLiked`, `createdAt`, `modifiedAt` |
| `CommentSummaryResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `createdAt`, `modifiedAt` |
| `LikeResponse` | `isLiked`, `likeCount` |
| `PurchaseVoteResponse` | `postId`, `eligible`, `buyableCount`, `unsureCount`, `waitCount`, `myVote` |
| `ProfileResponse` | `accountId`, `username`, `email`, `nickname`, `provider`, `isOAuthUser`, `roles`, `createdAt`, `updatedAt` |
| `FileUploadResponse` | `fileId`, `savedName`, `url` |

<a id="product-data-pipeline"></a>
## 상품 데이터 파이프라인 책임 명세

네 모듈은 같은 상품 흐름에 참여하지만 책임이 다르다.

| 모듈 | 한 문장 책임 | 입력 | 출력·소유 데이터 | 하지 않는 일 |
| --- | --- | --- | --- | --- |
| `source` | 외부 provider를 호출하고 provider 응답을 내부 중립 DTO로 번역 | `ProductSourceQuery`, `NewsSourceQuery`, credential/config | `ProductSourceResult`, `NewsSourceResult`; 영속 테이블 없음 | scheduling, raw 저장, 상품 병합·분류, 가격 판정 |
| `ingest` | 언제 무엇을 수집할지 정하고 전체 수집 단계를 조율 | admin 요청, scheduler, tracked keyword | `tracked_keywords`, `collection_jobs`, `raw_products`; Catalog/Price/문서·게시 port 호출 | provider별 HTTP 세부 구현, 상품 identity·matching 규칙, 가격 계산 규칙 |
| `catalog` | 저장된 상품 조회 모델과 source별 offer·동일상품 매칭 후보를 관리 | `ProductUpsertCommand` | `products`, `offers`, `product_categories`, matching 후보·embedding | 외부 수집 시점 결정, raw payload 보존, 가격 시계열·판정 |
| `price` | 상품 가격 시점 이력과 사용자 가격 비교 판정을 소유 | Catalog의 product/offer 또는 사용자 가격 요청 | `price_snapshots`, `PriceCheckResponse` | 상품 identity 병합, 외부 수집 job 관리, 게시글 생성 |

`source`는 controller와 영속 테이블이 없는 내부 adapter 모듈이다. 외부 API의 필드·인증·오류를 이 모듈 안에 가두고, 나머지 모듈에는 provider 중립 DTO만 넘긴다.

### 상품 수집 쓰기 흐름

```text
Admin 또는 Scheduler
  -> Ingest: collection job 생성
  -> Source: 외부 상품 검색 및 중립 DTO 변환
  -> Ingest: raw_products에 원문 JSON 보존
  -> Catalog: product/category/offer upsert 및 선택적 matching
  -> Price: 해당 시점 price_snapshot 저장
  -> Ingest: job SUCCESS 또는 FAILED 기록
```

### 조회·판정 흐름

```text
GET /api/products*                 -> Catalog DB만 조회
GET /api/products/{id}/prices      -> Price snapshot DB만 조회
POST /api/price-checks             -> Price -> Source의 시장 sample -> 중앙값 비교 -> 저장 없이 응답
Admin/System 뉴스 수집·발행        -> Ingest -> Source(Naver News) -> 문서 저장 또는 AI/Board port
```

따라서 “Ingest가 가져오고 Catalog가 분류하며 Price가 판정한다”는 설명은 축약형이다. 실제로 외부 통신은 Source, 실행 순서와 실패 이력은 Ingest, 저장된 상품 조회 모델·offer·검색은 Catalog, 시간별 가격 기록과 비교 계산은 Price가 맡는다.

<a id="source"></a>
## Source (내부 계약)

공개 HTTP endpoint는 없다. Ingest와 Price가 Java port를 호출한다.

| 흐름 | 입력 계약 — 필요한 이유 | routing·처리 | 출력 계약 | 실패·현재 상태 |
| --- | --- | --- | --- | --- |
| 상품 검색 | `ProductSourceQuery(source, keyword, displayCount)` — provider 선택, 검색어, 최대 결과 수. source `null`은 `MOCK`, count는 기본 10·최대 100 | `RoutingSourceRequestExecutor`가 `SourceType`을 지원하는 client 한 개를 선택 | `ProductSourceResult.items`; 각 item은 외부 ID, 제목, brand/maker, category 3단계, 가격, image/product URL, mall | 일반 환경의 MOCK은 기본 활성이고 9,900원 sample 한 건 반환; production은 기본 비활성. NAVER Shopping은 종료되어 항상 `UnsupportedOperationException` |
| 뉴스 검색 | `NewsSourceQuery(keyword, displayCount, sort)` — 검색어, 최대 결과 수, provider 정렬. count는 기본 10·최대 100, sort 기본 `date` | 현재는 단일 `NaverNewsSourceClient`가 API HUB 인증 header, `/search/v1/news` 호출, HTML 제거·길이 제한·metric 기록 수행 | `NewsSourceResult.items`; 정제 title/description/link/originalLink/publishedAt와 추적용 raw title/description | 기본 비활성. 활성화 시 API key ID/key가 없으면 시작 설정 검증 실패; 비활성 상태 호출이나 runtime credential 이상은 source 예외 |

Source는 “수집을 실행할지” 결정하지 않고, 결과를 DB에 저장하거나 상품을 분류·병합하지도 않는다. 따라서 Source를 외부 데이터 보관소로 이해하면 안 되고, 외부 provider를 교체 가능하게 만드는 anti-corruption boundary로 이해하는 것이 정확하다.

<a id="catalog"></a>
## Catalog

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `GET /api/products` | PUBLIC | pageable 기본 `size=20`, `sort=createdAt,DESC` — 활성 상품을 분할·정렬 조회 | `200 PageResponse<ProductResponse>` | 지원하지 않는 sort property·내부 조회 오류 `500` |
| `GET /api/products/search` | PUBLIC | query `query` 필수 존재 — 이름 검색어, blank면 전체 또는 category 필터만 적용; `categoryId?` 범위 제한; pageable 기본값 동일 | `200 PageResponse<ProductResponse>` | `query` 누락·`categoryId` 변환 실패 `400 INVALID_INPUT`; 지원하지 않는 sort property `500` 가능 |
| `GET /api/products/{productId}` | PUBLIC | path `productId` — 단일 상품 식별 | `200 ProductResponse`; 현재 status 필터가 없어 `HIDDEN`도 직접 조회 가능 | `404 RESOURCE_NOT_FOUND` |
| `GET /api/products/categories` | PUBLIC | 파라미터 없음 — 활성 상품이 있는 category만 조회 | `200 List<ProductCategoryResponse>` | 내부 조회 오류 `500` |
| `GET /api/products/categories/{categoryId}` | PUBLIC | path `categoryId` 범위, pageable 기본값 동일 | `200 PageResponse<ProductResponse>`; 존재하지 않는 category도 빈 page | 숫자가 아닌 경로 `404 RESOURCE_NOT_FOUND`; 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/admin/products` | ADMIN | body `ProductUpsertRequest` — source 외부 ID를 기준으로 상품·offer 생성 또는 갱신 | `201 ProductResponse`; 갱신도 `201`, 기존 `HIDDEN`은 `ACTIVE`로 복귀; 선택적 matching 장애는 상품 저장을 막지 않음 | `400 VALIDATION_ERROR`; 인증 `401/403`; unique 충돌 `409 DATA_INTEGRITY_VIOLATION`; 저장 오류 `500` |
| `PATCH /api/admin/products/{productId}/hide` | ADMIN | path `productId` — 공개 목록에서 숨길 상품 식별 | `200 MessageResponse` | 인증 `401/403`; `404 RESOURCE_NOT_FOUND` |
| `GET /api/admin/product-match-candidates` | ADMIN | query `status?` 후보 상태 필터, pageable 기본 `page=0`, `size=20`, 정렬 없음 | `200 PageResponse<ProductMatchCandidateResponse>` | 잘못된 enum `400 INVALID_INPUT`; 인증 `401/403`; 지원하지 않는 sort property `500` 가능 |
| `PATCH /api/admin/product-match-candidates/{candidateId}/approve` | ADMIN | path `candidateId` — 승인할 매칭 후보 식별 | `200 MessageResponse`; 현재 이전 상태 제약 없이 `APPROVED`로 변경 | 타입 오류 `400 INVALID_INPUT`; 인증 `401/403`; `404 RESOURCE_NOT_FOUND` |
| `PATCH /api/admin/product-match-candidates/{candidateId}/reject` | ADMIN | path `candidateId` — 거절할 매칭 후보 식별 | `200 MessageResponse`; 현재 이전 상태 제약 없이 `REJECTED`로 변경 | 타입 오류 `400 INVALID_INPUT`; 인증 `401/403`; `404 RESOURCE_NOT_FOUND` |

### 요청 DTO와 파라미터 이유

| `ProductUpsertRequest` field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `source` | 필수, non-blank | 외부 provider 구분 및 `externalProductId`와 복합 식별자 구성 |
| `externalProductId` | 필수, non-blank | 같은 외부 상품을 갱신하고 offer를 중복 생성하지 않기 위한 키 |
| `name` | 필수, non-blank | 상품 표시명, 정규화 검색, 유사 상품 matching 입력 |
| `brand`, `maker` | 선택 | 상품 표시와 matching 보조 신호 |
| `category1`, `category2`, `category3` | 선택 | source category 원문 보존. 가장 구체적인 non-blank 단계가 내부 category 이름이며 모두 비면 `기타` |
| `currentPrice` | 필수, 0 이상 | 대표 상품에 표시할 현재가. 현재 `Offer`에는 가격 필드가 없어 source별 가격으로 보존되지는 않음 |
| `imageUrl` | 선택 | 상품 이미지 위치 |
| `productUrl` | 선택 | 원본 상품 페이지 위치 |
| `mallName` | 선택 | 판매처 표시 |

`catalog`는 category 전용 모듈이 아니라 저장된 상품 조회 모델, source별 offer, 검색·매칭 후보를 소유한다. NFKC 기반 이름 정규화와 `(source, externalProductId)` 중복 방지는 하지만, 아직 여러 source의 동일 상품을 완전한 canonical truth로 병합한다고 보기는 어렵다. `ProductCategory`도 현재 상품 필터용 1단계 분류다. 입력의 `category3 → category2 → category1` 중 가장 구체적인 값 하나를 root(`depth=0`)로 저장하며, `parentId`를 연결하는 실제 계층 구성은 아직 구현하지 않았다.

현재 cross-source 자동 매칭은 모델 일관성 문제가 있다. 기존 product에 다른 source offer가 매칭되면 `Product.source/externalProductId`는 기존 source를 가리키면서 `currentPrice/mallName`은 새 source 값으로 덮일 수 있고, `Offer` 자체에는 가격이 없다. 또한 match candidate의 approve/reject는 상태만 바꾸며 상품 병합이나 offer 이동은 수행하지 않는다. 따라서 이 상태를 “정규화된 상품 truth”라고 명세하지 않는다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `ProductResponse` | `id`, `source`, `externalProductId`, `name`, `normalizedName`, `brand`, `maker`, `categoryId`, `categoryName`, `category1`, `category2`, `category3`, `currentPrice`, `imageUrl`, `productUrl`, `mallName`, `status`, `createdAt`, `updatedAt` |
| `ProductCategoryResponse` | `id`, `name`, `parentId`, `depth` |
| `ProductMatchCandidateResponse` | `id`, `sourceProductId`, `source`, `externalProductId`, `sourceProductTitle`, `candidateProductId`, `similarityScore`, `brandMatched`, `categoryMatched`, `status`, `createdAt` |

<a id="price"></a>
## Price

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `GET /api/products/{productId}/prices` | PUBLIC | path `productId` 이력 대상, query `from?`, `to?` ISO date-time 기간. 두 값이 모두 있을 때만 기간 필터 적용 | `200 List<PriceSnapshotResponse>`; 둘 다 있으면 기간 내 `ASC`, 하나라도 없으면 해당 값도 무시하고 전체 `DESC`; 없는 상품도 빈 배열 | 날짜 형식 오류 `400 INVALID_INPUT`; 내부 조회 오류 `500` |
| `POST /api/price-checks` | USER | body `PriceCheckRequest` — 비교 검색어와 사용자의 실질 결제 가격 계산 | 설계 계약은 `200 PriceCheckResponse`; 현재 정상 입력도 source 종료로 도달 불가 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; 현재 모든 유효 요청 `503 EXTERNAL_SERVICE_UNAVAILABLE` |

### 요청 DTO와 파라미터 이유

| `PriceCheckRequest` field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `keyword` | 필수, non-blank | 비교 상품 sample을 검색할 키워드 |
| `candidateUrl` | 선택 | 후보 상품 근거 URL로 받은 값. 현재 판정 로직에서는 사용하지 않는 예약 필드 |
| `basePrice` | 선택, 0 이상 | `finalPaidPrice`가 없을 때 계산의 기준 가격 |
| `shippingFee` | 선택, 0 이상; 계산 기본값 0 | `basePrice`에 더해 실질 가격 계산 |
| `discountAmount` | 선택, 0 이상; 계산 기본값 0 | 실질 가격에서 차감 |
| `finalPaidPrice` | 선택, 0 이상 | 있으면 다른 가격 구성보다 우선하는 최종 결제 예정액 |

`finalPaidPrice`가 없으면 `basePrice`가 필수이며, 계산값 `basePrice + shippingFee - discountAmount`는 0 이상이어야 한다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `PriceSnapshotResponse` | `id`, `productId`, `offerId`, `source`, `externalProductId`, `price`, `collectedAt` |
| `PriceCheckResponse` | `judgement`, `confidence`, `candidateEffectivePrice`, `shippingIncludedVerified`, `message`, `basis`, `items` |
| `PriceCheckBasisResponse` | `provider`, `sampleSize`, `medianPrice`, `lowThreshold`, `highThreshold` |
| `PriceCheckItemResponse` | `title`, `mallName`, `price`, `link` |

<a id="price-judgement-policy"></a>
### 판정 정책과 현재 제한

- Naver Shopping 검색 API 종료 대응으로 현재 `NaverShoppingSourceClient`는 검색을 지원하지 않으며, 유효한 가격 판정 요청은 `503 EXTERNAL_SERVICE_UNAVAILABLE`이다.
- 상품 수집 때 만드는 snapshot은 별도 계산값이 아니라 Catalog upsert 직후의 `product.currentPrice`다. 같은 시점의 `offerId`, source 외부 ID와 함께 저장해 상품별 변화 이력을 만든다.
- 대체 source 연결 후 가격 판정은 양수인 sample을 가격순으로 정렬하고 중앙값을 구한다. 홀수면 가운데 값, 짝수면 가운데 두 값 평균의 반올림값이다. 후보 실질 가격이 중앙값의 95% 미만이면 `CHEAP`, 105% 초과면 `EXPENSIVE`, 그 사이는 `NORMAL`이다.
- 후보 가격이 두 경계 중 하나와 3,000원 이내면 확인되지 않은 배송비가 결과를 뒤집을 수 있으므로 `INSUFFICIENT_INFO`로 낮춘다.
- 배송비 포함 여부를 확정하지 못하는 현재 계산은 `confidence=LOW`, `shippingIncludedVerified=false`를 사용한다.
- 이 API는 response-only이며 `price_snapshots`, `posts`, `post_reference_links`를 만들지 않는다.

<a id="ingest"></a>
## Ingest

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `POST /api/ingest/documents` | ADMIN | body `List<DocumentIngestRequest>` — 여러 원문을 공용 RAG 저장소에 chunk·embedding 저장. 사용자별 소유권·격리가 없어 운영자만 허용 | `200 DocumentIngestResult`; 빈 배열 금지 제약은 없고 vector store 저장 성공 시 0건 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; vector store `503 DOCUMENT_STORE_UNAVAILABLE` |
| `POST /api/admin/tracked-keywords` | ADMIN | body `ProductCollectionTargetRequest` — scheduler가 반복 수집할 source·검색어·건수 등록 | `201 TrackedKeywordResponse`; 같은 source+trimmed keyword면 기존 값을 갱신·재활성화하지 않고 반환 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; 경합 충돌 `409 DATA_INTEGRITY_VIOLATION` |
| `GET /api/admin/tracked-keywords` | ADMIN | 파라미터 없음 — 활성·비활성 추적 대상을 모두 운영 조회 | `200 List<TrackedKeywordResponse>` | 인증 `401/403`; 내부 조회 오류 `500` |
| `PATCH /api/admin/tracked-keywords/{id}/disable` | ADMIN | path `id` — 비활성화할 추적 키워드 식별 | `200 MessageResponse` | 인증 `401/403`; `404 RESOURCE_NOT_FOUND` |
| `POST /api/admin/collection-jobs/manual` | ADMIN | body `ProductCollectionTargetRequest` — 즉시 실행할 source·검색어·최대 수집 건수 | `200 CollectionJobResponse`; source 실패도 HTTP `200`이고 body `status=FAILED` | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; job 자체 저장 실패 `500` |
| `GET /api/admin/collection-jobs` | ADMIN | pageable 기본 `size=20`, `sort=requestedAt,DESC` — 실행 이력을 최신순 분할 조회 | `200 PageResponse<CollectionJobResponse>` | 인증 `401/403`; 지원하지 않는 sort property `500` 가능 |
| `POST /api/admin/news-documents/manual` | ADMIN | body `ProductNewsIngestRequest` — keyword+topic query로 뉴스 수집 후 vector 문서화 | `200 ProductNewsIngestResponse`; 검색 결과가 없으면 count 0 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; vector store `503 DOCUMENT_STORE_UNAVAILABLE`; source 비활성·외부 runtime 오류는 현재 `500` |
| `POST /api/admin/launch-news/manual` | ADMIN | body `LaunchNewsPublishRequest` — 뉴스 후보 검색·gate·일일 한도·게시 출처 지정; manual origin은 `ADMIN_BACKFILL` | `200 LaunchNewsPublishResponse`; 중복·광고·AI 생성 실패·일일 한도는 `skips`에 포함 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; DB 충돌 `409`; source 비활성·외부 runtime 오류는 현재 `500` |

### 요청 DTO와 파라미터 이유

| DTO.field | 제약·기본값 | 필요한 이유 |
| --- | --- | --- |
| `DocumentIngestRequest.content` | DTO는 필수·non-blank 선언. 현재 controller의 list element cascade 검증은 별도 보장 없음 | chunk와 embedding을 만들 원문 |
| `DocumentIngestRequest.source` | 선택 | 각 chunk의 `source` metadata로 원문 출처 보존 |
| `DocumentIngestRequest.metadata` | 선택, `Map<String,String>` | product/news URL 등 검색·추적용 부가 정보 보존. `source` key가 있으면 별도 `source` 값을 덮어씀 |
| `ProductCollectionTargetRequest.source` | 선택, 기본 `MOCK` | `MOCK` 또는 `NAVER` source client routing |
| `ProductCollectionTargetRequest.keyword` | 필수, non-blank | 외부 상품 검색어이자 tracked-keyword 식별 값 |
| `ProductCollectionTargetRequest.displayCount` | 선택; tracked 등록은 null/0 이하→20·상한 없음, manual은 null→20·0 이하→10·100 초과→100 | 한 번에 요청할 상품 수 제한 |
| `ProductNewsIngestRequest.productId` | 선택, 존재 검증 없음 | 생성 문서 metadata와 응답을 catalog 상품 ID 값에 연결 |
| `ProductNewsIngestRequest.keyword` | 필수, non-blank | 모든 뉴스 query의 기준어 |
| `ProductNewsIngestRequest.displayCount` | 선택, 기본 5, 실행 시 1~20으로 clamp | topic별 뉴스 요청 건수 제한 |
| `ProductNewsIngestRequest.topics` | 선택, 최대 10개·각 30자 | keyword에 붙일 검색 주제. `null`/빈 배열이면 기본 6개(`신제품/출시/공개/사전예약/가격/리뷰`), 제공값이 모두 null/blank면 keyword 단독 query |
| `LaunchNewsPublishRequest.keyword` | 필수, non-blank | 후보 검색, 중복·일일 한도 집계 기준 |
| `LaunchNewsPublishRequest.productId` | 선택 | 생성 게시글의 근거 링크를 catalog 상품과 연결 |
| `LaunchNewsPublishRequest.displayCount` | 선택, 1~20, 기본 10 | topic별 후보 검색 건수 |
| `LaunchNewsPublishRequest.dailyCap` | 선택, 1~20, 기본 3 | 같은 keyword+product+발행일 게시 수 제한 |
| `LaunchNewsPublishRequest.topics` | 선택, 최대 10개·각 30자 | keyword에 붙일 후보 검색 주제. `null`/빈 배열이거나 null·blank 제거 후 비면 기본 4개(`신제품/출시/공개/사전예약`); 그 외 trim·중복 제거 후 최대 10개 사용 |

`tracked_keywords`는 사용자 검색 기록이 아니라 scheduler의 영속 실행 설정이다. ADMIN이 source·keyword·건수를 한 번 등록하면 상품 수집 scheduler와 출시뉴스 발행 scheduler가 활성 항목을 실행 대상으로 사용한다. disable은 이력과 job 연결을 보존한 채 다음 실행에서 제외한다.

현재 출시뉴스 scheduler는 tracked keyword의 `source`를 무시하고 `keyword`, `displayCount`만 사용하며 `productId`도 연결하지 않는다. 두 scheduler를 모두 켜면 모든 활성 keyword가 상품 수집과 출시뉴스 검색에 함께 쓰인다. 두 작업의 대상어가 달라지는 시점에는 별도 table을 만들기보다 먼저 `purpose` 같은 구분값을 추가해 실행 대상을 필터링하는 편이 단순하다.

두 scheduler는 모두 명시적으로 enabled property를 `true`로 설정해야 bean이 생기므로 기본은 꺼져 있다. 상품 수집은 `ingest.product.scheduler.enabled=true`일 때 기본 매시 정각, 출시뉴스는 `ingest.news.launch.scheduler.enabled=true`일 때 기본 매시 30분에 활성 tracked keyword를 순회한다. 수동 endpoint는 scheduler 설정과 무관하게 즉시 실행한다.

`SourceType.NAVER` 상품 수집은 호환을 위해 enum에 남아 있지만 source가 종료되어 manual collection 결과가 `status=FAILED`가 된다. `MOCK`도 환경 설정에서 비활성화할 수 있으며 production 기본값은 비활성이다. 내부 `failureReason`은 API 응답에 노출하지 않는다.

Naver News source가 비활성이면 두 news endpoint 호출은 현재 `500 INTERNAL_SERVER_ERROR`다. `enabled=true`인데 credential이 없으면 endpoint 호출 전 애플리케이션 설정 검증 단계에서 시작이 실패한다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `DocumentIngestResult` | `documentCount`, `chunkCount` |
| `TrackedKeywordResponse` | `id`, `source`, `keyword`, `displayCount`, `enabled`, `createdAt`, `updatedAt` |
| `CollectionJobResponse` | `id`, `trackedKeywordId`, `source`, `keyword`, `status`, `requestedAt`, `startedAt`, `finishedAt`, `collectedCount` |
| `ProductNewsIngestResponse` | `keyword`, `productId`, `queries`, `newsCount`, `chunkCount` |
| `LaunchNewsPublishResponse` | `keyword`, `publishedCount`, `skippedCount`, `createdPostIds`, `skips` |
| `LaunchNewsPublishResponse.SkipResponse` | `url`, `reason` |

### 주요 enum

| Enum | 값 |
| --- | --- |
| `PostCategory` | `GENERAL`, `AI_ANALYSIS`, `PRODUCT_LAUNCH_NEWS` |
| `PostPublishOrigin` | `USER`, `SYSTEM_BATCH`, `ADMIN_BACKFILL` |
| `PurchaseVoteType` | `BUYABLE`, `UNSURE`, `WAIT` |
| `PostReferenceProvider` | `NAVER_NEWS` |
| `ProductStatus` | `ACTIVE`, `HIDDEN` |
| `ProductMatchStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `PriceJudgement` | `CHEAP`, `NORMAL`, `EXPENSIVE`, `INSUFFICIENT_INFO` |
| `PriceCheckConfidence` | `HIGH`, `MEDIUM`, `LOW`; 현재 판정 코드는 `LOW` 사용 |
| `SourceType` | `MOCK`, `NAVER` |
| `CollectionJobStatus` | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED` |
| `LaunchNewsSkipReason` | `DUPLICATE_ARTICLE`, `ADVERTISING`, `UNKNOWN_SOURCE`, `MISSING_LAUNCH_KEYWORD`, `AI_GENERATION_FAILED`, `DAILY_CAP_EXCEEDED` |
