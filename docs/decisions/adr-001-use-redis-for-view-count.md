# ADR-001 조회수에 Redis 사용

## 배경

게시글 상세 조회는 자주 호출되고, 매 요청마다 RDB `posts.views`를 즉시 update하면 write amplification과 row contention이 생긴다.
또 같은 사용자의 반복 조회는 24시간 안에 한 번만 카운트해야 한다.

## 결정

조회수는 Redis를 write buffer로 사용하고 account 단위 24시간 중복 조회를 막는다.
변경된 post만 scheduler가 RDB로 반영한다. 정확한 key와 상태 전이는 [Redis 캐시 전략](../architecture/redis-cache-strategy.md)에 둔다.

좋아요는 Redis write-behind로 처리하지 않는다.
좋아요의 source of truth는 `post_like`, `comment_like` table과 DB count이며, Redis는 짧은 cooldown/rate limit 같은 보호 장치에만 쓴다.

## 영향

- 읽기 요청에서 DB update 빈도를 줄이고 Redis `increment`로 빠르게 응답할 수 있다.
- `KEYS` scan 없이 dirty set만 처리하므로 Redis keyspace 규모가 커져도 sync 범위가 제한된다.
- Redis 재시작 시 마지막 sync 이후 증가분은 유실될 수 있다. 허용 손실 범위는 scheduler 주기에 묶인다.
- 조회수는 eventual consistency로 다루고, 결제/권한/좋아요 같은 강한 정합성 데이터에는 이 패턴을 쓰지 않는다.

## 검토했지만 선택하지 않은 대안

| 대안 | 선택하지 않은 이유 |
| --- | --- |
| RDB 즉시 update | 조회 트래픽이 증가할수록 write contention이 커진다. |
| Redis `KEYS post:views:*` scan | 운영 Redis에서 O(N) blocking 위험이 있다. |
| 좋아요까지 Redis write-behind | 사용자별 좋아요 상태는 정합성 요구가 높아 DB source of truth가 더 안전하다. |

## 관련 문서

- [Redis 캐시 전략](../architecture/redis-cache-strategy.md)
