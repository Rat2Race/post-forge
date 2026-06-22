# Redis 연결 장애

## 영향

Redis 장애는 단순 캐시 미스가 아니라 인증과 요청 보호에 직접 영향을 준다.

| 영역 | Redis 사용처 | 장애 정책 |
| --- | --- | --- |
| Refresh token | `refresh_token:{accountId}` 검증/저장/삭제 | fail-closed |
| OAuth2 exchange code | `oauth2_code:{code}` 60초 TTL, one-time exchange | fail-closed |
| 이메일 인증 | token/state/cooldown/rate/lock | fail-closed |
| 로그인 보호 | `auth:login:*` rate/fail/lock | fail-closed, `429` |
| 조회수 | `post:views:*`, `post:viewed:*`, dirty set | degraded, 마지막 미동기화 증가분 유실 가능 |
| 좋아요 요청 guard | `like:*` cooldown/rate limit | fail-closed |

## 증상

- `/actuator/health`에서 Redis down 또는 app health가 `UP`이 아닌 상태로 보인다.
- 로그인, 토큰 재발급, OAuth2 exchange, 이메일 인증, 좋아요 요청이 실패 계층에 따라 `429`, `401`, `5xx`를 반환한다.
- 로그에 Redis connection, timeout, command latency, serialization 오류가 남는다.
- 조회수 증가가 멈추거나 Redis 재시작 후 DB 기준값에서 다시 시작한다.

## 확인 절차

1. Redis container 또는 service가 실행 중인지 확인한다.
2. 앱의 Redis host, port, password, profile 환경값을 확인한다.
3. app container 또는 host에서 `/actuator/health`를 확인한다.
4. 최종 HTTP status만 보지 말고 app log에서 최초 Redis 예외를 찾는다.
5. Docker Compose 환경이면 앱 시작 전 PostgreSQL과 Redis healthcheck가 healthy인지 확인한다.
6. auth만 실패하면 `refresh_token:*`, `oauth2_code:*`, 이메일 인증 key TTL 동작을 확인한다.
7. board 조회수만 영향받으면 `post:views:dirty`와 scheduler log를 확인한다.

## 복구

- 미동기화 조회수 증가분 유실을 감수할 수 있는지 판단한 뒤 Redis를 재시작한다.
- Redis 재시작 후 client connection pool이 회복되지 않으면 앱을 재시작한다.
- refresh token state가 유실되었다면 영향받은 사용자는 다시 로그인하게 한다.
- 반복적인 auth abuse traffic은 Redis fail-closed 정책을 낮추기보다 edge rate limit을 먼저 추가한다.

## 관련 문서

- [Redis 캐시 전략](../architecture/redis-cache-strategy.md)
- `auth/src/main/resources/docs/auth.md`
- `support/src/main/java/dev/iamrat/support/redis/RedisGuardOperations.java`
