# PostForge API 명세

현재 `@RestController`, 요청·응답 DTO, `PostForgeAuthorizationRules`, 전역 예외 처리기를 기준으로 한 단일 명세다. Spring Security가 소유하는 `/oauth2/**`, `/login/oauth2/**`와 Swagger/static 경로는 제외한다.

현재 제품 흐름은 뉴스 수집·분야별 선별·LLM 초안 작성·자동 게시와 전날 뉴스의 데일리 종합 게시다. 메일 구독 API는 아직 구현되지 않았다.

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

`PageResponse<T>`는 Board 전용이 아니라 `core`에 둔 공용 페이지 응답이다. Board·Ingest의 pageable 조회가 `content`, `page`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last`, `empty`를 같은 형태로 반환한다.

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
| `503` | `EXTERNAL_SERVICE_UNAVAILABLE`, `DOCUMENT_STORE_UNAVAILABLE` | AI vector 검색 장애, 문서 vector store 장애 |

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

클라이언트가 직접 호출하는 AI endpoint는 이 RAG 채팅 하나다. 별도 검색 endpoint는 없고, `ChatService`가 내부적으로 vector 유사 문서 검색을 수행한다. 뉴스·데일리 초안 생성은 admin 수동 게시 또는 scheduler가 내부 port로 호출한다.

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
| `GET /api/posts` | PUBLIC | `keyword?` 제목·본문 검색, `category?` 게시글 종류 필터, `boardCategory?` 분야 필터, `publishOrigin?` 사용자·batch·backfill 출처 필터, pageable 기본 `size=20`, `sort=createdAt,DESC`; 선택 JWT는 `isLiked` 계산에 사용 | `200 PageResponse<PostDetailResponse>` | 잘못된 enum `400 INVALID_INPUT`; 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts` | USER | body `PostRequest` — 일반 게시글 본문·태그·첨부 지정; JWT account ID는 작성자 | `201 PostSummaryResponse` | `400 VALIDATION_ERROR`; 인증·계정 상태 `401/403`; 작성자 `404 USER_NOT_FOUND`; 충돌 `409` |
| `GET /api/posts/{postId}` | PUBLIC | path `postId` — 조회할 게시글 식별. 선택 JWT가 있으면 개인화 및 사용자별 조회수 증가 | `200 PostDetailResponse` | `404 POST_NOT_FOUND`; 숫자가 아닌 경로 `404 RESOURCE_NOT_FOUND` |
| `PUT /api/posts/{postId}` | USER | path `postId` 대상 식별, body `PostRequest` 새 상태. 작성자 또는 ADMIN만 허용 | `200 PostSummaryResponse` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `DELETE /api/posts/{postId}` | USER | path `postId` — 삭제할 게시글과 연결 파일·조회수 정리 대상 식별. 작성자 또는 ADMIN만 허용 | `200 MessageResponse` | 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `POST /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID 중복 방지·개인 상태 | `200 LikeResponse` | 인증 `401/403`; `404 POST_NOT_FOUND`; cooldown·분당 한도·guard 장애 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID의 좋아요만 제거 | `200 LikeResponse` | 인증 `401/403`; `404 POST_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `GET /api/posts/{postId}/comments` | PUBLIC | path `postId` 댓글 묶음, pageable 기본 `size=50`, `sort=createdAt,ASC`; 선택 JWT는 `isLiked` 계산 | `200 PageResponse<CommentDetailResponse>`; 댓글이 없으면 빈 page | 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts/{postId}/comments` | USER | path `postId` 작성 대상, body `CommentRequest`, JWT account ID 작성자 | `201 CommentSummaryResponse` | `400 VALIDATION_ERROR/INVALID_COMMENT_PARENT/MAX_COMMENT_DEPTH_EXCEEDED`; 인증·계정 `401/403`; `404 POST_NOT_FOUND/COMMENT_NOT_FOUND` |
| `PUT /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 수정 대상, body `CommentRequest`의 `content` 사용. 작성자 또는 ADMIN만 허용 | `200 CommentSummaryResponse` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `DELETE /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 삭제 대상. 작성자 또는 ADMIN만 허용 | `200 MessageResponse` | 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `POST /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID 개인 상태 | `200 LikeResponse` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID의 좋아요 제거 | `200 LikeResponse` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `GET /api/user/profile` | USER | body 없음. JWT account ID로 상세 프로필 조회 | `200 ProfileResponse` | 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `PATCH /api/user/profile/nickname` | USER | body `ProfileNicknameUpdateRequest` — 새 공개 닉네임 | `200 MessageResponse` | `400 VALIDATION_ERROR`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND`; `409 DUPLICATE_NICKNAME` |
| `PATCH /api/user/profile/password` | USER | body `ProfilePasswordUpdateRequest` — 현재 비밀번호 확인 후 변경 | `200 MessageResponse`; refresh token 폐기 | `400 VALIDATION_ERROR/INVALID_PASSWORD/OAUTH_PASSWORD_UPDATE_NOT_ALLOWED`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `GET /api/files/presigned-url` | USER | query `fileName` 확장자·저장명 생성, `contentType` 실제 업로드 MIME 서명. `/api/files/s3/presigned-url` 별칭도 동일 | `200 FileUploadResponse`; URL TTL 5분 | 누락 `400 INVALID_INPUT`; `400 FILE_EXTENSION_NOT_ALLOWED/FILE_TYPE_MISMATCH`; 인증 `401/403`; S3/DB 오류 `500` |
| `GET /api/files/{fileId}/download-url` | USER | path `fileId` — 저장 object key를 조회. `/api/files/s3/{fileId}/download-url` 별칭도 동일 | `200 UrlResponse`; URL TTL 5분 | 타입 오류 `400 INVALID_INPUT`; 인증 `401/403`; `404 FILE_NOT_FOUND`; S3 오류 `500` |

### HTTP method 선택 기준

| Method | 의미 | 이 프로젝트의 예 |
| --- | --- | --- |
| `POST` | 대상 collection이나 처리기에 payload를 맡긴다. 보통 서버가 새 ID를 정하며 같은 요청을 반복하면 새 결과가 생길 수 있다 | `POST /api/posts`, 로그인, 수동 수집 |
| `PUT` | client가 수정 권한을 가진 target 표현의 원하는 전체 상태를 설정한다. 같은 요청을 반복해도 최종 상태가 같아야 한다 | 게시글 편집 상태 설정 |
| `PATCH` | 기존 target에 변경분만 적용한다. 요청에 없는 속성을 보존하도록 서버가 merge해야 하며 method 자체가 이를 자동 보장하지 않는다 | nickname 변경, 고정 상태 전이 |
| `DELETE` | target resource 또는 관계를 제거한다 | 게시글 삭제, 좋아요 관계 삭제 |

`/api/posts`와 중첩 `/comments`는 collection URI다. `GET`은 collection 조회, `POST`는 그 안에 단일 resource 생성이므로 `/posts/create`처럼 동사까지 넣으면 `POST`와 URI가 모두 `create`를 말하는 의미 중복이 생긴다. 요청이 두 번 실행된다는 뜻은 아니다.

좋아요의 별도 `GET`도 DB가 같은 요청을 두 번 실행한다는 뜻이 아니다. 게시글·댓글 상세가 이미 `isLiked`와 `likeCount`를 반환하므로, `/like` 조회를 추가하면 같은 상태를 노출하는 API 계약이 하나 더 생긴다는 뜻이다. 좋아요만 가볍게 polling할 실제 요구가 생기기 전에는 추가하지 않는다.

현재 좋아요의 `POST`/`DELETE`는 관계 생성/삭제로 해석할 수 있다. 다만 관계 key가 `(targetId, accountId)`로 이미 결정되므로 신규 계약은 `PUT /like`와 `DELETE /like`가 더 정확하다. 이 경우 현재 cooldown이 동일한 `PUT` 재호출을 `429`로 막지 않도록 함께 바꿔야 진짜 idempotent 계약이 된다.

### PUT과 PATCH의 현재 구현 범위

`PATCH`의 “부분 수정”은 요청에서 지정한 값만 바꾸고, 빠진 값은 기존 값을 유지한다는 뜻이다. 예를 들어 현재 값이 `{nickname: "rat", email: "a@example.com", roles: ["USER"]}`일 때 `{ "nickname": "newRat" }`를 적용하면 email과 roles는 그대로 남는다. 반면 full-replacement `PUT`이라면 빠진 email과 roles를 삭제·기본화하거나 불완전한 요청으로 거절하는 것이 일반적인 계약이다.

PUT/PATCH의 기준은 ID·작성자·생성일 같은 서버 관리 필드를 보존하느냐가 아니다. client가 관리하는 편집 필드를 매번 완전한 상태로 보내면 PUT이고, 일부 필드만 보내며 누락 필드를 유지해야 하면 PATCH다. PATCH로 바꾸려면 annotation만 교체할 것이 아니라 DTO가 `누락`과 `명시적 null/빈 값`을 구분하고 service가 기존 값과 merge하도록 바꿔야 한다.

현재 `PUT /api/posts/{postId}`는 client가 편집 가능한 `title`, `content`, `tags`, `fileIds` 묶음을 교체한다. `tags`를 생략하면 빈 목록이 되고 `fileIds`를 생략하면 기존 첨부가 모두 해제되므로, edit client는 네 필드를 전체 전송해야 한다. `summary`, `publishOrigin`은 서버 관리 필드로 유지하지만 category는 무조건 `GENERAL`로 바꾼다. 따라서 ADMIN이 system 출시뉴스를 수정하면 category가 바뀌는 결함이 있다. 댓글 `PUT`도 생성용 `CommentRequest`를 재사용해 `parentId`를 받지만 수정에서는 무시하고 `content`만 바꾼다. 게시글 category는 보존하고, 댓글 수정에는 `content`만 가진 별도 DTO를 쓰는 것이 현재 의도에 맞다.

Board의 게시글·댓글 path ID는 `\d+` 경로만 매핑한다. 숫자가 아니면 controller에 도달하지 않고 `404 RESOURCE_NOT_FOUND`다. `PUT/DELETE` 댓글 경로의 `postId`는 현재 서비스 계층에서 `commentId`와의 소속 관계 검증에 사용되지 않는다. 호출자는 실제 댓글의 게시글 ID를 넣어야 한다.

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `PostRequest.title` | 필수, 2~100자, `<`·`>` 금지 | 목록·상세 제목과 기본 검색 대상 |
| `PostRequest.content` | 필수, 10~10000자 | 게시글 본문 |
| `PostRequest.tags` | 선택, 최대 20개·각 50자 | 게시글 분류·표시용 태그 |
| `PostRequest.fileIds` | 선택 | presigned URL 발급 때 만든 파일 레코드를 게시글에 연결. 현재 저장소 조회 결과에 없는 ID는 연결되지 않음 |
| `CommentRequest.parentId` | 선택 | 대댓글의 바로 위 댓글 식별. 없으면 최상위 댓글 |
| `CommentRequest.content` | 필수, 2~500자, 위험 HTML 패턴 차단 | 댓글 본문 |
| `ProfileNicknameUpdateRequest.nickname` | 필수, 2~20자 한글·영문·숫자·`_` | 변경할 공개 표시명 |
| `ProfilePasswordUpdateRequest.currentPassword` | 필수 | 본인 확인 |
| `ProfilePasswordUpdateRequest.newPassword` | 필수, 8~100자 | 새 password hash 입력 |

사용자 게시글 생성·수정은 요청에서 category를 받지 않고 항상 `GENERAL`, `publishOrigin=USER`로 저장한다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `PostDetailResponse` | `id`, `title`, `content`, `summary`, `tags`, `category`, `boardCategory`, `publishOrigin`, `accountId`, `nickname`, `views`, `commentCount`, `likeCount`, `isLiked`, `references`, `files`, `createdAt`, `modifiedAt` |
| `PostSummaryResponse` | `id`, `title`, `summary`, `tags`, `category`, `publishOrigin`, `accountId`, `nickname`, `createdAt`, `modifiedAt` |
| `PostReferenceLinkResponse` | `id`, `keyword`, `provider`, `canonicalUrl`, `originalUrl`, `sourceName`, `publishedAt`, `titleSnapshot`, `publishOrigin` |
| `FileInfoResponse` | `fileId`, `originalFileName`, `fileType` |
| `CommentDetailResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `replyCount`, `likeCount`, `isLiked`, `createdAt`, `modifiedAt` |
| `CommentSummaryResponse` | `id`, `content`, `accountId`, `nickname`, `parentId`, `createdAt`, `modifiedAt` |
| `LikeResponse` | `isLiked`, `likeCount` |
| `ProfileResponse` | `accountId`, `username`, `email`, `nickname`, `provider`, `isOAuthUser`, `roles`, `createdAt`, `updatedAt` |
| `FileUploadResponse` | `fileId`, `savedName`, `url` |

