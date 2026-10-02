# 요청 흐름

PostForge는 사용자가 올린 자료로 학습하는 서비스다. 학습 흐름은 [Study](#study)에 있다. 뉴스 수집·자동 게시·데일리 종합은 [ADR-008](../decisions/adr-008-switch-to-learning-platform.md)에 따라 지웠다.

Endpoint, DTO, status의 정본은 [API 문서](../api/README.md)다. 이 문서는 모듈 경계, 실패 처리, 일관성 보장만 설명한다.

## 공통 구조 원리

```text
presentation -> application -> domain
                         -> port <- infrastructure
```

- application service가 트랜잭션 경계를 소유하고, LLM 호출은 긴 DB 트랜잭션 밖에서 실행한다.
- 모듈 간 구현 결합이 필요한 경계는 `core` port로 분리한다. 허용 의존성은 [Module Dependency Policy](./module-dependencies.md)를 따른다.
- 인증과 소유권은 `accountId`를 기준으로 판단한다. 세부 경계는 [Authentication Architecture](./authentication.md)를 따른다.
- Redis에는 인증 보호, token, 좋아요 보호, 조회수처럼 복구 가능한 보조 상태만 둔다.
- 공개 조회는 LLM을 호출하지 않는다. 비용이 드는 호출은 사용자 요청 또는 admin/system 흐름에서만 수행한다([AI Cost Policy](../policy.md#ai-cost)).

## AI

LLM은 `core`의 `StudyAssistant` port 뒤에서 실행하며 provider와 model은 실행 설정으로 선택한다. 호출 지점은 학습 문제 초안·AI 학생 질문·꼬리질문뿐이다.

LLM adapter는 장애 시 null을 반환하고 metric을 남긴다. 학습은 규칙 문제와 빠진 핵심 항목을 되묻는 질문으로 대신한다. LLM 출력은 문제 초안과 질문으로만 쓰고, 근거 문장이 자료에 그대로 있어야 저장한다([Study](#study)).

## Auth

인증은 Spring Security와 JWT를 사용하며 Redis가 이메일 인증, 로그인 시도 제한, refresh token, OAuth2 일회성 교환 코드를 보관한다. endpoint별 요청 형식은 [API 문서](../api/README.md), token/cookie와 오류 경계는 [Authentication Architecture](./authentication.md)를 따른다.

- 이메일 발송 제한은 Redis Lua script로 cooldown·rate·lock을 원자적으로 평가하며 장애 시 fail-closed다. 인증 token과 OAuth2 교환 code는 `getAndDelete`로 한 번만 소비한다.
- 로그인은 BCrypt 비교 전에 저비용 Redis guard를 통과해야 한다. 성공하면 실패 기록을 지우고 access/refresh token을 발급한다.
- 재발급은 Redis 저장 refresh token과 요청 token을 상수 시간 비교한 뒤 새 refresh token으로 rotation한다. 동시성 한계는 [ADR-002](../decisions/adr-002-refresh-token-rotation.md)에 기록한다.
- 비밀번호 변경과 로그아웃은 저장된 refresh token을 폐기한다. 이미 발급된 stateless access token은 만료까지 유효하다.

Redis key 소유권은 [DB Schema Ownership](../database/schema-ownership.md#non-relational-storage)을 따른다.

## Board

`board`는 게시글·댓글·좋아요·조회수·파일의 저장·조회 경계를 소유한다.

- 목록 조회는 필터를 조합해 페이징하고 좋아요·조회수·댓글 수·출처 링크를 post ID 단위로 batch 조회한다. 근거는 [N+1 분석](../performance/results.md#n1-분석)에 있다.
- 상세 조회수는 인증 사용자에 한해 Redis에서 계정별 중복 증가를 막고 DB로 동기화한다. 정확성·손실 경계는 [Redis 캐시 전략](./redis-cache-strategy.md)과 [ADR-001](../decisions/adr-001-use-redis-for-view-count.md)을 따른다.
- 게시글 수정·삭제는 owner 또는 ADMIN 권한을 검사한다. 삭제는 연결 파일과 조회수 캐시를 정리한 뒤 게시글을 물리 삭제한다.
- 좋아요는 Redis cooldown/rate guard를 거치고 DB unique constraint로 동일 관계의 중복 row 생성을 막는다. DB가 source of truth이며 Redis 장애 시 쓰기를 fail-closed한다.
- 파일 업로드는 앱 서버가 바이트를 중계하지 않고 S3 presigned URL을 발급한다. 연결되지 않은 metadata는 cleanup scheduler가 정리한다.

댓글, 파일, 게시글 endpoint의 세부 단계와 응답 상태는 [API 문서](../api/README.md)를 따른다.

## Study

`study`는 사용자가 올린 자료로 문제 풀기·간격 반복·빈 페이지 정리·가르치기를 하는 학습 루프를 소유한다. LLM은 `core`의 `StudyAssistant` port 뒤에 있고 `ai`가 구현한다. 원칙과 게이트는 [ADR-008](../decisions/adr-008-switch-to-learning-platform.md), endpoint와 규칙은 [API 문서](../api/README.md#study)를 따른다.

```text
자료 업로드(커밋) -> 메모리 실행기: LLM 문제 초안 -> 근거 검증 -> 문제 저장 + READY
                                    (실패·전부 버림) -> 규칙 문제
오늘 할 것 -> 답하기 -> 근거 확인 -> 자가 평가 -> 상자 이동                    (LLM 없음)
          -> 빈 페이지 정리 -> 핵심 항목 대조(겹침 제안) -> 직접 체크 -> 자료별 일정  (LLM 없음)
버튼: 가르치기 · 꼬리질문 -> StudyAiService -> LLM 1회
```

- LLM 경로는 문제 생성과 `StudyAiService`(가르치기·꼬리질문)뿐이다. 매일 반복 루프의 `StudyPracticeService`는 `StudyAssistant`를 갖지 않아 LLM을 부를 수 없다. LLM 호출은 트랜잭션 밖에서 하고 결과 저장만 짧은 트랜잭션으로 묶는다.
- 채점은 학습자가 한다. LLM 출력은 문제 초안과 질문으로만 쓰고, 문제는 근거 문장이 자료에 그대로 있어야 저장된다. LLM이 실패하거나 초안이 모두 버려지면 마크다운 제목·목록으로 규칙 문제를 만들고, 꼬리질문은 앞 근거로 예를 묻는 문제로 대신한다.
- 문제 생성은 자료 저장을 커밋한 뒤 메모리 실행기에서 돈다. 재시작으로 잃은 생성은 기동 직후(`ApplicationReadyEvent`) `GENERATING` 자료를 다시 맡겨 잇는다. 인스턴스 하나를 전제하며, 여럿이면 DB 작업 큐로 옮긴다.
- 복습은 `StudyQuestion`의 `@Version`과 예정 전 제출 거절(`409 NOT_DUE_YET`)로 동시·중복 제출을 한 번만 반영한다. 빈 페이지 일정도 `StudySource`의 `@Version`으로 겹친 갱신을 막는다.
- 학습 현황(잔디·연속 학습일·게이트 수치)은 기록 테이블에서 그때그때 집계하고 따로 저장하지 않는다.
