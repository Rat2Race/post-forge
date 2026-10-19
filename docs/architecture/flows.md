# 요청 흐름

PostForge는 게시판 서비스다. 뉴스 수집·자동 게시·데일리 종합은 [ADR-008](../decisions/adr-008-switch-to-learning-platform.md)에 따라 지웠고, 학습 기능은 [ADR-009](../decisions/adr-009-split-study-from-board.md)에 따라 이 브랜치에서 뗐다.

Endpoint, DTO, status의 정본은 [API 문서](../api/README.md)다. 이 문서는 모듈 경계, 실패 처리, 일관성 보장만 설명한다.

## 공통 구조 원리

```text
presentation -> application -> domain
                         -> port <- infrastructure
```

- application service가 트랜잭션 경계를 소유하고, LLM 호출은 긴 DB 트랜잭션 밖에서 실행한다.
- OSIV(`spring.jpa.open-in-view`)는 끈다. 영속성 컨텍스트와 DB 커넥션은 서비스 트랜잭션이 끝나면 닫힌다. 응답 DTO는 트랜잭션 안에서 지연 컬렉션까지 복사해 만든다. 읽기 API 전체를 `ReadEndpointSmokeTest`가 OSIV 없이 호출한다.
- 모듈 간 구현 결합이 필요한 경계는 `core` port로 분리한다. 허용 의존성은 [Module Dependency Policy](./module-dependencies.md)를 따른다.
- 인증과 소유권은 `accountId`를 기준으로 판단한다. 세부 경계는 [Authentication Architecture](./authentication.md)를 따른다.
- Redis에는 인증 보호, token, 좋아요 보호, 조회수처럼 복구 가능한 보조 상태만 둔다.
- 공개 조회는 LLM을 호출하지 않는다. 비용이 드는 호출은 사용자 요청 또는 admin/system 흐름에서만 수행한다([AI Cost Policy](../policy.md#ai-cost)).

## AI

`ai` 모듈은 LLM 호출 경로(`TextGenerationClient`)만 둔다. provider와 model은 실행 설정으로 선택하고, adapter는 장애 시 null을 반환하고 metric을 남긴다. 지금은 이 경로를 부르는 기능이 없다.

## Auth

인증은 Spring Security와 JWT를 사용하며 Redis가 이메일 인증, 로그인 시도 제한, refresh token, OAuth2 일회성 교환 코드를 보관한다. endpoint별 요청 형식은 [API 문서](../api/README.md), token/cookie와 오류 경계는 [Authentication Architecture](./authentication.md)를 따른다.

- 이메일 발송 제한은 Redis Lua script로 cooldown·rate·lock을 원자적으로 평가하며 장애 시 fail-closed다. 인증 token과 OAuth2 교환 code는 `getAndDelete`로 한 번만 소비한다.
- 로그인은 BCrypt 비교 전에 저비용 Redis guard를 통과해야 한다. 성공하면 실패 기록을 지우고 access/refresh token을 발급한다.
- 재발급은 Redis 저장 refresh token과 요청 token을 상수 시간 비교한 뒤 새 refresh token으로 rotation한다. 동시성 한계는 [ADR-002](../decisions/adr-002-refresh-token-rotation.md)에 기록한다.
- 비밀번호 변경과 로그아웃은 저장된 refresh token을 폐기한다. 이미 발급된 stateless access token은 만료까지 유효하다.

Redis key 소유권은 [DB Schema Ownership](../database/schema-ownership.md#non-relational-storage)을 따른다.

## Board

`board`는 게시글·댓글·좋아요·조회수의 저장·조회 경계를 소유한다.

- 목록 조회는 검색어(`keyword`, 제목·본문)로 거른 뒤 페이징한다. 좋아요 여부·좋아요 수·조회수·댓글 수는 post ID 단위로 batch 조회한다. 근거는 [N+1 분석](../performance/results.md#n1-분석)에 있다.
- 상세 조회수는 인증 사용자에 한해 Redis에서 계정별 중복 증가를 막고 DB로 동기화한다. Redis가 응답하지 않으면 조회수는 DB 값으로 보여 주고 증가는 건너뛰어 게시글 조회를 막지 않는다. 정확성·손실 경계는 [Redis 캐시 전략](./redis-cache-strategy.md)과 [ADR-001](../decisions/adr-001-use-redis-for-view-count.md)을 따른다.
- 게시글 수정·삭제는 owner 또는 ADMIN 권한을 검사한다. 삭제는 조회수 캐시를 정리한 뒤 게시글을 물리 삭제한다.
- 좋아요는 Redis cooldown/rate guard를 거치고 DB unique constraint로 동일 관계의 중복 row 생성을 막는다. DB가 source of truth이며 Redis 장애 시 쓰기를 fail-closed한다.

댓글, 게시글 endpoint의 세부 단계와 응답 상태는 [API 문서](../api/README.md)를 따른다.
