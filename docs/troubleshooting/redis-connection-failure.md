# Redis 연결 장애

## 영향

Redis 장애는 단순 캐시 미스가 아니라 인증 기능 상태에 직접 영향을 준다.

| 영역 | Redis 사용처 | 장애 정책 |
| --- | --- | --- |
| Refresh token | `refresh_token:{accountId}` 검증/저장/삭제 | fail-closed |
| OAuth2 exchange code | `oauth2_code:{code}` 60초 TTL, one-time exchange | fail-closed |
| 이메일 인증 | token/state | fail-closed |

## 증상

- `/actuator/health`에서 Redis down 또는 app health가 `UP`이 아닌 상태로 보인다.
- 토큰 재발급, OAuth2 exchange, 이메일 인증이 실패 계층에 따라 `401`, `5xx`를 반환한다.
- 로그에 Redis connection, timeout, command latency, serialization 오류가 남는다.

## 확인 절차

1. Redis container 또는 service가 실행 중인지 확인한다.
2. 앱의 Redis host, port, password, profile 환경값을 확인한다.
3. app container 또는 host에서 `/actuator/health`를 확인한다.
4. 최종 HTTP status만 보지 말고 app log에서 최초 Redis 예외를 찾는다.
5. Docker Compose 환경이면 앱 시작 전 PostgreSQL과 Redis healthcheck가 healthy인지 확인한다.
6. auth만 실패하면 `refresh_token:*`, `oauth2_code:*`, 이메일 인증 key TTL 동작을 확인한다.

## 복구

- Redis 재시작 후 client connection pool이 회복되지 않으면 앱을 재시작한다.
- refresh token state가 유실되었다면 영향받은 사용자는 다시 로그인하게 한다.

## 관련 문서

- `auth/src/main/java/dev/iamrat/auth/token/infrastructure/redis/RefreshTokenRepository.java`
- `auth/src/main/java/dev/iamrat/auth/email/infrastructure/redis/RedisEmailVerificationStore.java`
- `auth/src/main/java/dev/iamrat/auth/oauth/infrastructure/redis/RedisOAuth2CodeStore.java`
