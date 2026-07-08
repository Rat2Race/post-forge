# Auth API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/email/send` | Public | `200` | `SendEmailRequest` | `MessageResponse` |
| `GET` | `/api/auth/email/verify` | Public | `200` | query `token` | `EmailVerificationResponse` |
| `POST` | `/api/auth/register` | Public | `201` | `RegisterRequest` | `RegisterResponse` |
| `POST` | `/api/auth/login` | Public | `200` | `LoginRequest` | `AccessTokenResponse` |
| `POST` | `/api/auth/logout` | USER/ADMIN | `200` | none | `MessageResponse` |
| `POST` | `/api/auth/token/reissue` | Public + refresh cookie | `200` | cookie `refresh_token` | `AccessTokenResponse` |
| `POST` | `/api/auth/oauth2/exchange` | Public | `200` | `OAuth2ExchangeRequest` | `AccessTokenResponse` |
| `GET` | `/api/user/account` | USER/ADMIN | `200` | none | `AccountResponse` |
| `PATCH` | `/api/user/account/nickname` | USER/ADMIN | `200` | `AccountUpdateRequest` | `MessageResponse` |
| `PATCH` | `/api/user/account/password` | USER/ADMIN | `200` | `PasswordUpdateRequest` | `MessageResponse` |

## Token/Cookie

| 항목 | 값 |
| --- | --- |
| Access token header | `Authorization: Bearer {accessToken}` |
| Refresh cookie name | `refresh_token` |
| Refresh cookie attributes | `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/auth` |
| Token response headers | `Cache-Control: no-store`, `Pragma: no-cache` |

## Request DTO

| DTO | Fields |
| --- | --- |
| `SendEmailRequest` | `email` required, email format, normalized |
| `RegisterRequest` | `username` required 4-20 alphanumeric, `password` required min 8 with upper/lower/digit/special, `email` required email format, `nickname` required 2-20 Korean/English/digit/underscore |
| `LoginRequest` | `username` required 4-20 alphanumeric, `password` required |
| `OAuth2ExchangeRequest` | `code` required |
| `AccountUpdateRequest` | `nickname` required 2-20 Korean/English/digit/underscore |
| `PasswordUpdateRequest` | `currentPassword` required, `newPassword` required 8-100 |

## Response DTO

| DTO | Fields |
| --- | --- |
| `EmailVerificationResponse` | `message`, `email` |
| `RegisterResponse` | `accountId`, `message` |
| `AccessTokenResponse` | `grantType`, `accessToken` |
| `AccountResponse` | `username`, `nickname`, `provider`, `isOAuthUser`, `roles` |
| `MessageResponse` | `message` |

## Endpoint 상세

### POST `/api/auth/email/send`

이메일 인증 메일을 발송한다.

- Body: `SendEmailRequest`
- Success: `200 MessageResponse`
- 주요 오류: `400` validation, `409` duplicate email, `500` mail send failure

### GET `/api/auth/email/verify`

이메일 인증 token을 검증한다.

- Query: `token` required
- Success: `200 EmailVerificationResponse`
- 주요 오류: `400` missing token, `404` token not found or already used

### POST `/api/auth/register`

이메일 인증이 완료된 local 계정을 생성한다.

- Body: `RegisterRequest`
- Success: `201 RegisterResponse`
- 주요 오류: `400` validation/email not verified, `409` duplicate username/nickname/email

### POST `/api/auth/login`

username/password로 로그인하고 access token과 refresh cookie를 발급한다.

- Body: `LoginRequest`
- Success: `200 AccessTokenResponse`
- Headers: `Set-Cookie: refresh_token=...; Path=/api/auth; HttpOnly; Secure; SameSite=Lax`
- 주요 오류: `400` validation, `401` invalid credentials, `403` inactive account

### POST `/api/auth/logout`

현재 계정의 refresh token을 폐기하고 refresh cookie를 제거한다.

- Auth: USER/ADMIN
- Success: `200 MessageResponse`
- Headers: `Set-Cookie: refresh_token=; Max-Age=0; Path=/api/auth; HttpOnly; Secure; SameSite=Lax`

### POST `/api/auth/token/reissue`

refresh cookie로 access token과 refresh token을 재발급한다.

- Cookie: `refresh_token` required
- Success: `200 AccessTokenResponse`
- 주요 오류: `401` missing/invalid/expired refresh token, `403` inactive account, `404` account not found

### POST `/api/auth/oauth2/exchange`

OAuth2 success 이후 받은 one-time code를 access token과 refresh cookie로 교환한다.

- Body: `OAuth2ExchangeRequest`
- Success: `200 AccessTokenResponse`
- Headers: `Set-Cookie: refresh_token=...; Path=/api/auth; HttpOnly; Secure; SameSite=Lax`

### Account endpoints

- `GET /api/user/account`: 현재 계정 정보를 반환한다.
- `PATCH /api/user/account/nickname`: nickname을 변경한다.
- `PATCH /api/user/account/password`: 비밀번호를 변경한다.
