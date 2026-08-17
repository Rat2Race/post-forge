# 트레이드오프와 한계

> 이 문서는 약점 고백이 아니라 설계 판단의 일부다. 무엇을 알면서 남겨뒀고, 왜 지금은 그래도 되는지, 확장하면 어떻게 푸는지를 함께 적는다.
> 면접에서 "이 시스템의 한계는?"에 바로 답하기 위한 근거이기도 하다.

각 항목은 **한계 / 지금 괜찮은 이유 / 확장 시 해법** 순서로 읽는다.

---

## 1. Refresh 토큰 검증과 교체가 원자적이지 않다

- **한계** — refresh 재발급은 Redis 저장값과 요청 토큰을 상수 시간 비교로 검증한 뒤 새 토큰으로 교체한다. 이 `validate + replace`가 단일 원자 연산이 아니라서, 같은 refresh 토큰으로 동시 재발급이 들어오면 둘 다 검증을 통과하고 마지막 저장분만 살아남을 수 있다.
- **지금 괜찮은 이유** — 정상 클라이언트는 재발급을 순차로 하므로 창이 매우 좁고, rotation 자체는 탈취 토큰 재사용을 여전히 차단한다.
- **확장 시** — Redis Lua script나 compare-and-set 저장 계약으로 `validate + replace`를 원자화한다. 클라이언트는 중복 재발급 중 하나가 `INVALID_TOKEN`으로 실패하는 흐름을 정상으로 처리한다. → [ADR-002](../decisions/adr-002-refresh-token-rotation.md)

## 2. 아웃박스 relay가 기본 비활성이다

- **한계** — 도메인 변경과 발행 의도를 한 트랜잭션에 남기는 구조까지가 실동작이고, 실제 relay(`relay-enabled=false`)와 외부 MQ 연동은 켜서 붙이는 확장 지점이다. 아직 "메시지 큐를 운영"하지는 않는다.
- **지금 괜찮은 이유** — 현재 제품 흐름은 동기 처리로 충분하고, dual-write를 막는 핵심(커밋과 발행 의도의 원자성)은 이미 확보돼 있다.
- **확장 시** — `relay-enabled=true`로 켜고 MQ adapter를 붙인다. relay는 at-least-once이므로 consumer를 `event_id` 기준으로 멱등하게 만든다. → [이벤트 아웃박스](../architecture/event-driven-outbox.md)

## 3. Redis 실패 정책이 용도별로 다르다 (의도된 판단)

- **한계로 보이는 것** — 조회수는 Redis 재시작 시 마지막 동기화 이후 변경분을 최대 5분까지 잃을 수 있다.
- **지금 괜찮은 이유** — 이건 실수가 아니라 용도별 정책이다. 조회수는 정확도보다 가용성이 중요한 파생 데이터라 **degraded**(잃어도 다음 요청에 DB 기준으로 복구)로 둔다. 반대로 인증(refresh / OAuth2 / 이메일 인증)의 Redis는 **fail-closed**로 두어, 상태를 잃느니 요청을 실패시킨다.
- **확장 시** — 조회수 동기화 주기를 줄이거나 write-ahead 로그를 두어 유실 창을 좁힐 수 있다. 단, 조회수에 그만한 비용을 쓸 가치가 있는지부터 판단한다. → [Redis 캐시 전략](../architecture/redis-cache-strategy.md) · [Redis 장애 대응](../troubleshooting/redis-connection-failure.md)

## 4. 성능 수치는 historical baseline이다

- **한계** — 문서의 20~25 RPS, p95 개선치는 모두 Oracle Cloud ARM 1vCPU / 1GB에서 측정한 과거 값이다. 현재 운영(Intel N100)에서 동일 시나리오로 잡은 새 capacity baseline은 아직 없다.
- **지금 괜찮은 이유** — 애초에 capacity를 과장하지 않으려고 historical / current를 명시적으로 구분해 기록했다. 병목 분석의 방법론과 상대 개선치는 환경이 바뀌어도 유효하다.
- **확장 시** — N100에서 같은 k6 시나리오를 돌리고, 같은 시간 구간의 CPU / memory / GC / DB / Redis 지표를 함께 수집해 새 baseline을 남긴다. → [성능 리포트 인덱스](../performance/README.md)

## 5. 목록 조회는 조회수 캐시로 오히려 느려졌다

- **한계** — 조회수 Redis 버퍼는 상세 조회를 크게 개선했지만 목록 조회 p95는 약 60% 악화됐다.
- **지금 괜찮은 이유** — A/B로 회귀를 정량 확인했고, 목록 최적화를 조회수 버퍼링과 분리해야 한다는 결론을 근거와 함께 남겼다.
- **확장 시** — 목록은 전용 projection DTO / 커버링 인덱스 / fetch 전략으로 따로 최적화한다. 조회수 버퍼는 상세에만 이롭다는 전제를 유지한다. → [Redis 캐시 벤치마크](../performance/redis-cache-benchmark.md)

## 6. 가격 판정 외부 소스가 현재 비활성이다

- **한계** — Naver Shopping 검색 API 종료 대응으로 가격 판정에 사용할 실시간 외부 샘플이 없다.
- **지금 괜찮은 이유** — 가짜 데이터나 종료된 API를 호출하지 않고 `503`으로 가용성 부족을 명시한다. 판정 결과도 저장하거나 게시글로 만들지 않는다.
- **확장 시** — 대체 상품 소스를 `SourceRequestExecutor`에 연결하고 기존 판정 계산을 재사용한다. → [통합 API 명세의 Price](../api/README.md#price) · [Use Case Data Policy](../policy/usecase-data-policy.md)

## 7. 게시글·댓글은 아직 물리 삭제다

- **한계** — 현재 구현은 게시글/댓글을 물리 삭제(hard delete)한다. 복구나 감사 이력이 남지 않는다.
- **지금 괜찮은 이유** — MVP 범위에서 삭제 정책의 기준은 "일반 사용자에게 더 이상 노출하지 않는 것"이고, 계정은 이미 상태 기반 soft delete를 target으로 잡아뒀다.
- **확장 시** — 게시글/댓글도 상태 기반 soft delete로 전환하고, 물리 삭제는 별도 보존 기간과 정리 작업으로 옮긴다. → [삭제 정책](../policy/delete-policy.md)
