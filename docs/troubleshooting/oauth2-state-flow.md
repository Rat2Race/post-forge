# OAuth2 상태 흐름

## 현재 흐름

PostForge는 provider OAuth2 로그인을 backend에서 완료한 뒤, 짧은 수명의 exchange code를 프론트엔드로 redirect한다.

1. OAuth provider가 Spring Security callback으로 redirect한다.
2. `OAuth2SuccessHandler`가 인증된 principal을 받는다.
3. `OAuth2CodeService.createCode`가 UUID code를 만든다.
4. `RedisOAuth2CodeStore`가 `oauth2_code:{code}`를 60초 TTL로 저장한다.
5. backend가 `app.oauth2.redirect-url`에 `?code={code}`를 붙여 redirect한다.
6. frontend가 code를 담아 `POST /auth/oauth2/exchange`를 호출한다.
7. `OAuth2CodeService.exchangeCode`는 Redis `getAndDelete`를 사용하므로 code는 1회만 쓸 수 있다.
8. `OAuth2LoginService`가 일반 JWT access token과 refresh token을 발급한다.

## 실패 사례

| 증상 | 가능성 높은 원인 | 확인 |
| --- | --- | --- |
| exchange에서 `INVALID_TOKEN` | code 만료, 이미 사용됨, Redis에 없음, 저장값이 잘못됨 | Redis key TTL과 frontend retry 동작 |
| redirect URL에 `error=`가 있음 | provider 인증 실패 또는 success handler 예외 | OAuth2 failure log와 provider 응답 |
| 사용자가 잘못된 frontend URL로 돌아감 | `app.oauth2.redirect-url` 불일치 | profile별 config |
| 로컬은 되지만 운영에서 실패 | HTTPS/cookie/SameSite/CORS/redirect URI 불일치 | provider console과 frontend origin |
| timeout 후 재교환이 실패 | 1회용 `getAndDelete`가 이미 code를 소비함 | frontend idempotency와 retry UX |

## 상태 메모

이 흐름은 OAuth redirect URL에 refresh token을 노출하지 않는다.
redirect code는 짧은 수명의 1회용 값이며, frontend가 auth API를 통해 교환한다.
OAuth 로그인 중 사용된 Spring HTTP session은 exchange code 생성 후 무효화된다.

Provider `state` 검증은 여전히 Spring Security OAuth2 login flow가 담당한다.
PostForge가 추가로 관리하는 handoff state는 Redis exchange code다.

## 운영자 체크리스트

- provider redirect URI가 backend OAuth callback route와 일치하는지 확인한다.
- `app.oauth2.redirect-url`이 배포된 frontend가 기대하는 callback route를 가리키는지 확인한다.
- Redis가 healthy이고 `oauth2_code:*` key를 60초 동안 유지하는지 확인한다.
- frontend가 code 하나당 `POST /auth/oauth2/exchange`를 한 번만 호출하는지 확인한다.
- clock skew 또는 긴 redirect 시간이 60초 code TTL을 넘지 않는지 확인한다.

## 관련 문서

- `auth/src/main/java/dev/iamrat/auth/security/infrastructure/handler/OAuth2SuccessHandler.java`
- `auth/src/main/java/dev/iamrat/auth/oauth/application/OAuth2CodeService.java`
- `auth/src/main/java/dev/iamrat/auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java`
- `auth/src/main/resources/docs/auth.md`