## Source (내부 계약)

공개 HTTP endpoint는 없다. Ingest가 Java port를 호출한다.

| 흐름 | 입력 계약 — 필요한 이유 | routing·처리 | 출력 계약 | 실패·현재 상태 |
| --- | --- | --- | --- | --- |
| 뉴스 검색 | `NewsSourceQuery(keyword, displayCount, sort)` — 검색어, 최대 결과 수, provider 정렬. count는 기본 10·최대 100, sort 기본 `date`, 허용값 `date`/`sim` | 현재는 단일 `NaverNewsSourceClient`가 API HUB 인증 header, `/search/v1/news` 호출, 강조 태그 제거·텍스트 길이 제한·URL 검증·metric 기록 수행 | `List<NewsSourceItem>`; 정제 title/description/link/originalLink/publishedAt와 추적용 raw title/description | 기본 비활성. 활성화 시 API key ID/key가 없으면 시작 설정 검증 실패; 비활성 상태 호출이나 runtime credential 이상은 source 예외 |

Source는 “수집을 실행할지” 결정하지 않고, 결과를 DB에 저장하지도 않는다. 따라서 Source를 외부 데이터 보관소로 이해하면 안 되고, 외부 provider를 교체 가능하게 만드는 anti-corruption boundary로 이해하는 것이 정확하다.

