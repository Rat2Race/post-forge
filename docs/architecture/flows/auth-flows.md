# Auth API 흐름

Endpoint 명세는 [통합 API 명세의 Auth](../../api/README.md#auth), Redis key 소유권은 [DB Schema Ownership](../../database/schema-ownership.md#non-relational-storage)이 canonical이다. 세부 제한값은 `auth` 구현과 설정을 따른다.

인증 소유권, 오류 처리, token/cookie 경계는 [Authentication Architecture](../authentication.md), refresh token 선택 근거는 [ADR-002](../../decisions/adr-002-refresh-token-rotation.md)를 따른다.

## POST /api/auth/email/send — 인증 메일 발송

1. `EmailVerificationService.sendVerificationEmail`이 email을 normalize한다.
2. `EmailVerificationRequestGuard.guard` — Redis Lua script로 cooldown/rate/lock 상태를 원자적으로 평가한다. 초과 시 `429`.
3. `accounts` 테이블에서 email 중복이면 `409`.
4. UUID token을 TTL과 함께 저장하고 메일을 발송한다.

원리: 발송 guard를 Lua script 하나로 평가해 "cooldown 확인 → rate 증가 → lock 생성"이 요청 경쟁 상황에서도 일관되게 동작한다. 메일 발송은 비용/스팸 벡터라서 fail-closed다.

## GET /api/auth/email/verify — 인증 확인

1. `emailVerificationStore.getEmailAndDeleteToken(token)` — Redis `getAndDelete`로 token을 **1회성 소비**한다. 없으면 `404`.
2. 인증 완료 상태를 TTL과 함께 마킹한다.

## POST /api/auth/register — 회원가입

1. `RegisterService.register` (`@Transactional`)가 `email_verified:{email}` 상태를 확인한다. 없으면 거절.
2. username/nickname/email 중복 확인 후 `accounts` + `account_roles` 저장. 비밀번호는 BCrypt 해시.
3. 성공 후 `email_verified:{email}`을 삭제해 인증 상태 재사용을 막는다.

## POST /api/auth/login — 로그인

1. `LoginService.login`이 `LoginAttemptGuard.guard(username, clientIp)`를 먼저 실행해 Redis 로그인 보호 상태를 확인한다. 초과/잠금이면 `429`.
2. `AuthenticationManager.authenticate` — `CustomUserDetailsService`가 계정을 로드하고 BCrypt 비교. 실패하면 `loginAttemptGuard.recordFailure`로 실패 누적(한도 도달 시 lock 생성) 후 `401`.
3. 성공하면 실패 기록을 지우고 `TokenService.createToken` — access/refresh 발급, refresh는 `refresh_token:{accountId}`에 저장.
4. 컨트롤러가 access token과 refresh token을 [정해진 경계](../authentication.md#cookie-and-token-boundary)에 따라 반환한다.

원리: BCrypt는 의도적으로 CPU-heavy하므로 로그인은 인증 전 단계의 저비용 guard로 보호한다. 부하 분석 근거는 [k6 시나리오 결과](../../performance/k6-scenario-results.md)다.

## POST /api/auth/token/reissue — 재발급 (rotation)

1. cookie의 refresh token을 parse해 subject에서 `accountId`를 얻는다.
2. `refreshTokenStore.validate` — Redis 저장값과 요청 token을 **상수 시간 비교**로 검증. 불일치면 `401`.
3. 계정 활성 상태와 role을 DB에서 다시 조회한다. 비활성이면 `403`.
4. 새 access/refresh를 발급하고 Redis 값을 새 refresh로 **rotation**(overwrite)한다.

동시 재발급의 원자성 한계와 후속 과제는 [ADR-002](../../decisions/adr-002-refresh-token-rotation.md)에 둔다.

## POST /api/auth/logout

`tokenService.deleteToken(accountId)` — Redis refresh token 삭제 + cookie `Max-Age=0` 응답. 이미 발급된 access token은 stateless라 만료까지 유효하다(즉시 폐기가 필요하면 blacklist 같은 추가 상태 필요).

## OAuth2 로그인 → POST /api/auth/oauth2/exchange

1. provider callback은 Spring Security OAuth2 flow가 처리하고, `OAuth2SuccessHandler`가 계정을 연결/조회한다.
2. `OAuth2CodeService.createCode` — UUID code를 짧은 TTL로 저장하고, frontend redirect URL에는 `?code=`만 붙인다.
3. frontend가 exchange를 호출하면 `getAndDelete`로 code를 1회성 소비하고 일반 JWT 발급 흐름(`TokenService.createToken`)으로 합류한다.

원리: redirect URL에 토큰을 노출하지 않기 위한 one-time handoff. 실패 사례는 [OAuth2 상태 흐름](../../troubleshooting/oauth2-state-flow.md).

## 계정 변경 (PATCH /api/user/account/*)

- nickname: 중복 확인 후 갱신. 기존 게시글/댓글의 nickname snapshot은 갱신하지 않는다(정책: [account-policy](../../policy/account-policy.md)).
- password: 현재 비밀번호 검증 → 해시 저장 → `refreshTokenStore.delete(accountId)`로 기존 refresh token 폐기(재로그인 유도). OAuth 전용 계정은 거절.

장애 정책과 오류 응답 경계는 [Authentication Architecture](../authentication.md), 복구 절차는 [Redis 연결 장애](../../troubleshooting/redis-connection-failure.md)를 따른다.
