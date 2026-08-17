# 이벤트 기반 아웃박스

이 문서는 도메인 변경과 이벤트 발행 의도를 같은 DB 트랜잭션에 남기기로 한 이유와 전달 보장을 설명한다.
현재 relay 순서, 설정, publisher 상태는 [Messaging 내부 흐름](./flows/messaging-flows.md)을 정본으로 둔다.

## 문제

도메인 쓰기와 broker publish를 따로 수행하면 dual write 문제가 생긴다.

```text
1. business table write commit
2. broker publish fail
```

반대로 broker publish가 먼저 성공하고 DB 트랜잭션이 rollback되면 이벤트가 실제 상태보다 앞서 나간다.
outbox는 business table 변경과 `outbox_events` insert를 같은 트랜잭션에 넣어 "커밋된 변경은 발행 의도도 남는다"는 조건을 만든다.

## 결정

- 도메인 변경과 outbox insert는 같은 트랜잭션으로 커밋한다.
- `outbox_events`는 기능 table과 FK를 맺지 않는다. aggregate 정보는 이벤트 계약으로 해석한다.
- publisher 호출은 DB 트랜잭션 밖에서 수행한다.
- relay 실패는 상태와 다음 재시도 시각을 남긴다.

물리 컬럼과 table ownership은 [DB Schema Ownership](../database/schema-ownership.md)과 [PostForge MVP ERD](../database/postforge-mvp-erd.md)를 따른다.

## 전달 보장

relay는 **at-least-once** 전달이다. publish 성공 직후 완료 상태 기록 전에 프로세스가 종료되면 같은 이벤트가 다시 발행될 수 있다.
consumer는 `event_id` 또는 domain unique key를 사용해 중복 처리에도 결과가 깨지지 않도록 해야 한다.

## 영향

- 커밋된 도메인 변경의 발행 의도가 DB에 남는다.
- 즉시 exactly-once 전달은 보장하지 않으며 consumer 멱등성이 필요하다.
- broker와 relay가 느려도 도메인 트랜잭션이 네트워크 I/O를 기다리지 않는다.