일반 테스트는 실서버를 호출하지 않는다. 실 API 확인은 `./gradlew :source:test -PnaverSmoke=true --tests '*NaverNewsSmokeTest'`로 명시적으로 실행한다. 이때만 루트 `.env`의 네이버 인증값을 읽으며 셸 환경변수가 우선한다. 플래그나 인증값이 없으면 스모크는 건너뛴다.

## Ingest

### 엔드포인트

| Endpoint | Auth | Parameters — 필요한 이유 | Success | Fail |
| --- | --- | --- | --- | --- |
| `POST /api/ingest/documents` | ADMIN | body `List<DocumentIngestRequest>` — 여러 원문을 공용 RAG 저장소에 chunk·embedding 저장. 사용자별 소유권·격리가 없어 운영자만 허용 | `200 DocumentIngestResult`; 빈 배열 금지 제약은 없고 vector store 저장 성공 시 0건 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; vector store `503 DOCUMENT_STORE_UNAVAILABLE` |
| `POST /api/admin/news-documents/manual` | ADMIN | body `ProductNewsIngestRequest` — keyword+topic query로 뉴스 수집 후 vector 문서화 | `200 ProductNewsIngestResponse`; 검색 결과가 없으면 count 0 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; vector store `503 DOCUMENT_STORE_UNAVAILABLE`; source 비활성·외부 runtime 오류는 현재 `500` |
| `POST /api/admin/launch-news/manual` | ADMIN | body `LaunchNewsPublishRequest` — 뉴스 수집·벡터 적재·gate·RAG 초안·일일 한도·게시 출처 지정; manual origin은 `ADMIN_BACKFILL` | `200 LaunchNewsPublishResponse`; 중복·광고·AI 생성 실패·일일 한도는 `skips`에 포함 | `400 VALIDATION_ERROR/INVALID_INPUT`; 인증 `401/403`; DB 충돌 `409`; source 비활성·외부 runtime 오류는 현재 `500` |
| `POST /api/admin/news/digest` | ADMIN | body `DailyDigestPublishRequest` 선택(생략 가능) — `newsDate`의 출시뉴스 게시글을 분야별로 요약해 데일리 브리핑 게시 | `200 DailyDigestPublishResponse`; 소스 없음·이미 게시·AI 생성 실패는 분야별 `skips`에 포함 | `400 INVALID_INPUT`(날짜 파싱 실패); 인증 `401/403` |

