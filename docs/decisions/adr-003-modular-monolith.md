# ADR-003 모듈러 모놀리스

상태: **Accepted**

PostForge는 하나의 Spring Boot 애플리케이션으로 배포하되, 내부는 기능 경계별 Gradle module로 나눈다.
목표는 초기 운영 복잡도를 낮게 유지하면서 서비스 분리가 필요해질 때 경계를 식별하기 쉽게 만드는 것이다.

## 결정

현재 선택은 "MSA를 바로 도입하지 않고, MSA 후보 경계를 코드 안에서 먼저 검증하는 modular monolith"이다.

- 하나의 process로 실행해 배포, transaction, local debugging, 운영 관측 비용을 낮춘다.
- 허용 모듈 그래프와 책임은 [Module Dependency Policy](../architecture/module-dependencies.md)를 따른다.
- 모듈 간 호출은 허용된 application API 또는 역방향 구현 결합을 끊는 port 계약을 사용한다.

## MSA를 먼저 도입하지 않는 이유

MSA는 서비스별 독립 배포와 장애 격리에 유리하지만, 처음부터 도입하면 다음 비용이 즉시 생긴다.

- 서비스 간 네트워크 실패, timeout, retry, circuit breaker 설계가 필요하다.
- 분산 transaction을 피하기 위한 event/outbox/idempotency 설계가 필요하다.
- 서비스별 observability, 배포 파이프라인, secret, ingress, 인증 정책을 따로 운영해야 한다.
- 아직 검증되지 않은 도메인 경계를 API로 고정하면 이후 변경 비용이 커진다.

그래서 현재는 process는 하나로 유지하되, 서비스 후보 경계를 module과 test로 먼저 검증한다.

## 영향

- 기능 간 호출은 현재 in-process이므로 네트워크 실패 처리가 필요 없다.
- 같은 DB transaction을 사용할 수 있어 초기 구현과 운영이 단순하다.
- 서비스 분리 시 Java 호출을 HTTP 또는 message contract로 바꾸고, 저장소와 운영 정책을 별도로 설계해야 한다.

## 후속 변경

- `messaging` 모듈과 outbox 경계는 [ADR-004](./adr-004-scope-reduction.md)로 삭제됐다. 소비자가 존재한 적이 없어 유지 비용만 남아 있었다. 서비스 분리 시점의 event/outbox 설계는 그때 다시 한다. 현재 모듈 그래프는 [Module Dependency Policy](../architecture/module-dependencies.md)를 따른다.
- monolith 운영 DB migration은 Flyway로 결정됐다. 현재 적용 경로는 [DB Schema Ownership](../database/schema-ownership.md#마이그레이션-규칙)을 따른다.
- 미해결 항목은 migration 도구 도입이 아니라 **서비스별 DB로 분리할 때의 migration·호환성 전략**이다.
