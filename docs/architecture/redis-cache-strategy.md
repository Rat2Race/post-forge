# Redis 캐시 전략

## 개요

Redis를 **조회수 버퍼**로 사용한다.  
좋아요는 RDB를 source of truth로 두고, Redis write-behind 구조는 사용하지 않는다.

관련 결정 기록은 [ADR-001 Use Redis for View Count](../decisions/adr-001-use-redis-for-view-count.md)에 둔다.
이 문서가 조회수 Redis key와 동기화 상태 전이의 정본이다. RDB 반영 컬럼은 `posts.views`다.

---

## 조회수

| 키 패턴 | 타입 | 예시 | 설명 |
|---------|------|------|------|
| `post:views:{postId}` | String | `post:views:5` = `"142"` | 게시글 조회수 버퍼. DB 값을 캐싱하고 increment로 증가 |
| `post:viewed:{postId}:{accountId}` | String | `post:viewed:5:42` = `"Viewed"` | 중복 조회 방지 가드키. 24시간 TTL |
| `post:views:dirty` | SET | `{ "5", "12", "30" }` | 조회수가 변경된 postId 목록. 스케줄러가 이 목록만 동기화 |

### 동작 흐름

```
[계정 42의 게시글 조회]
  1. setIfAbsent(post:viewed:5:42, "Viewed", 24h)       → 이미 있으면 종료
  2. get(post:views:5)                                  → null이면 DB에서 로드
  3. increment(post:views:5)                            → 조회수 +1
  4. sadd(post:views:dirty, "5")                        → dirty 목록에 추가

[스케줄러 5분 주기]
  1. rename(post:views:dirty → post:views:dirty:processing)   → 원자적 이동
  2. smembers(post:views:dirty:processing)                    → 변경된 ID 목록 조회
  3. 각 postId에 대해: get(post:views:{id})                    → posts.views UPDATE
  4. 성공한 ID만 processing SET에서 제거
  5. 실패한 ID는 processing SET에 남겨 다음 실행에서 재시도
```

### 설계 의도

- **가드키 TTL 24시간**: 같은 사용자가 같은 글을 반복 조회해도 24시간 내 1회만 카운트
- **setIfAbsent로 캐시 저장**: 동시 miss가 기존 값을 덮어쓰지 않는다. 동시 요청이 각각 DB를 읽을 수는 있다.
- **dirty tracking**: `KEYS` 명령은 O(N) 블로킹이라 사용하지 않음. 변경된 건만 SET으로 추적
- **rename으로 dirty 이동**: `smembers`→`delete` 사이에 `sadd`된 postId가 유실되지 않도록 dirty SET을 원자적으로 rename한 뒤 읽는다. 처리 중 추가분은 새 dirty 키에 쌓인다.

---

## 좋아요

좋아요는 Redis에 상태를 저장하지 않는다.

- **원본 데이터**: `post_like`, `comment_like` 테이블. `(대상, 계정)` 유니크 제약으로 한 계정의 좋아요는 하나다.
- **넣기·지우기**: `INSERT … ON CONFLICT DO NOTHING`과 한 문장 `DELETE`가 바뀐 행 수(0 또는 1)를 돌려준다. 이미 그 상태인 요청은 아무것도 바꾸지 않고 성공한다(멱등).
- **카운터**: 행을 실제로 바꾼 요청만 `like_count = like_count ± 1`을 한 문장으로 실행한다. 읽은 값을 다시 쓰지 않으므로 동시 요청이 서로의 갱신을 덮지 않는다.
- **조회 방식**: 응답·상세·목록의 좋아요 수는 좋아요 행을 센 값(COUNT)이다. `like_count` 열은 아직 읽지 않는다.
- **정합성 기준**: Redis 장애와 무관하게 좋아요 상태는 DB 기준으로 유지
- **Redis 사용처**: 짧은 cooldown과 사용자별 rate limit 같은 보호 장치에만 사용

### 설계 의도

- **예외 대신 ON CONFLICT**: 중복을 유니크 제약 예외로 받으면 PostgreSQL은 그 트랜잭션의 다음 문장을 모두 거절하고(25P02), Hibernate 세션에는 id 없는 엔티티가 남아 다음 flush가 실패한다. 예외를 잡아 삼켜도 요청은 500이 된다.
- **버린 대안 — 저장을 REQUIRES_NEW로 분리**: 바깥 트랜잭션이 커넥션을 잡은 채 안쪽이 하나 더 쓴다. 풀(기본 10)이 바깥 트랜잭션으로 차면 안쪽이 커넥션을 기다리며 멈춘다. 카운터 갱신 손실도 그대로 남는다.
- **버린 대안 — savepoint(NESTED)**: DB 트랜잭션은 되살리지만 Hibernate 세션에 남은 실패 엔티티는 지우지 못한다.
- **검증**: 실제 PostgreSQL에서 이미 있는 좋아요를 다시 넣는 요청(게시글·댓글)과 계정 20개의 동시 좋아요를 확인한다(`LikeConcurrencyTest`).

---

## 횟수 제한 키

로그인 실패와 좋아요 rate limit은 고정 창(window) 카운터를 쓴다. `RedisGuardOperations.incrementWithExpiry`가 `INCR`와 만료 설정을 Lua 스크립트 한 번으로 실행한다.

- **원자성**: 두 명령을 따로 보내면 그 사이 연결이 끊길 때 만료 없는 카운터가 남는다. 그 키는 창이 끝나도 지워지지 않아 해당 계정·IP의 요청이 영원히 막힌다.
- **만료 거는 조건**: 첫 증가가 아니라 "만료가 없을 때" 건다. 예전 구현에서 만료 없이 남은 키도 다음 증가 때 만료가 걸려 스스로 낫는다.
- **검증**: 실제 Redis(Testcontainers `redis:7-alpine`)에서 만료 설정 실패 주입, 100번 동시 증가, 만료 없이 남은 키 회복을 확인한다.
- 이메일 인증 요청 가드는 cooldown·rate·lock을 함께 판단하는 별도 Lua 스크립트를 쓴다.

---

## 키 생명주기

| 이벤트 | 생성되는 키 | 삭제되는 키 |
|--------|------------|------------|
| 게시글 조회 | `post:views:{id}`, `post:viewed:{id}:{accountId}`, dirty SET에 추가 | - |
| 좋아요 토글 | DB의 like row / likeCount 갱신 | - |
| 게시글 삭제 | - | `post:views:{id}` |
| 댓글 삭제 | - | - |
| 24시간 경과 | - | `post:viewed:{id}:{accountId}` (TTL 만료) |
| 스케줄러 실행 | `dirty:processing` (임시) | DB 반영에 성공한 ID |

---

## Redis 장애 시 영향

| 상황 | 영향 | 복구 |
|------|------|------|
| Redis 재시작 | 마지막 동기화 이후 조회수 변경분 유실 (최대 5분) | 다음 요청 시 DB 기준으로 재시작 |
| Redis 응답 없음 | 명령이 2초 안에 끊긴다. 조회수는 막지 않는다: 읽기는 DB 값(최대 5분 늦음), 증가·캐시 적재·삭제는 건너뛴다. 로그인·메일·좋아요 가드는 막는다(`429`) | Redis가 돌아오면 다음 요청부터 정상 |
| 스케줄러 실패 | dirty 데이터가 다음 주기까지 누적 | 다음 실행 시 자동 처리 |