### 요청 DTO와 파라미터 이유

| DTO.field | 제약·기본값 | 필요한 이유 |
| --- | --- | --- |
| `DocumentIngestRequest.content` | DTO는 필수·non-blank 선언. 현재 controller의 list element cascade 검증은 별도 보장 없음 | chunk와 embedding을 만들 원문 |
| `DocumentIngestRequest.source` | 선택 | 각 chunk의 `source` metadata로 원문 출처 보존 |
| `DocumentIngestRequest.metadata` | 선택, `Map<String,String>` | news URL 등 검색·추적용 부가 정보 보존. `source` key가 있으면 별도 `source` 값을 덮어씀 |
| `ProductNewsIngestRequest.keyword` | 필수, non-blank | 모든 뉴스 query의 기준어 |
| `ProductNewsIngestRequest.displayCount` | 선택, 1~100, 기본 5 | topic별 뉴스 요청 건수 제한 |
| `ProductNewsIngestRequest.topics` | 선택, 최대 10개·각 30자 | keyword에 붙일 검색 주제. `null`/빈 배열이면 기본 5개(`신제품/출시/공개/사전예약/리뷰`), 제공값이 모두 null/blank면 keyword 단독 query |
| `LaunchNewsPublishRequest.keyword` | 필수, non-blank | 후보 검색, 중복·일일 한도 집계 기준 |
| `LaunchNewsPublishRequest.displayCount` | 선택, 1~100, 기본 10 | topic별 후보 검색 건수 |
| `LaunchNewsPublishRequest.dailyCap` | 선택, 1~20, 기본 3 | 같은 keyword+발행일 게시 수 제한 |
| `LaunchNewsPublishRequest.topics` | 선택, 최대 10개·각 30자 | keyword에 붙일 후보 검색 주제. `null`/빈 배열이거나 null·blank 제거 후 비면 기본 4개(`신제품/출시/공개/사전예약`); 그 외 trim·중복 제거 후 최대 10개 사용 |
| `LaunchNewsPublishRequest.category` | 선택, 기본 `GENERAL` | 생성 게시글에 저장할 분야(`BoardCategory`) 지정 |
| `DailyDigestPublishRequest.newsDate` | 선택, 기본 어제(Asia/Seoul clock) | 요약 대상 출시뉴스 게시글의 작성일. 분야+날짜로 제목을 만들어 재실행 시 기존 글을 확인 |

