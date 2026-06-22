# 이벤트 기반 아웃박스

PostForge의 `messaging` 모듈은 도메인 변경과 이벤트 발행 의도를 같은 DB 트랜잭션 경계 안에 남기기 위한 outbox 기반을 제공한다.
현재 relay는 기본 비활성화이며, in-process event dispatch나 후속 MQ adapter를 붙일 수 있는 확장 지점으로 둔다.

## 문제

도메인 쓰기와 broker publish를 따로 수행하면 dual write 문제가 생긴다.

```text
1. business table write commit
2. broker publish fail
```

반대로 broker publish가 먼저 성공하고 DB 트랜잭션이 rollback되면 이벤트가 실제 상태보다 앞서 나간다.
outbox는 business table 변경과 `outbox_events` insert를 같은 트랜잭션에 넣어 "커밋된 변경은 발행 의도도 남는다"는 조건을 만든다.

## 아웃박스 테이블

| 컬럼 | 목적 |
| --- | --- |
| `event_id` | 이벤트 고유 id. consumer 멱등성 키로도 사용 가능 |
| `event_type` | `product.collected` 같은 이벤트 계약 이름 |
| `aggregate_type`, `aggregate_id` | 도메인 대상 논리 참조 |
| `payload` | JSON 이벤트 본문 |
| `status` | `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED` |
| `retry_count`, `available_at`, `last_error` | relay 재시도와 장애 추적 |
| `occurred_at`, `published_at`, `created_at`, `updated_at` | 운영 추적 timestamp |

`outbox_events`는 기능 table과 FK를 맺지 않는다. aggregate 정보는 DB 참조 무결성이 아니라 이벤트 계약으로 해석한다.

## 릴레이 정책

기본 설정은 보수적이다.

| 설정 | 기본값 |
| --- | ---: |
| `postforge.messaging.outbox.relay-enabled` | `false` |
| `postforge.messaging.outbox.batch-size` | `50` |
| `postforge.messaging.outbox.max-retries` | `5` |
| `postforge.messaging.outbox.retry-backoff` | `30s` |

relay가 켜지면 claim 가능한 `PENDING`/`FAILED` 이벤트를 `PROCESSING`으로 가져오고, publisher 성공 시 `PUBLISHED`, 실패 시 `FAILED`와 다음 `available_at`을 기록한다.

## 멱등성 규칙

outbox relay는 적어도 한 번 전달될 수 있다.
consumer는 같은 `event_id`를 두 번 받아도 결과가 깨지지 않아야 한다.
검색 색인 갱신, 알림 생성, 상품 수집 후속 처리처럼 중복 실행 위험이 있는 consumer는 event id 또는 domain unique key로 중복 방지를 둔다.

## 구현 근거

- `messaging/src/main/java/dev/iamrat/messaging/outbox/domain/OutboxMessage.java`
- `messaging/src/main/java/dev/iamrat/messaging/outbox/application/OutboxWriter.java`
- `messaging/src/main/java/dev/iamrat/messaging/outbox/application/OutboxRelayService.java`
- `messaging/src/main/java/dev/iamrat/messaging/outbox/infrastructure/config/OutboxProperties.java`
- `docs/policy/usecase-data-policy.md`
