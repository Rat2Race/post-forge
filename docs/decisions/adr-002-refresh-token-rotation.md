# ADR-002 Refresh Token Rotation

## 배경

PostForge는 access token을 stateless JWT로 처리한다.
하지만 access token만으로는 로그아웃, 장기 세션 유지, 탈취 refresh token 무효화 같은 상태성 요구를 다루기 어렵다.

## 결정

refresh token은 계정별 Redis key `refresh_token:{accountId}`에 저장하고, 재발급 때마다 새 refresh token으로 rotation한다.
재발급 요청은 token subject에서 account id를 읽고 Redis 저장값과 요청 token을 상수 시간 비교로 검증한다.
검증이 끝나면 계정 활성 상태와 role을 다시 조회해 새 access token과 refresh token을 발급한다.

## 영향

- 로그아웃과 비밀번호 변경은 Redis refresh token 삭제로 다음 재발급을 차단할 수 있다.
- 탈취된 refresh token은 rotation 이후 Redis 저장값과 불일치하므로 재사용이 막힌다.
- 이미 발급된 access token은 stateless라 만료 전까지 유효하다. 즉시 폐기가 필요하면 access token blacklist, token version, logout-after timestamp 같은 추가 상태가 필요하다.
- 현재 검증과 교체는 단일 Redis 원자 연산이 아니다. 같은 refresh token으로 동시 재발급이 들어오면 둘 다 검증을 통과한 뒤 마지막 저장 token만 살아남을 수 있다.

## 후속 과제

강한 single-use rotation이 필요해지면 Redis Lua script나 compare-and-set 저장소 계약으로 `validate + replace`를 원자화한다.
클라이언트는 중복 재발급 요청 중 하나가 `INVALID_TOKEN`으로 실패할 수 있다는 흐름을 정상으로 처리해야 한다.

## 관련 문서

- `auth/src/main/java/dev/iamrat/auth/token/application/TokenService.java`
- `auth/src/main/java/dev/iamrat/auth/token/infrastructure/redis/RefreshTokenRepository.java`
- `auth/src/main/resources/docs/auth-redis.md`