### 자동 게시 스케줄

두 scheduler는 **Asia/Seoul** 시간대를 사용하며 기본 비활성이다. 수동 endpoint는 scheduler 설정과 무관하게 즉시 실행한다.

| 작업 | 기본 주기 | 활성화 조건 | 주기 환경변수 |
| --- | --- | --- | --- |
| 뉴스 수집·초안 작성·자동 게시 | 매시 30분 (`0 30 * * * *`) | `NAVER_NEWS_ENABLED=true`, `INGEST_NEWS_LAUNCH_SCHEDULER_ENABLED=true` | `INGEST_NEWS_LAUNCH_CRON` |
| 전날 뉴스의 데일리 포스트 게시 | 매일 **06:00** (`0 0 6 * * *`) | `INGEST_NEWS_DIGEST_SCHEDULER_ENABLED=true` | `INGEST_NEWS_DIGEST_CRON` |

설정 위치는 `ingest.news.launch.scheduler.enabled`·`ingest.news.launch.cron`, `ingest.news.digest.scheduler.enabled`·`ingest.news.digest.cron`이다. 기본값과 전체 환경변수는 [application.yml](../../app/src/main/resources/application.yml), [.env.example](../../.env.example)을 따른다. 활성화 전 Naver 인증, LLM, PostgreSQL/PgVector 연결과 수집 키워드를 준비한다. 데일리 실행 자체는 이미 게시된 글과 LLM을 사용하므로 Naver 수집이 꺼져 있어도 실행할 수 있다.

