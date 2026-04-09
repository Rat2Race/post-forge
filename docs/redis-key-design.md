# Redis Key Design

## 개요

Redis를 **조회수 버퍼**로 사용한다.  
좋아요는 RDB를 source of truth로 두고, Redis write-behind 구조는 사용하지 않는다.

---

## 조회수 (View Count)

| 키 패턴 | 타입 | 예시 | 설명 |
|---------|------|------|------|
| `post:views:{postId}` | String | `post:views:5` = `"142"` | 게시글 조회수 버퍼. DB 값을 캐싱하고 increment로 증가 |
| `post:viewed:{postId}:{userId}` | String | `post:viewed:5:userA` = `"Viewed"` | 중복 조회 방지 가드키. 24시간 TTL |
| `post:views:dirty` | SET | `{ "5", "12", "30" }` | 조회수가 변경된 postId 목록. 스케줄러가 이 목록만 동기화 |

### 동작 흐름

```
[사용자 게시글 조회]
  1. setIfAbsent(post:viewed:5:userA, "Viewed", 24h)    → 이미 있으면 종료
  2. get(post:views:5)                                  → null이면 DB에서 로드
  3. increment(post:views:5)                            → 조회수 +1
  4. sadd(post:views:dirty, "5")                        → dirty 목록에 추가

[스케줄러 5분 주기]
  1. rename(post:views:dirty → post:views:dirty:processing)   → 원자적 이동
  2. smembers(post:views:dirty:processing)                    → 변경된 ID 목록 조회
  3. 각 postId에 대해: get(post:views:{id})                    → DB UPDATE
  4. delete(post:views:dirty:processing)
```

### 설계 의도

- **가드키 TTL 24시간**: 같은 사용자가 같은 글을 반복 조회해도 24시간 내 1회만 카운트
- **setIfAbsent로 캐시 로드**: 동시 요청 시 DB 조회 1번만 발생 (나머지는 캐시 히트)
- **dirty tracking**: `KEYS` 명령은 O(N) 블로킹이라 사용하지 않음. 변경된 건만 SET으로 추적

---

## 좋아요 (Like)

좋아요는 Redis에 상태를 저장하지 않는다.

- **원본 데이터**: `post_like`, `comment_like` 테이블
- **카운트 동기화**: 토글 시점에 DB에서 즉시 반영
- **조회 방식**: 목록 조회는 DB 집계 쿼리와 사용자별 좋아요 조회 쿼리로 처리
- **정합성 기준**: Redis 장애와 무관하게 좋아요 상태는 DB 기준으로 유지
- **Redis 사용처**: 짧은 cooldown과 사용자별 rate limit 같은 보호 장치에만 사용

---

## dirty tracking의 rename 패턴

```
일반 방식 (레이스 컨디션 발생):
  smembers(dirty)  →  [이 사이에 새 dirty 추가]  →  delete(dirty)  →  유실

rename 방식 (안전):
  rename(dirty → dirty:processing)   ← 원자적. 이후 추가분은 새 dirty 키에 쌓임
  smembers(dirty:processing)         ← 안전하게 읽기
  delete(dirty:processing)           ← 처리 완료
```

---

## 키 생명주기

| 이벤트 | 생성되는 키 | 삭제되는 키 |
|--------|------------|------------|
| 게시글 조회 | `post:views:{id}`, `post:viewed:{id}:{userId}`, dirty SET에 추가 | - |
| 좋아요 토글 | DB의 like row / likeCount 갱신 | - |
| 게시글 삭제 | - | `post:views:{id}` |
| 댓글 삭제 | - | - |
| 24시간 경과 | - | `post:viewed:{id}:{userId}` (TTL 만료) |
| 스케줄러 실행 | `dirty:processing` (임시) | `dirty:processing` |

---

## Redis 장애 시 영향

| 상황 | 영향 | 복구 |
|------|------|------|
| Redis 재시작 | 마지막 동기화 이후 조회수 변경분 유실 (최대 5분) | 다음 요청 시 DB 기준으로 재시작 |
| 스케줄러 실패 | dirty 데이터가 다음 주기까지 누적 | 다음 실행 시 자동 처리 |
