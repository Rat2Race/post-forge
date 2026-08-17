# 엔지니어링 하이라이트

> PostForge에서 기술적으로 설명할 가치가 있는 세 가지를 골라 문제 → 접근 → 트레이드오프 → 근거 순서로 정리한다.
> 각 항목의 상세 수치와 코드는 링크한 기존 문서에 있다.

## 요약

| # | 주제 | 한 줄 |
| --- | --- | --- |
| H1 | 성능 병목의 체계적 분리 | 추측 대신 CPU / SQL count / p95로 병목을 분리해 `GET /posts` p95를 1.89s → 104ms로 개선. 캐시가 오히려 악화된 케이스도 근거와 함께 기록 |
| H2 | 트랜잭셔널 아웃박스 | 도메인 쓰기와 이벤트 발행 의도를 같은 트랜잭션에 남겨 dual-write 문제를 제거. `MANDATORY` 전파로 원자성을 강제 |
| H3 | 모듈 경계와 포트 계약 | 허용된 application API와 `core` 포트로 결합 방향을 제한하고 ArchUnit으로 검증 |

---

## H1. 성능 병목의 체계적 분리

### 문제

초기 부하 테스트에서 `GET /posts`가 느렸다. 하지만 "느리다"는 관찰만으로는 원인을 알 수 없었다.
로그인과 읽기를 같은 비중으로 섞은 시나리오는 1vCPU 서버를 BCrypt 해싱으로 포화시켜, 읽기 API의 실제 병목을 가리고 있었다.

### 접근 — 한 번에 한 변수만 분리

1. **CPU 포화부터 제거** — 로그인/읽기를 한 시나리오에 섞지 않고 읽기 25 VU + 로그인 5 VU로 분리.
2. **SQL count 확인** — CPU가 여유로운데도 목록만 느리면 쿼리 수를 본다. Hibernate SQL 로그에서 요청 1건당 추가 쿼리 11개(N+1) 확인.
3. **fetch 전략 교정** — 연관 로딩 전략과 batch size 조정.
4. **캐시 실험은 분리해서 판단** — 조회수 Redis 버퍼를 상세/목록에 각각 A/B.

### 결과

- Oracle Cloud ARM 1vCPU / 1GB historical baseline에서 `GET /posts` p95를 1.89s에서 104.47ms까지 낮췄다. 단계별 조건과 수치는 [N+1 분석](../performance/n-plus-one-analysis.md)에 있다.
- 조회수 Redis 버퍼는 상세 p95를 376.16ms에서 63.68ms로 낮췄지만 목록 p95는 512.94ms에서 820.93ms로 악화됐다. 전체 A/B 조건은 [Redis 캐시 벤치마크](../performance/redis-cache-benchmark.md)에 있다.

### 무엇을 배웠나 (트레이드오프)

- **같은 최적화가 API마다 반대로 작동한다.** 조회수 버퍼는 조회수 update가 잦은 상세 조회에는 크게 이로웠지만, 목록 조회는 파생 데이터 읽기·리소스 경쟁으로 오히려 악화됐다. 목록 최적화는 조회수 버퍼링 결정과 **분리해서** 다뤄야 한다.
- **수치는 방법이지 자랑이 아니다.** 위 숫자는 1vCPU / 1GB의 historical baseline이며 현재 운영(Intel N100) capacity 주장이 아니다. 값의 가치는 "20 RPS를 낸다"가 아니라 "병목을 재현 가능하게 분리하고 개선을 근거로 남겼다"는 절차에 있다.

### 근거

- [N+1 분석](../performance/n-plus-one-analysis.md) · [Redis 캐시 벤치마크](../performance/redis-cache-benchmark.md) · [k6 시나리오 결과](../performance/k6-scenario-results.md)
- 측정 범위와 환경 한계: [성능 리포트 인덱스](../performance/README.md)

---

## H2. 트랜잭셔널 아웃박스

### 문제

도메인 상태 변경과 이벤트 발행을 따로 하면 둘 중 하나만 성공하는 dual-write 문제가 생긴다.
DB는 커밋됐는데 broker publish가 실패하거나, publish는 나갔는데 트랜잭션이 rollback되면 이벤트가 실제 상태보다 앞서 나간다.

### 설계

`messaging` 모듈이 도메인 쓰기와 `outbox_events` insert를 **같은 트랜잭션 경계**에 넣어 "커밋된 변경은 발행 의도도 함께 남는다"는 불변식을 만든다.

- 도메인 트랜잭션 안에서 `core`의 `DomainEventRecorder`를 호출한다.
- 구현체 `OutboxWriter`는 `MANDATORY` 전파로 호출자의 트랜잭션 참여를 강제한다.
- relay 상태 전이와 재시도 절차는 [Messaging Flow](../architecture/flows/messaging-flows.md)에 한 번만 기록한다.

### 트레이드오프 (정직하게)

- **at-least-once** — relay는 같은 이벤트를 두 번 보낼 수 있으므로 consumer는 `event_id`로 멱등해야 한다. 버그가 아니라 계약이다.
- **relay는 기본 비활성(`relay-enabled=false`)이고 외부 MQ는 확장 지점이다.** 이 브랜치 기준 실동작은 "도메인 변경과 발행 의도를 한 트랜잭션에 원자적으로 남기는 것"까지다. "메시지 큐를 운영한다"가 아니라 "dual-write를 구조적으로 막았다"가 정확한 표현이다.
- `outbox_events`는 기능 테이블과 FK를 맺지 않는다. aggregate 참조는 DB 무결성이 아니라 이벤트 계약으로 해석한다.

### 근거

- [이벤트 기반 아웃박스](../architecture/event-driven-outbox.md) · [Messaging Flow](../architecture/flows/messaging-flows.md)
- 구현과 검증: `DomainEventRecorder`, `OutboxWriter`, `OutboxRelayService` 및 대응 테스트

---

## H3. 모듈 경계와 포트 계약

### 문제

기능이 늘면 모듈이 서로의 구현을 직접 부르기 쉽다. 그러면 모듈러 모놀리스라도 결합이 얽혀 나중에 분리가 불가능해진다.

### 설계

두 종류의 공유 모듈을 성격으로 나눴다.

- `core` = 모듈 간 **compile-time 계약**(port, 공통 DTO/예외, principal). shared kernel.
- `support` = Spring 실행에 필요한 **공통 인프라**(Redis guard, JPA auditing, 공통 MVC 예외 응답).

그 위에 경계 규칙을 지켰다.

- `ingest`는 허용 그래프 안에서 `source`, `catalog`, `price`의 공개 application API를 사용한다.
- `board`는 인증 구현 모듈 `auth`를 의존하지 않는다. 컨트롤러는 `core`의 공통 principal 계약만 참조한다.
- `ingest`는 AI 구현 모듈 `ai`를 의존하지 않는다. 문서 적재는 `VectorStore` API로, launch-news 초안은 `core`의 draft 생성 포트로 호출한다.
- `ingest`의 launch-news 발행이 `board` 구현에 직접 붙지 않도록 `PostWriter`를 `core` 포트로 두고, 구현체는 `board.post.application.BoardPostWriter`에 둔다.

### 검증

- 허용 모듈 그래프는 `ModuleBoundaryTest`로 검사하고, 외부 라이브러리 전이 의존성은 `dependencyInsight`로 확인한다.

### 근거

- [모듈 의존성 정책](../architecture/module-dependencies.md) · [Gradle 의존성 근거](../architecture/gradle-dependency-rationale.md) · [ADR-003 모듈러 모놀리스](../decisions/adr-003-modular-monolith.md)
