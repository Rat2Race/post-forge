# AI API 흐름

Endpoint 명세는 [통합 API 명세의 AI](../../api/README.md#ai)가 canonical이다.

LLM 호출은 application port 뒤에 두고, provider/model은 실행 설정으로 선택한다. 호출 시간과 token은 metric으로 계측한다.

## 안전 가드 원리 (모든 AI 흐름 공통)

`AiSafetyGuard`는 모델 판단에 의존하지 않는 **deterministic 규칙**으로 LLM 호출 앞뒤를 감싼다.

- 입력 방어 `shouldRefuse(...)`: "대상(내부 프롬프트/secret/운영 정보) × 행위(보여줘/무시해)" **조합** 매칭으로 판단해 단일 키워드 오탐을 줄이고, 교육적 질문(`EDUCATIONAL_CONTEXT`)은 예외 처리한다. 거절이면 **LLM을 호출하지 않고** 고정 거절 메시지를 반환한다.
- 출력 방어 `sanitizeOutput(...)`: 응답에 API key 형태(`sk-...` 정규식), secret 할당문, 내부 프롬프트 dump marker가 보이면 **응답 전체를** 거절 메시지로 치환한다(부분 마스킹은 문맥 유추 여지가 남으므로).

한계 인지: 키워드/정규식 방어는 우회 가능하므로 완전한 방어가 아니라 defense-in-depth의 한 층이다. 상위 원칙은 "프롬프트/컨텍스트에 secret을 넣지 않는다"이다.

## POST /api/ai/chat — RAG 채팅

1. `ChatService.chat` — `aiSafetyGuard.shouldRefuse(message)` 통과 못 하면 즉시 거절 메시지 반환(LLM/검색 비용 0).
2. `ChatService`가 `SearchPort.searchSimilar(message, 5)`를 호출해 PgVector `vector_store`에서 유사 문서 top-5를 검색한다. 결과 0건은 빈 목록으로 계속 처리하고, store 장애는 `503 EXTERNAL_SERVICE_UNAVAILABLE`로 종료해 서로 구분한다.
3. 검색 결과를 system prompt에 넣어(`ChatPromptTemplate`) `TextGenerationClient.generate` 호출.
4. 응답을 `sanitizeOutput`으로 검사하고, null/blank면 고정 fallback 메시지로 대체한다.

원리: LLM adapter는 예외를 던지지 않고 null을 반환한다(로그 + degraded metric). 호출부는 null을 fallback으로 처리하므로 LLM 장애가 5xx로 전파되지 않는다.

## launch-news 초안 port

`LaunchNewsPostDraftGenerationService`는 `core`의 `LaunchNewsPostDraftGenerator` port 구현으로, `ingest`의 출시 뉴스 자동 게시가 모듈 경계를 넘어 호출한다. 실패는 `Optional.empty()`로 반환되고 호출부(`ingest`)가 `AI_GENERATION_FAILED` skip으로 처리한다. 흐름 전체는 [ingest-flows](./ingest-flows.md).

## 비용 경계

AI 호출은 명시적 사용자 채팅 요청 또는 gated admin/system 경로(launch-news)에서만 발생한다. 게시글 목록/상세 같은 public read path는 AI를 호출하지 않는다. 정책: [AI Cost Policy](../../policy/ai-cost-policy.md)

## 검증 레이어

AI 흐름은 외부 LLM/PgVector를 항상 실제로 호출하지 않고, 아래 순서로 분리해서 확인한다.

| 레이어 | 대상 | 확인 내용 |
| --- | --- | --- |
| 단위 테스트 | `ChatService`, `LaunchNewsPostDraftGenerationService`, `AiSafetyGuard` | safety pre-check, fallback, sanitize, public post 차단 정책 |
| 어댑터 테스트 | `LlmTextGenerationAdapter`, `LlmProductEmbeddingClient`, `PgVectorSearchAdapter` | Spring AI `ChatModel`/`EmbeddingModel`/`VectorStore` 호출 변환과 실패 metric |
| HTTP slice 테스트 | `ChatController` | `/api/ai/chat` 요청/응답 shape와 validation |
| 수동 smoke | [`ai-chat-smoke.http`](../../api/ai-chat-smoke.http), [`naver-source-smoke.http`](../../api/naver-source-smoke.http) | 로컬 gateway, app 인증 API, AI endpoint, Naver/source/admin write path 연결 확인 |

자동 테스트는 실제 LLM 비용과 네트워크 상태에 의존하지 않는다. 실제 gateway/Ollama/Naver/PgVector 연결은 `.http` smoke로 분리해 필요할 때만 실행한다.
