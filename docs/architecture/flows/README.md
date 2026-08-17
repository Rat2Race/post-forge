# 모듈별 API 흐름

이 폴더는 각 모듈의 API가 요청부터 응답까지 어떤 계층과 저장소를 거치는지, 왜 그렇게 설계했는지를 정리한다.
Endpoint/DTO/status의 canonical source는 [`docs/api/`](../../api/README.md)이고, 이 문서들은 **동작 원리와 내부 흐름**을 다룬다.

## 모듈 문서

| 문서 | 다루는 흐름 |
| --- | --- |
| [auth-flows.md](./auth-flows.md) | 이메일 인증, 회원가입, 로그인/로그아웃, 토큰 재발급, OAuth2 exchange, 계정 변경 |
| [board-flows.md](./board-flows.md) | 게시글 CRUD, 조회수 버퍼, 좋아요, 댓글, 구매 판단 투표, 파일 presigned URL |
| [catalog-flows.md](./catalog-flows.md) | 상품 조회/검색, admin upsert, 상품 매칭 후보 승인/거절 |
| [price-flows.md](./price-flows.md) | 가격 이력 조회, response-only 가격 판정 |
| [ai-flows.md](./ai-flows.md) | AI 채팅(RAG), 내부 출시뉴스 초안 생성, 입출력 안전 가드 |
| [ingest-flows.md](./ingest-flows.md) | 문서 적재 파이프라인, 상품 수집, 뉴스 문서 수집, 출시 뉴스 자동 게시 |
| [messaging-flows.md](./messaging-flows.md) | outbox 저장, relay claim/dispatch |

## 공통 구조 원리

기능 모듈의 요청 흐름은 대체로 다음 계층을 따른다.

```text
presentation (Controller, DTO)
  -> application (Service, use case, port interface)
    -> domain (Entity, policy, enum)
    -> infrastructure (JPA adapter, Redis repository, 외부 API client)
```

- **의존 방향은 안쪽으로만.** presentation은 application을, application은 domain과 port를 참조한다. infrastructure는 application의 port를 구현한다.
- **모듈 간 호출은 허용 의존성 안에서만.** 기능 모듈의 application API를 직접 호출할 수 있고, 역방향 구현 결합을 피해야 하는 경계는 `core` port를 사용한다. 허용 그래프와 책임의 정본은 [Module Dependency Policy](../module-dependencies.md)다.
- **인증 경계는 공통.** 권한과 소유권은 `accountId` 기준으로 판단하며, filter·route·오류·cookie 경계는 [Authentication Architecture](../authentication.md)를 따른다.
- **트랜잭션 경계는 application service.** 조회 서비스는 `@Transactional(readOnly = true)`, 쓰기 use case는 메서드 단위 `@Transactional`을 쓴다. 외부 I/O(브로커 publish, S3)는 가능한 트랜잭션 밖에 둔다. outbox relay가 대표 사례다.
- **Redis는 보조 상태만.** 토큰/인증 보호/좋아요 보호/조회수처럼 재계산 또는 재로그인으로 복구 가능한 상태만 Redis에 둔다. 장애 정책은 사용처별로 fail-closed 또는 degraded로 나뉜다. 근거: [Redis 연결 장애](../../troubleshooting/redis-connection-failure.md)
- **공개 read path는 AI/외부 API를 호출하지 않는다.** AI와 외부 쇼핑/뉴스 API는 명시적 사용자 요청 또는 admin/system 경로에서만 호출한다. 근거: [AI Cost Policy](../../policy/ai-cost-policy.md)
