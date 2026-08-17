# Messaging 내부 흐름

messaging은 자체 HTTP endpoint가 없다. 기능 모듈이 `core`의 이벤트 기록 계약을 호출하면 뒤에서 동작한다.
동일 트랜잭션 기록과 전달 의미는 [이벤트 기반 아웃박스](../event-driven-outbox.md)가 정본이고, 이 문서는 현재 실행 순서와 설정을 다룬다.

현재 `DomainEventRecorder` 구현은 `OutboxWriter` 하나다. 이벤트를 만드는 모듈(board, price 등)은 recorder를 호출하고, 이벤트는 도메인 변경과 같은 트랜잭션에서 outbox에 기록된다.

| 구현 | 저장 | 전달 시점 | 유실 가능성 |
| --- | --- | --- | --- |
| `OutboxWriter` | `outbox_events` row (도메인 변경과 같은 트랜잭션) | relay가 나중에 | DB 기록은 유지되지만 현재 stale `PROCESSING` 자동 복구는 없음 |

## Outbox 기록 흐름

1. 기능 모듈의 `@Transactional` 메서드가 `DomainEventRecorder.record(...)`를 호출한다. (예: `PostCommandService.deletePost`의 `PostDeletedEvent`, `PriceSnapshotService`의 `PriceSnapshotCreatedEvent`)
2. `OutboxWriter`는 `@Transactional(propagation = MANDATORY)` — 호출자의 트랜잭션이 없으면 예외를 던진다. 도메인 변경과 outbox insert가 **반드시 같은 트랜잭션으로 커밋**되게 강제하는 장치다. 이게 깨지면 dual write 문제가 되살아난다.
3. payload를 JSON으로 직렬화해 `outbox_events`에 `PENDING`으로 저장한다. 직렬화 실패는 조용히 무시하지 않고 예외로 전파한다(호출 트랜잭션 롤백).

## Relay 흐름 (OutboxRelayService.relayPending)

`OutboxRelayScheduler`가 주기 실행하며, `relay-enabled=false`(기본값)이거나 publisher가 없으면 아무것도 하지 않는다.

| 설정 | 기본값 |
| --- | ---: |
| `postforge.messaging.outbox.relay-enabled` | `false` |
| `postforge.messaging.outbox.batch-size` | `50` |
| `postforge.messaging.outbox.max-retries` | `5` |
| `postforge.messaging.outbox.retry-backoff` | `30s` |

```text
① claim (짧은 트랜잭션)
   findClaimableForUpdate(batch, maxRetries)  ← PENDING/FAILED + available_at 도래, FOR UPDATE
   markProcessing → 커밋
② dispatch (트랜잭션 밖)
   event type을 지원하는 EventPublisher들에 publish
③ 결과 기록 (각각 별도 트랜잭션)
   성공 → markPublished / 실패 → OutboxRetryService.scheduleRetry
        (retry_count 증가, 다음 available_at = backoff, last_error 기록)
```

트랜잭션을 3개로 나누는 이유: publish는 네트워크 I/O라서 트랜잭션 안에 두면 브로커가 느릴 때 DB 커넥션과 row lock을 그 시간만큼 점유한다. claim을 `FOR UPDATE`로 하는 이유: 스케줄 실행이 겹치거나 인스턴스가 늘어도 같은 이벤트를 동시에 claim하지 못하게 한다.

## 실패 관찰

재발행 가능성과 consumer 멱등성 규칙은 [이벤트 기반 아웃박스](../event-driven-outbox.md#전달-보장)를 따른다.
재시도 한도를 넘긴 이벤트는 claim 대상에서 빠져 `FAILED`로 남고, `retry_count`와 `last_error`로 관찰한다.
현재 claim 이후 프로세스가 종료되어 `PROCESSING`에 머문 이벤트를 자동 복구하는 lease는 구현되어 있지 않다.

## 현재 상태

- 실제 MQ broker adapter는 없다. 현재 publisher는 `LoggingEventPublisher`(로그 출력)이고, relay는 기본 비활성이다. outbox는 broker를 붙일 때를 위한 신뢰 전달 기반이며, DB의 `outbox_events`가 publish 전 source of truth다.
- relay가 비활성인 동안 이벤트는 `PENDING`으로만 쌓인다. relay와 publisher를 켜면 저장된 이벤트를 순서대로 전달한다.
