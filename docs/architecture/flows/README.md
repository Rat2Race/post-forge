# 모듈별 API 흐름

이 폴더는 각 모듈의 API가 요청부터 응답까지 어떤 계층과 저장소를 거치는지, 왜 그렇게 설계했는지를 정리한다.
Endpoint/DTO/status의 canonical source는 [`docs/api/`](../../api/README.md)이고, 이 문서들은 **동작 원리와 내부 흐름**을 다룬다.

Last verified against code: 2026-07-05.

## 모듈 문서

| 문서 | 다루는 흐름 |
| --- | --- |
| [auth-flows.md](./auth-flows.md) | 이메일 인증, 회원가입, 로그인/로그아웃, 토큰 재발급, OAuth2 exchange, 계정 변경 |
| [board-flows.md](./board-flows.md) | 게시글 CRUD, DB 직접 조회수, 좋아요, 댓글, 구매 판단 투표, 파일 presigned URL |
| [catalog-flows.md](./catalog-flows.md) | 상품 조회/검색, admin upsert, 상품 매칭 후보 승인/거절 |
| [price-flows.md](./price-flows.md) | 가격 이력 조회, response-only 가격 판정 |
| [ai-flows.md](./ai-flows.md) | AI 채팅(RAG), 게시글 초안 생성, 입출력 안전 가드 |
| [ingest-flows.md](./ingest-flows.md) | 문서 적재 파이프라인, 상품 수집, 뉴스 문서 수집, 출시 뉴스 자동 게시 |

## 공통 구조 원리

모든 모듈이 같은 계층 구조를 따른다.

```text
presentation (Controller, DTO)
  -> application (Service, use case, port interface)
    -> domain (Entity, policy, enum)
    -> infrastructure (JPA adapter, Redis repository, 외부 API client)
```

- **의존 방향은 안쪽으로만.** presentation은 application을, application은 domain과 port를 참조한다. infrastructure는 application의 port를 구현한다.
- **모듈 간 호출은 `core`의 port 계약으로만.** 예를 들어 `ingest`는 `board` 구현을 모른 채 `core`의 `PostWriter`로 게시글을 쓰고, `board`는 `auth`를 의존하지 않고 `core`의 principal 계약만 참조한다. 근거: [Module Dependency Policy](../module-dependencies.md)
- **인증 파이프라인은 공통.** 요청이 컨트롤러에 닿기 전에 JWT filter가 `Authorization: Bearer` 토큰을 검증해 `accountId` 기반 principal을 만들고, route 권한은 `app`의 `PostForgeAuthorizationRules`가 조립한다. 권한/소유권 판단은 항상 `accountId` 기준이다. 근거: [Authentication Architecture](../authentication.md)
- **트랜잭션 경계는 application service.** 조회 서비스는 `@Transactional(readOnly = true)`, 쓰기 use case는 메서드 단위 `@Transactional`을 쓴다. 외부 I/O(S3 등)는 가능한 트랜잭션 밖에 둔다.
- **Redis는 인증 기능 상태만.** refresh token, email verification token/state, OAuth2 exchange code처럼 MVP 기능에 필요한 짧은 수명 상태만 Redis에 둔다. 성능 캐시와 요청 보호 guard는 MVP 범위에서 제외한다.
- **공개 read path는 AI/외부 API를 호출하지 않는다.** AI와 외부 쇼핑/뉴스 API는 명시적 사용자 요청 또는 admin/system 경로에서만 호출한다. 근거: [AI Cost Policy](../../policy/ai-cost-policy.md)
