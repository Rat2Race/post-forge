# Authentication Architecture

이 문서는 인증/인가 흐름과 운영 경계를 설명한다. HTTP endpoint, request/response DTO, validation 상세는 [Auth API](../api/auth.md)를 canonical source로 둔다.

## Runtime Ownership

| 영역 | 소유 모듈 | 비고 |
| --- | --- | --- |
| 계정/권한 | `auth` | `accounts`, `account_roles` |
| JWT 발급/검증 | `auth` | access token은 stateless, refresh token은 Redis stateful |
| OAuth2 callback | `auth` + Spring Security | provider callback 이후 frontend에는 one-time exchange code만 전달 |
| route authorization | `app` | `PostForgeAuthorizationRules`에서 조립 |
| 공통 principal 계약 | `core` | `UserPrincipal` 기반 account id 사용 |
| Redis infrastructure | `support` | key ownership은 `auth`에 남김 |

## Core Rules

- 권한과 소유권 판단은 username이나 nickname이 아니라 `accountId`를 기준으로 한다.
- refresh token은 Redis에 저장하고 재발급 시 rotation한다.
- OAuth2 redirect URL에는 refresh token이나 access token을 노출하지 않는다.
- 로그인 실패, 이메일 인증, OAuth2 exchange code는 Redis guard/TTL state로 보호한다.
- OAuth2 또는 이메일 인증 Redis state 장애는 인증 안전성을 위해 fail-closed로 처리한다.
- 계정 비활성 상태는 login, token reissue, OAuth2 exchange에서 거절한다.

## Main Flows

### Email Registration

1. 사용자가 이메일 인증 메일 발송을 요청한다.
2. `auth`가 이메일 중복과 Redis 발송 제한을 확인한다.
3. 인증 token을 Redis TTL 상태로 저장하고 메일을 보낸다.
4. 사용자가 인증 링크를 열면 token을 1회성으로 소비하고 이메일 인증 완료 상태를 Redis에 남긴다.
5. 회원가입은 인증 완료 상태를 확인한 뒤 `accounts`와 `account_roles`를 저장한다.

### Password Login

1. 사용자가 username/password로 로그인한다.
2. 로그인 guard가 사용자/IP별 rate, failure, lock 상태를 확인한다.
3. 성공하면 access token을 body로 반환하고 refresh token을 `refresh_token` cookie와 Redis에 저장한다.
4. 실패하면 Redis 실패 상태를 갱신한다.

### Token Reissue

1. 클라이언트가 `refresh_token` cookie로 재발급을 요청한다.
2. `auth`가 JWT parse, Redis 저장값, 계정 활성 상태를 확인한다.
3. 성공하면 access token과 새 refresh token을 발급하고 Redis 값을 rotation한다.

### OAuth2 Login

1. Provider callback은 Spring Security OAuth2 login flow가 처리한다.
2. 성공 handler가 account를 연결하거나 조회한다.
3. `OAuth2CodeService`가 짧은 수명의 one-time exchange code를 Redis에 저장한다.
4. Backend는 frontend redirect URL에 `?code=`만 붙인다.
5. Frontend가 exchange API를 호출하면 code를 `getAndDelete` 방식으로 소비하고 JWT/refresh token을 발급한다.

## Cookie And Token Boundary

| 항목 | 정책 |
| --- | --- |
| Access token | response body, `Authorization: Bearer`로 사용 |
| Refresh token | `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/auth` cookie |
| Token response cache | `Cache-Control: no-store`, `Pragma: no-cache` |
| OAuth2 handoff code | Redis TTL, one-time consume |

## Operational Checks

- Redis 장애 시 refresh reissue, OAuth2 exchange, 이메일 인증, 로그인 guard가 실패할 수 있다.
- OAuth2 장애는 provider redirect URI, `app.oauth2.redirect-url`, Redis `oauth2_code:*` TTL, frontend duplicate exchange를 먼저 확인한다.
- 인증 API 상세는 중복 작성하지 않고 [Auth API](../api/auth.md), Redis 장애 대응은 [Redis 연결 장애](../troubleshooting/redis-connection-failure.md), OAuth2 장애 대응은 [OAuth2 상태 흐름](../troubleshooting/oauth2-state-flow.md)을 따른다.