`tracked_keywords`는 뉴스 scheduler의 영속 실행 설정이다. 등록 API는 없고 DB에 직접 넣는다. 활성 row의 `keyword`, `displayCount`, `category`를 사용하며, 분야는 이 `category`(수동 게시에서는 요청의 `category`)로 정한다. LLM이 분야를 자동 판정하는 기능은 현재 없다. LLM은 선별된 기사의 본문·요약·태그 초안을 작성한다.

뉴스 작업은 스케줄 실행 안에서 수집→벡터 적재→게시 후보 선별→LLM 초안→게시까지 처리한다. 수집 건수(`displayCount`, topic당 최대 100)와 게시 건수(`dailyCap`, 기본 3)는 별개다. 동일 수집 결과를 적재와 게시 후보에 함께 사용한다. 초안을 저장해 별도 시각에 발행하는 예약 대기열은 없다.

데일리는 **전날 00:00 이상, 당일 00:00 미만에 작성된 `PRODUCT_LAUNCH_NEWS` 게시글**의 제목·요약을 분야별로 종합해 `DAILY_DIGEST`로 게시한다. 기사 원문의 발행일이나 수집 원문 전체를 기준으로 하지 않는다. 수집 완료와 별개로 06:00에 실행하며, Naver·벡터 검색을 다시 호출하지 않는다. 대상 글이 없는 분야와 이미 데일리가 게시된 분야는 건너뛴다. 메일 발송은 향후 계획이다.

게시 흐름은 적재 실패 시 중단한다. 적재 후 RAG 검색이나 LLM 생성이 실패하면 해당 기사는 `AI_GENERATION_FAILED`로 건너뛰고 게시 완료 기록을 남기지 않는다. 같은 요청의 재실행은 원본 자료를 재적재할 수 있지만, 이미 게시한 URL은 중복 gate에서 제외한다. 정상 검색 결과가 비어 있으면 주 기사 정보로만 초안을 작성한다.

Naver News source가 비활성이면 두 news endpoint 호출은 현재 `500 INTERNAL_SERVER_ERROR`다. `enabled=true`인데 credential이 없으면 endpoint 호출 전 애플리케이션 설정 검증 단계에서 시작이 실패한다.

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `DocumentIngestResult` | `documentCount`, `chunkCount` |
| `ProductNewsIngestResponse` | `keyword`, `queries`, `newsCount`, `chunkCount` |
| `LaunchNewsPublishResponse` | `keyword`, `publishedCount`, `skippedCount`, `createdPostIds`, `skips` |
| `LaunchNewsPublishResponse.SkipResponse` | `url`, `reason` |
| `DailyDigestPublishResponse` | `newsDate`, `publishedCount`, `skippedCount`, `createdPostIds`, `skips`(분야 → `DailyDigestSkipReason`) |

### 주요 enum

| Enum | 값 |
| --- | --- |
| `PostCategory` | `GENERAL`, `DAILY_DIGEST`(분야별 데일리 뉴스 브리핑, system이 게시), `PRODUCT_LAUNCH_NEWS` |
| `BoardCategory` | `GENERAL`, `DIGITAL`, `APPLIANCE`, `LIVING`, `HEALTH`, `BEAUTY`, `SPORTS` |
| `PostPublishOrigin` | `USER`, `SYSTEM_BATCH`, `ADMIN_BACKFILL` |
| `PostReferenceProvider` | `NAVER_NEWS` |
| `LaunchNewsSkipReason` | `DUPLICATE_ARTICLE`, `ADVERTISING`, `UNKNOWN_SOURCE`, `MISSING_LAUNCH_KEYWORD`, `AI_GENERATION_FAILED`, `DAILY_CAP_EXCEEDED` |
| `DailyDigestSkipReason` | `NO_SOURCE`, `ALREADY_PUBLISHED`, `AI_GENERATION_FAILED` |
