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

- **원본 데이터**: `post_like`, `comment_like` 테이블
- **카운트 동기화**: 토글 시점에 DB에서 즉시 반영
- **조회 방식**: 목록 조회는 DB 집계 쿼리와 사용자별 좋아요 조회 쿼리로 처리
- **정합성 기준**: Redis 장애와 무관하게 좋아요 상태는 DB 기준으로 유지
- **Redis 사용처**: 짧은 cooldown과 사용자별 rate limit 같은 보호 장치에만 사용

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
| 스케줄러 실패 | dirty 데이터가 다음 주기까지 누적 | 다음 실행 시 자동 처리 |
