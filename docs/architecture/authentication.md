# Authentication Architecture

이 문서는 인증/인가 흐름과 운영 경계를 설명한다. HTTP endpoint, request/response DTO, validation 상세는 [통합 API 명세의 Auth](../api/README.md#auth)를 canonical source로 둔다. 단계별 요청 흐름은 [요청 흐름](./flows.md#auth)에 있다.

## Runtime Ownership

| 영역 | 소유 모듈 | 비고 |
| --- | --- | --- |
| 계정/권한 | `auth` | `accounts`, `account_roles` |
| JWT 발급/검증 | `auth` | access token은 stateless, refresh token은 Redis stateful |
| OAuth2 callback | `auth` + Spring Security | provider callback 이후 frontend에는 one-time exchange code만 전달 |
| route authorization | `app` | `PostForgeAuthorizationRules`에서 조립 |
| MVC 인증 예외 변환 | `auth` | `SecurityExceptionHandler`가 인증 예외를 `ErrorResponse`로 변환 |
| Security filter 오류 응답 | `auth` | `JwtAuthenticationEntryPoint` 401, `JwtAccessDeniedHandler` 403 |
| 공통 MVC 예외 변환 | `support` | `ExceptionResponseHandler`가 공통 예외와 최종 fallback 처리 |
| 공통 principal 계약 | `core` | `UserPrincipal` 기반 account id 사용 |
| Redis infrastructure | `support` | key ownership은 `auth`에 남김 |

## Core Rules

- 권한과 소유권 판단은 username이나 nickname이 아니라 `accountId`를 기준으로 한다.
- refresh token은 Redis에 저장하고 재발급 시 rotation한다.
- OAuth2 redirect URL에는 refresh token이나 access token을 노출하지 않는다.
- 로그인 실패, 이메일 인증, OAuth2 exchange code는 Redis guard/TTL state로 보호한다.
- refresh token, 로그인 guard, 이메일 인증, OAuth2 exchange code의 Redis state 장애는 인증 안전성을 위해 fail-closed로 처리한다.
- 계정 비활성 상태는 login, token reissue, OAuth2 exchange에서 거절한다.
- 존재하지 않는 username과 잘못된 password는 모두 `INVALID_CREDENTIALS`로 응답해 계정 존재 여부를 노출하지 않는다.

## Error Handling Paths

인증 오류는 발생 위치에 따라 MVC Advice 또는 Security filter handler가 응답한다.

| 발생 위치/예외 | 처리 클래스 | 응답 |
| --- | --- | --- |
| MVC `BadCredentialsException` | `SecurityExceptionHandler` | `401 INVALID_CREDENTIALS` |
| MVC `UsernameNotFoundException` | `SecurityExceptionHandler` | `401 INVALID_CREDENTIALS` |
| MVC `DisabledException` | `SecurityExceptionHandler` | `403 ACCOUNT_NOT_ACTIVE` |
| MVC `AccessDeniedException` | `SecurityExceptionHandler` | `403 ACCESS_DENIED` |
| 보호 경로의 미인증 요청 | `JwtAuthenticationEntryPoint` | `401 UNAUTHORIZED` |
| 인증됐지만 권한이 부족한 요청 | `JwtAccessDeniedHandler` | `403 FORBIDDEN` |
| 그 밖의 MVC 예외 | `support.ExceptionResponseHandler` | 각 공통 오류 코드 또는 최종 `500 INTERNAL_SERVER_ERROR` |

`SecurityExceptionHandler`는 `HIGHEST_PRECEDENCE`, 공통 `ExceptionResponseHandler`는 `LOWEST_PRECEDENCE`다. 따라서 인증 예외는 auth 정책이 먼저 처리하고, 매핑되지 않은 MVC 예외만 공통 handler로 넘어간다.

`JwtAuthenticationFilter`는 유효하지 않거나 만료된 JWT를 발견하면 예외를 MVC로 전달하지 않고 `SecurityContext`를 비운 뒤 filter chain을 계속 진행한다. 보호 경로라면 이후 `JwtAuthenticationEntryPoint`가 401을 반환하고, 공개 경로라면 익명 요청으로 계속 처리한다.

## Token Boundary

- access token은 stateless이며 API 응답 후 `Authorization` header로 사용한다.
- refresh token은 Redis에 저장하는 stateful credential이며 재발급 때 rotation한다.
- OAuth2 handoff code는 짧은 TTL의 1회성 값이다.

cookie 속성과 response header의 HTTP 계약은 [통합 API 명세의 Token/Cookie](../api/README.md#token--cookie)를 따른다.

## Browser Session Boundary

같은 출처의 화면(`/study.html`, `/index.html`)은 refresh cookie(경로 `/api/auth`)와 localStorage(`pf.token`·`pf.name`·`pf.session`)를 함께 쓴다. 두 화면은 [`static/auth.js`](../../app/src/main/resources/static/auth.js) 하나로 인증한다.

- 로그인·로그아웃·재발급은 같은 이름의 Web Lock(`pf-auth`)으로 탭 사이에서도 한 줄로 세운다. 응답의 `Set-Cookie`는 JS가 결과를 버려도 적용되므로, 이전 세션의 재발급 응답이 다른 계정의 로그인보다 늦게 오면 새 refresh cookie를 덮는다. 잠금은 앞 요청의 응답을 다 받은 뒤에 다음 요청을 보내게 해 이 순서를 막는다. Web Locks가 없는 비보안 출처에서는 탭 안의 순서만 지킨다.
- 로그인마다 새 세션 표식(`pf.session`)을 정하고 로그아웃 때 지운다. 요청을 보낼 때와 401을 받을 때 표식이 다르면 그 요청을 새 토큰으로 다시 보내지 않고, 늦게 온 재발급 토큰도 버린다.
- 로그아웃은 서버의 refresh token도 지운다. access token이 만료됐으면 잠금 안에서 재발급받아 지운다.
- 서버의 검증·교체가 단일 원자 연산이 아니라는 한계([ADR-002](../decisions/adr-002-refresh-token-rotation.md))는 그대로다. 잠금은 같은 브라우저 안의 순서만 지키며, 원자적 rotation은 서버에서 따로 다룬다.

