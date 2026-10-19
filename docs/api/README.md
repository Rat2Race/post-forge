# PostForge API 명세

현재 `@RestController`, 요청·응답 DTO, `PostForgeAuthorizationRules`, 전역 예외 처리기를 기준으로 한 단일 명세다. Spring Security가 소유하는 `/oauth2/**`, `/login/oauth2/**`와 Swagger/static 경로는 제외한다.

제품은 게시판 서비스다.

## 공통 계약

### 인증 표기

| 표기 | 호출 조건 |
| --- | --- |
| `PUBLIC` | 인증 없이 호출 가능. 일부 조회는 JWT가 있으면 개인화하며 엔드포인트 설명에 별도 표기 |
| `USER` | access token이 필요한 사용자 영역. `ROLE_USER` 전용(`ROLE_ADMIN`만 있는 토큰은 `403 ACCESS_DENIED`)과 소유자/ADMIN 조건은 각 엔드포인트 설명에 별도 표기하고, 표기가 없으면 `ROLE_USER/ROLE_ADMIN` 공용 |
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

`PageResponse<T>`는 Board 전용이 아니라 `core`에 둔 공용 페이지 응답이다. Board의 pageable 조회가 `content`, `page`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last`, `empty`를 같은 형태로 반환한다.

Spring pageable resolver는 일부 잘못된 `page`/`size` 값을 `0`, 엔드포인트 기본값 또는 최대값으로 보정한다. 반면 지원하지 않는 `sort` property는 현재 repository 단계에서 `500`이 될 수 있다.

### Success

| 공통 DTO | 필드 |
| --- | --- |
| `PageResponse<T>` | 위 pagination 필드와 `content` |

### Fail

전역·보안 handler가 처리하는 애플리케이션 오류는 다음 형태를 사용한다. `validation`은 Bean Validation 실패일 때만 포함된다. 잘못된 method(`405 METHOD_NOT_ALLOWED`, `Allow` 헤더 포함)와 지원하지 않는 `Content-Type`(`415 UNSUPPORTED_MEDIA_TYPE`)도 같은 형태다. 보안 경로 규칙에 없는 method·경로 조합은 그 전에 `401`/`403`으로 막힌다.

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
| `405` | `METHOD_NOT_ALLOWED` | 경로는 있지만 그 method는 지원하지 않음. `Allow` 헤더가 지원 method를 알린다 |
| `409` | `DATA_INTEGRITY_VIOLATION`, `CONCURRENT_MODIFICATION` 또는 중복 코드 | unique 제약이나 동시 수정 충돌 |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | 본문의 `Content-Type`을 읽을 수 없음(JSON 본문을 받는 API에 다른 형식) |
| `429` | `TOO_MANY_REQUESTS` | 이메일·로그인·좋아요 요청 보호 한도 초과 또는 보호 저장소 장애 시 fail-closed |
| `500` | `INTERNAL_SERVER_ERROR` 또는 모듈별 오류 | 처리되지 않은 내부·외부 연동 오류 |

각 엔드포인트의 `Fail`에는 공통 인증 실패를 반복해서 쓰지 않는다. 인증 표기가 `PUBLIC`이 아니면 `401`, 역할·소유권 조건이 있으면 `403`이 함께 적용된다.

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
| `POST /api/auth/email/send` | PUBLIC | body `SendEmailRequest` — 인증 대상 이메일과 중복 여부를 확인하고 일회용 링크를 발송 | `204 No Content` | `400 VALIDATION_ERROR`; `409 DUPLICATE_EMAIL`; `429 TOO_MANY_REQUESTS`; `500 EMAIL_SEND_FAILED` |
| `GET /api/auth/email/verify` | PUBLIC | query `token` 필수 — 발송된 일회용 토큰을 이메일과 연결하고 사용 후 제거 | `200 EmailVerificationResponse` | 누락 `400 INVALID_INPUT`; 만료·없음·이미 사용된 토큰 `404 EMAIL_CODE_NOT_FOUND` |
| `POST /api/auth/register` | PUBLIC | body `RegisterRequest` — local 계정 생성과 이메일 인증 여부·중복 검사에 사용 | `201 RegisterResponse` | `400 VALIDATION_ERROR/EMAIL_NOT_VERIFIED`; `409 DUPLICATE_USERNAME/DUPLICATE_NICKNAME/DATA_INTEGRITY_VIOLATION` |
| `POST /api/auth/login` | PUBLIC | body `LoginRequest` — 자격 증명 검증. 원격 IP는 서버가 로그인 보호에 사용 | `200 AccessTokenResponse` + refresh cookie | `400 VALIDATION_ERROR`; `401 INVALID_CREDENTIALS`; `403 ACCOUNT_NOT_ACTIVE`; `429 TOO_MANY_REQUESTS` |
| `POST /api/auth/logout` | USER | body 없음. JWT의 account ID로 저장된 refresh token을 삭제 | `204 No Content` + refresh cookie 제거 | 인증 `401/403`; 처리되지 않은 저장소 예외 `500 INTERNAL_SERVER_ERROR` |
| `POST /api/auth/token/reissue` | PUBLIC | cookie `refresh_token` 필수 — route는 공개지만 저장된 refresh 자격 증명과 대조해 탈취·폐기된 토큰의 재사용을 막음 | `200 AccessTokenResponse` + refresh cookie 회전 | 누락 `401 UNAUTHORIZED`; `401 INVALID_TOKEN/EXPIRED_TOKEN`; `403 ACCOUNT_NOT_ACTIVE`; `404 USER_NOT_FOUND` |
| `POST /api/auth/oauth2/exchange` | PUBLIC | body `OAuth2ExchangeRequest` — OAuth 성공 후 받은 일회용 code를 계정 토큰으로 교환 | `200 AccessTokenResponse` + refresh cookie | `400 VALIDATION_ERROR/INVALID_INPUT`; `401 INVALID_TOKEN`; `403 ACCOUNT_NOT_ACTIVE`; `404 USER_NOT_FOUND` |
| `GET /api/user/account` | USER | body 없음. JWT account ID로 현재 계정 조회 | `200 AccountResponse` | 인증 `401/403`; `404 USER_NOT_FOUND` |
| `PATCH /api/user/account/nickname` | USER | body `AccountUpdateRequest` — 새 공개 닉네임 지정 | `204 No Content` | `400 VALIDATION_ERROR`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND`; `409 DUPLICATE_NICKNAME` |
| `PATCH /api/user/account/password` | USER | body `PasswordUpdateRequest` — 현재 비밀번호로 본인 확인 후 새 비밀번호 저장 | `204 No Content`; 기존 refresh token 폐기 | `400 VALIDATION_ERROR/INVALID_PASSWORD/OAUTH_PASSWORD_UPDATE_NOT_ALLOWED`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `PUT /api/admin/accounts/{accountId}/roles/admin` | ADMIN | path `accountId` — 기존 관리자만 다른 활성 계정에 ADMIN 권한 부여. 본인 승격은 거절 | `204 No Content`; 이미 ADMIN이면 동일하게 성공 | 인증 `401/403`; 대상 없음 `404 USER_NOT_FOUND` |

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `SendEmailRequest.email` | 필수, email 형식; trim·lowercase 정규화 | 인증 링크의 수신자이자 verified-email key |
| `RegisterRequest.username` | 필수, 4~20자 영문·숫자 | local 로그인 식별자 |
| `RegisterRequest.password` | 필수, 최소 8자, 영문 대·소문자·숫자·`@$!%*?&`만 쓸 수 있고 각각 1자 이상 포함. 그 밖의 문자(`#`, 공백, 한글 등)가 있으면 `400 VALIDATION_ERROR` | local 계정 자격 증명 강도 보장 |
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
| `GET /api/posts` | PUBLIC | `keyword?` 제목·본문 검색, pageable 기본 `size=20`, `sort=createdAt,DESC`; 선택 JWT는 `isLiked` 계산에 사용 | `200 PageResponse<PostDetail>` | 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts` | USER | body `PostRequest` — 일반 게시글 본문·태그 지정; JWT account ID는 작성자. `ROLE_USER` 전용 | `201 PostSummary` | `400 VALIDATION_ERROR`; 인증·계정 상태 `401/403`; 작성자 `404 USER_NOT_FOUND`; 충돌 `409` |
| `GET /api/posts/{postId}` | PUBLIC | path `postId` — 조회할 게시글 식별. 선택 JWT가 있으면 개인화 및 사용자별 조회수 증가 | `200 PostDetail` | `404 POST_NOT_FOUND`; 숫자가 아닌 경로는 인증이 없으면 `401 UNAUTHORIZED`, 인증되면 `404 RESOURCE_NOT_FOUND` |
| `PUT /api/posts/{postId}` | USER | path `postId` 대상 식별, body `PostRequest` 새 상태. 작성자 또는 ADMIN만 허용 | `200 PostSummary` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `DELETE /api/posts/{postId}` | USER | path `postId` — 삭제할 게시글과 조회수 정리 대상 식별. 작성자 또는 ADMIN만 허용 | `204 No Content` | 인증·소유권 `401/403`; `404 POST_NOT_FOUND` |
| `POST /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID 중복 방지·개인 상태. `ROLE_USER` 전용 | `200 LikeResult` | 인증 `401/403`; `404 POST_NOT_FOUND`; cooldown·분당 한도·guard 장애 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/like` | USER | path `postId` 대상, JWT account ID의 좋아요만 제거. `ROLE_USER` 전용 | `200 LikeResult` | 인증 `401/403`; `404 POST_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `GET /api/posts/{postId}/comments` | PUBLIC | path `postId` 댓글 묶음, pageable 기본 `size=50`, `sort=createdAt,ASC`; 선택 JWT는 `isLiked` 계산 | `200 PageResponse<CommentDetail>`; 댓글이 없으면 빈 page | 지원하지 않는 sort property·내부 조회 오류 `500` |
| `POST /api/posts/{postId}/comments` | USER | path `postId` 작성 대상, body `CommentRequest`, JWT account ID 작성자. `ROLE_USER` 전용 | `201 CommentSummary` | `400 VALIDATION_ERROR/INVALID_COMMENT_PARENT/MAX_COMMENT_DEPTH_EXCEEDED`; 인증·계정 `401/403`; `404 POST_NOT_FOUND/COMMENT_NOT_FOUND` |
| `PUT /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 수정 대상, body `CommentRequest`의 `content` 사용. 작성자 또는 ADMIN만 허용 | `200 CommentSummary` | `400 VALIDATION_ERROR`; 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `DELETE /api/posts/{postId}/comments/{commentId}` | USER | `postId`는 중첩 URI 문맥, `commentId`가 실제 삭제 대상. 작성자 또는 ADMIN만 허용 | `204 No Content` | 인증·소유권 `401/403`; `404 COMMENT_NOT_FOUND` |
| `POST /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID 개인 상태. `ROLE_USER` 전용 | `200 LikeResult` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `DELETE /api/posts/{postId}/comments/{commentId}/like` | USER | `postId`는 URI 문맥, `commentId` 대상, JWT account ID의 좋아요 제거. `ROLE_USER` 전용 | `200 LikeResult` | 인증 `401/403`; `404 COMMENT_NOT_FOUND`; 요청 보호 `429 TOO_MANY_REQUESTS` |
| `GET /api/user/profile` | USER | body 없음. JWT account ID로 상세 프로필 조회 | `200 ProfileResponse` | 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |
| `PATCH /api/user/profile/nickname` | USER | body `ProfileNicknameUpdateRequest` — 새 공개 닉네임 | `204 No Content` | `400 VALIDATION_ERROR`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND`; `409 DUPLICATE_NICKNAME` |
| `PATCH /api/user/profile/password` | USER | body `ProfilePasswordUpdateRequest` — 현재 비밀번호 확인 후 변경 | `204 No Content`; refresh token 폐기 | `400 VALIDATION_ERROR/INVALID_PASSWORD/OAUTH_PASSWORD_UPDATE_NOT_ALLOWED`; 인증·비활성 `401/403`; `404 USER_NOT_FOUND` |

### HTTP method 선택 기준

| Method | 의미 | 이 프로젝트의 예 |
| --- | --- | --- |
| `POST` | 대상 collection이나 처리기에 payload를 맡긴다. 보통 서버가 새 ID를 정하며 같은 요청을 반복하면 새 결과가 생길 수 있다 | `POST /api/posts`, 로그인 |
| `PUT` | client가 수정 권한을 가진 target 표현의 원하는 전체 상태를 설정한다. 같은 요청을 반복해도 최종 상태가 같아야 한다 | 게시글 편집 상태 설정 |
| `PATCH` | 기존 target에 변경분만 적용한다. 요청에 없는 속성을 보존하도록 서버가 merge해야 하며 method 자체가 이를 자동 보장하지 않는다 | nickname 변경, 비밀번호 변경 |
| `DELETE` | target resource 또는 관계를 제거한다 | 게시글 삭제, 좋아요 관계 삭제 |

`/api/posts`와 중첩 `/comments`는 collection URI다. `GET`은 collection 조회, `POST`는 그 안에 단일 resource 생성이므로 `/posts/create`처럼 동사까지 넣으면 `POST`와 URI가 모두 `create`를 말하는 의미 중복이 생긴다. 요청이 두 번 실행된다는 뜻은 아니다.

좋아요의 별도 `GET`도 DB가 같은 요청을 두 번 실행한다는 뜻이 아니다. 게시글·댓글 상세가 이미 `isLiked`와 `likeCount`를 반환하므로, `/like` 조회를 추가하면 같은 상태를 노출하는 API 계약이 하나 더 생긴다는 뜻이다. 좋아요만 가볍게 polling할 실제 요구가 생기기 전에는 추가하지 않는다.

현재 좋아요의 `POST`/`DELETE`는 관계 생성/삭제로 해석할 수 있다. 다만 관계 key가 `(targetId, accountId)`로 이미 결정되므로 신규 계약은 `PUT /like`와 `DELETE /like`가 더 정확하다. 이 경우 현재 cooldown이 동일한 `PUT` 재호출을 `429`로 막지 않도록 함께 바꿔야 진짜 idempotent 계약이 된다.

### PUT과 PATCH의 현재 구현 범위

`PATCH`의 “부분 수정”은 요청에서 지정한 값만 바꾸고, 빠진 값은 기존 값을 유지한다는 뜻이다. 예를 들어 현재 값이 `{nickname: "rat", email: "a@example.com", roles: ["USER"]}`일 때 `{ "nickname": "newRat" }`를 적용하면 email과 roles는 그대로 남는다. 반면 full-replacement `PUT`이라면 빠진 email과 roles를 삭제·기본화하거나 불완전한 요청으로 거절하는 것이 일반적인 계약이다.

PUT/PATCH의 기준은 ID·작성자·생성일 같은 서버 관리 필드를 보존하느냐가 아니다. client가 관리하는 편집 필드를 매번 완전한 상태로 보내면 PUT이고, 일부 필드만 보내며 누락 필드를 유지해야 하면 PATCH다. PATCH로 바꾸려면 annotation만 교체할 것이 아니라 DTO가 `누락`과 `명시적 null/빈 값`을 구분하고 service가 기존 값과 merge하도록 바꿔야 한다.

현재 `PUT /api/posts/{postId}`는 client가 편집 가능한 `title`, `content`, `tags` 묶음을 교체한다. `tags`를 생략하면 빈 목록이 되므로 edit client는 세 필드를 전체 전송해야 한다. 댓글 `PUT`도 생성용 `CommentRequest`를 재사용해 `parentId`를 받지만 수정에서는 무시하고 `content`만 바꾼다. 댓글 수정에는 `content`만 가진 별도 DTO를 쓰는 것이 현재 의도에 맞다.

Board의 게시글·댓글 path ID는 `\d+` 경로만 매핑한다. 숫자가 아니면 controller에 도달하지 않는다. 보안 규칙도 숫자 경로만 공개하므로 인증 없이 부르면 `401 UNAUTHORIZED`, 인증된 요청은 `404 RESOURCE_NOT_FOUND`다. `PUT/DELETE` 댓글 경로의 `postId`는 현재 서비스 계층에서 `commentId`와의 소속 관계 검증에 사용되지 않는다. 호출자는 실제 댓글의 게시글 ID를 넣어야 한다.

### 요청 DTO와 파라미터 이유

| DTO.field | 제약 | 필요한 이유 |
| --- | --- | --- |
| `PostRequest.title` | 필수, 2~100자, `<`·`>` 금지 | 목록·상세 제목과 기본 검색 대상 |
| `PostRequest.content` | 필수, 10~10000자 | 게시글 본문 |
| `PostRequest.tags` | 선택, 최대 20개·각 50자 | 게시글 분류·표시용 태그 |
| `CommentRequest.parentId` | 선택 | 대댓글의 바로 위 댓글 식별. 없으면 최상위 댓글 |
| `CommentRequest.content` | 필수, 2~500자, 위험 HTML 패턴 차단 | 댓글 본문 |
| `ProfileNicknameUpdateRequest.nickname` | 필수, 2~20자 한글·영문·숫자·`_` | 변경할 공개 표시명 |
| `ProfilePasswordUpdateRequest.currentPassword` | 필수 | 본인 확인 |
| `ProfilePasswordUpdateRequest.newPassword` | 필수, 8~100자 | 새 password hash 입력 |

### 응답 DTO

| DTO | 필드 |
| --- | --- |
| `PostDetail` | `id`, `title`, `content`, `tags`, `accountId`, `nickname`, `views`, `commentCount`, `likeCount`, `isLiked`, `createdAt`, `modifiedAt` |
| `PostSummary` | `id`, `title`, `tags`, `accountId`, `nickname`, `createdAt`, `modifiedAt` |
| `CommentDetail` | `id`, `content`, `accountId`, `nickname`, `parentId`, `replyCount`, `likeCount`, `isLiked`, `createdAt`, `modifiedAt` |
| `CommentSummary` | `id`, `content`, `accountId`, `nickname`, `parentId`, `createdAt`, `modifiedAt` |
| `LikeResult` | `isLiked`, `likeCount` |
| `ProfileResponse` | `accountId`, `username`, `email`, `nickname`, `provider`, `isOAuthUser`, `roles`, `createdAt`, `updatedAt` |
