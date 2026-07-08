# Auth API 흐름

Endpoint 명세는 [Auth API](../../api/auth.md)가 canonical이다.

## 모듈 구조

```text
auth
├── account   계정 조회/변경 (AccountQueryService, AccountCommandService)
├── email     이메일 인증 (EmailVerificationService + Redis TTL 상태)
├── login     로그인/로그아웃 (LoginService)
├── oauth     OAuth2 exchange code (OAuth2LoginService, OAuth2CodeService)
├── register  회원가입 (RegisterService)
├── token     JWT 발급/재발급 (TokenService, TokenIssuer, RefreshTokenStore)
└── security  Spring Security filter chain, OAuth2 success handler
```

원리: access token은 stateless JWT, refresh token은 Redis stateful. MVP에서는 refresh token, 이메일 인증, OAuth2 exchange code처럼 기능에 필요한 짧은 수명 상태만 Redis에 둔다. 결정 기록: [ADR-002](../../decisions/adr-002-refresh-token-rotation.md)

## POST /api/auth/email/send — 인증 메일 발송

1. `EmailVerificationService.sendVerificationEmail`이 email을 normalize한다.
2. `accounts` 테이블에서 email 중복이면 `409`.
3. UUID token을 `email_verify_token:{token}`에 30분 TTL로 저장하고 메일을 발송한다.

## GET /api/auth/email/verify — 인증 확인

1. `emailVerificationStore.getEmailAndDeleteToken(token)` — Redis `getAndDelete`로 token을 **1회성 소비**한다. 없으면 `404`.
2. `email_verified:{email}`을 1시간 TTL로 마킹한다.

## POST /api/auth/register — 회원가입

1. `RegisterService.register` (`@Transactional`)가 `email_verified:{email}` 상태를 확인한다. 없으면 거절.
2. username/nickname/email 중복 확인 후 `accounts` + `account_roles` 저장. 비밀번호는 BCrypt 해시.
3. 성공 후 `email_verified:{email}`을 삭제해 인증 상태 재사용을 막는다.

## POST /api/auth/login — 로그인

1. `AuthenticationManager.authenticate` — `CustomUserDetailsService`가 계정을 로드하고 BCrypt 비교. 실패하면 `401`.
2. 성공하면 `TokenService.createToken` — access/refresh 발급, refresh는 `refresh_token:{accountId}`에 저장.
3. 컨트롤러가 access token은 body로, refresh token은 `HttpOnly; Secure; SameSite=Lax; Path=/api/auth` cookie로 반환한다.

## POST /api/auth/token/reissue — 재발급 (rotation)

1. cookie의 refresh token을 parse해 subject에서 `accountId`를 얻는다.
2. `refreshTokenStore.validate` — Redis 저장값과 요청 token을 **상수 시간 비교**로 검증. 불일치면 `401`.
3. 계정 활성 상태와 role을 DB에서 다시 조회한다. 비활성이면 `403`.
4. 새 access/refresh를 발급하고 Redis 값을 새 refresh로 **rotation**(overwrite)한다.

알려진 한계: validate와 replace가 단일 원자 연산이 아니라 동시 재발급 시 마지막 저장 token만 살아남는다. 상세와 후속 과제는 [ADR-002](../../decisions/adr-002-refresh-token-rotation.md).

## POST /api/auth/logout

`tokenService.deleteToken(accountId)` — Redis refresh token 삭제 + cookie `Max-Age=0` 응답. 이미 발급된 access token은 stateless라 만료까지 유효하다(즉시 폐기가 필요하면 blacklist 같은 추가 상태 필요).

## OAuth2 로그인 → POST /api/auth/oauth2/exchange

1. provider callback은 Spring Security OAuth2 flow가 처리하고, `OAuth2SuccessHandler`가 계정을 연결/조회한다.
2. `OAuth2CodeService.createCode` — UUID code를 `oauth2_code:{code}`에 60초 TTL로 저장하고, frontend redirect URL에는 `?code=`만 붙인다.
3. frontend가 exchange를 호출하면 `getAndDelete`로 code를 1회성 소비하고 일반 JWT 발급 흐름(`TokenService.createToken`)으로 합류한다.

원리: redirect URL에 토큰을 노출하지 않기 위한 one-time handoff. 실패 사례는 [OAuth2 상태 흐름](../../troubleshooting/oauth2-state-flow.md).

## 계정 변경 (PATCH /api/user/account/*)

- nickname: 중복 확인 후 갱신. 기존 게시글/댓글의 nickname snapshot은 갱신하지 않는다(정책: [account-policy](../../policy/account-policy.md)).
- password: 현재 비밀번호 검증 → 해시 저장 → `refreshTokenStore.delete(accountId)`로 기존 refresh token 폐기(재로그인 유도). OAuth 전용 계정은 거절.

## 장애 정책

auth의 Redis 사용처는 인증 기능 상태다. Redis 장애 시 refresh token 재발급, 이메일 인증, OAuth2 exchange code 흐름이 실패할 수 있다. 복구 절차: [Redis 연결 장애](../../troubleshooting/redis-connection-failure.md)
