# 요청 흐름

PostForge는 뉴스를 수집하고, 분야별 정책과 LLM 가공을 거쳐 게시글 초안을 만든 뒤 자동 게시한다. 매일 전날 게시된 뉴스를 종합한 데일리 포스트도 자동 게시한다. 메일 구독은 향후 계획이며 현재 요청 흐름에는 포함되지 않는다.

Endpoint, DTO, status와 스케줄 설정의 정본은 [API 문서](../api/README.md)와 [자동 게시 스케줄](../api/README.md#자동-게시-스케줄)이다. 이 문서는 모듈 경계, 실패 처리, 일관성 보장만 설명한다.

## 공통 구조 원리

```text
presentation -> application -> domain
                         -> port <- infrastructure
```

- application service가 트랜잭션 경계를 소유하고, 외부 뉴스 API와 LLM 호출은 긴 DB 트랜잭션 밖에서 실행한다.
- 모듈 간 구현 결합이 필요한 경계는 `core` port로 분리한다. 허용 의존성은 [Module Dependency Policy](./module-dependencies.md)를 따른다.
- 인증과 소유권은 `accountId`를 기준으로 판단한다. 세부 경계는 [Authentication Architecture](./authentication.md)를 따른다.
- Redis에는 인증 보호, token, 좋아요 보호, 조회수처럼 복구 가능한 보조 상태만 둔다.
- 공개 조회는 LLM이나 외부 뉴스 API를 호출하지 않는다. 비용이 드는 호출은 사용자 요청 또는 admin/system 흐름에서만 수행한다([AI Cost Policy](../policy.md#ai-cost)).

## Ingest

`ingest`는 전체 뉴스 처리 순서를 조율한다. 뉴스 검색은 `source`, 초안 생성은 `core`의 generator port 구현인 `ai`, 게시글 저장은 `core`의 `PostWriter` 구현인 `board`가 맡는다.

### 뉴스 수집과 자동 게시

현재 자동 게시 구현은 등록된 키워드별 출시 뉴스를 대상으로 다음 순서로 실행된다.

```text
tracked keyword 또는 admin 요청
  -> Naver 뉴스 검색
  -> 링크 중복 제거 및 vector_store 적재
  -> 중복·콘텐츠·일일 상한 gate
  -> 관련 문서 검색 + LLM 초안 생성
  -> post와 출처 링크를 함께 저장
```

1. `IngestProductNewsUseCase`가 키워드와 주제를 조합해 검색하고 결과를 RAG용 `vector_store`에 적재한다. 수동 문서 적재는 여기서 끝나며 게시글을 만들지 않는다.
2. `PublishLaunchNewsUseCase`가 canonical URL 중복, 광고성 문구, 출처, 필수 키워드, 키워드별 일일 상한을 순서대로 검사한다. 이 gate는 LLM 호출 전에 실행되어 불필요한 비용을 막는다.
3. 통과한 기사만 `LaunchNewsPostDraftGenerator`로 초안을 만든다. 기존 벡터 문서는 보조 자료이며 주 기사의 날짜·출처와 구분한다.
4. `LaunchNewsPostRecorder`가 짧은 트랜잭션에서 `PRODUCT_LAUNCH_NEWS` 게시글과 출처 링크를 함께 저장한다. 링크 없이 글만 남아 다음 실행에서 재게시되는 상태를 막기 위해 둘을 같은 트랜잭션으로 묶는다.

분야는 LLM이 판정하지 않는다. 수동 요청의 `category` 또는 `tracked_keywords.category`가 게시글의 `boardCategory`로 전달되며, 값이 없으면 `GENERAL`이다.

후보의 중복, AI 초안 생성 실패, 저장 시 `DataIntegrityViolationException`은 skip으로 기록하고 나머지 후보를 처리한다. 그 밖의 수집·적재·저장 예외는 해당 키워드 실행을 중단할 수 있으며, scheduler는 키워드 단위로 예외를 격리해 다음 키워드를 처리한다. 저장된 미게시 기사를 나중에 꺼내는 영속 초안 큐나 독립 예약 발행 기능은 없으며, 스케줄 실행 때 수집·초안 생성·게시가 한 흐름으로 진행된다.

### 데일리 포스트

```text
전날 게시된 PRODUCT_LAUNCH_NEWS
  -> 분야별 title/summary 조회
  -> 중복 확인
  -> LLM 데일리 초안 생성
  -> DAILY_DIGEST 게시
```

`PublishDailyDigestUseCase`는 모든 `BoardCategory`를 순회한다. 입력은 수집 원문 전체가 아니라 전날 실제 게시된 출시 뉴스의 `title`과 `summary`다. 분야별 입력이 없으면 건너뛰고, 결정적인 제목(`[분야] 데일리 브리핑 - 날짜`)으로 재실행 중복을 막는다. AI 실패는 해당 분야만 건너뛰며 다른 분야는 계속 처리한다.

`DailyDigestScheduler`는 활성화된 경우 서울 시간 매일 06:00에 전날 뉴스를 처리한다. 활성 조건과 전체 스케줄 값은 [자동 게시 스케줄](../api/README.md#자동-게시-스케줄)을 따른다.

## AI

LLM은 application port 뒤에서 실행하며 provider와 model은 실행 설정으로 선택한다. 현재 호출 지점은 사용자 RAG 채팅, 출시 뉴스 초안, 데일리 초안이다.

### 공통 안전·실패 경계

`AiSafetyGuard`가 호출 전 입력을 검사하고 호출 후 출력을 정리한다. 내부 프롬프트·secret 노출 요청은 LLM을 호출하지 않고 거절하며, 출력에서 API key나 내부 프롬프트 흔적을 찾으면 전체 응답을 고정 거절문으로 바꾼다. 이 규칙은 보조 방어이므로 secret 자체를 프롬프트에 넣지 않는 원칙을 함께 지킨다.

LLM adapter는 장애 시 null을 반환하고 metric을 남긴다. 채팅은 고정 fallback으로 응답하고, 자동 게시 generator는 `Optional.empty()`를 반환해 해당 기사나 분야만 skip한다.

### RAG 채팅

`ChatService`는 안전 검사를 통과한 질문으로 PgVector에서 관련 문서 최대 5개를 검색한 뒤 system prompt에 넣어 답변을 생성한다. 검색 결과가 없으면 빈 컨텍스트로 계속하고, vector store 장애는 `503 EXTERNAL_SERVICE_UNAVAILABLE`로 구분한다. 생성 결과가 비어 있으면 고정 fallback을 반환한다.

출시 뉴스 초안도 관련 문서 최대 5개를 검색하지만, 데일리 초안은 게시판에서 받은 전날 뉴스의 `title`과 `summary`만 사용하고 벡터 검색을 하지 않는다.

## Auth

인증은 Spring Security와 JWT를 사용하며 Redis가 이메일 인증, 로그인 시도 제한, refresh token, OAuth2 일회성 교환 코드를 보관한다. endpoint별 요청 형식은 [API 문서](../api/README.md), token/cookie와 오류 경계는 [Authentication Architecture](./authentication.md)를 따른다.

- 이메일 발송 제한은 Redis Lua script로 cooldown·rate·lock을 원자적으로 평가하며 장애 시 fail-closed다. 인증 token과 OAuth2 교환 code는 `getAndDelete`로 한 번만 소비한다.
- 로그인은 BCrypt 비교 전에 저비용 Redis guard를 통과해야 한다. 성공하면 실패 기록을 지우고 access/refresh token을 발급한다.
- 재발급은 Redis 저장 refresh token과 요청 token을 상수 시간 비교한 뒤 새 refresh token으로 rotation한다. 동시성 한계는 [ADR-002](../decisions/adr-002-refresh-token-rotation.md)에 기록한다.
- 비밀번호 변경과 로그아웃은 저장된 refresh token을 폐기한다. 이미 발급된 stateless access token은 만료까지 유효하다.

Redis key 소유권은 [DB Schema Ownership](../database/schema-ownership.md#non-relational-storage)을 따른다.

## Board

`board`는 사용자 게시글과 자동 생성 게시글의 공통 저장·조회 경계를 소유한다. 자동 게시에서는 `core` port를 구현해 `ingest`가 board 구현에 직접 의존하지 않게 한다.

- 목록 조회는 필터를 조합해 페이징하고 좋아요·조회수·댓글 수·출처 링크를 post ID 단위로 batch 조회한다. 근거는 [N+1 분석](../performance/results.md#n1-분석)에 있다.
- 상세 조회수는 인증 사용자에 한해 Redis에서 계정별 중복 증가를 막고 DB로 동기화한다. 정확성·손실 경계는 [Redis 캐시 전략](./redis-cache-strategy.md)과 [ADR-001](../decisions/adr-001-use-redis-for-view-count.md)을 따른다.
- 게시글 수정·삭제는 owner 또는 ADMIN 권한을 검사한다. 삭제는 연결 파일과 조회수 캐시를 정리한 뒤 게시글을 물리 삭제한다.
- 좋아요는 Redis cooldown/rate guard를 거치고 DB unique constraint로 동일 관계의 중복 row 생성을 막는다. DB가 source of truth이며 Redis 장애 시 쓰기를 fail-closed한다.
- 파일 업로드는 앱 서버가 바이트를 중계하지 않고 S3 presigned URL을 발급한다. 연결되지 않은 metadata는 cleanup scheduler가 정리한다.

댓글, 파일, 게시글 endpoint의 세부 단계와 응답 상태는 [API 문서](../api/README.md)를 따른다.
