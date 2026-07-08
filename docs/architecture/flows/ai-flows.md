# AI API 흐름

Endpoint 명세는 [AI API](../../api/ai.md)가 canonical이다.

## 모듈 구조

```text
ai
├── chat     RAG 채팅 (ChatService)
├── draft    게시글 초안 생성 (PostDraftGenerationService)
│            + launch-news용 초안 port 구현 (LaunchNewsPostDraftGenerationService)
├── search   PgVector 유사 문서 검색 (SemanticSearchService -> SearchPort -> PgVectorSearchAdapter)
└── support  안전 가드(AiSafetyGuard), prompt loader, LLM adapter(LlmTextGenerationAdapter) + metric
```

LLM 연결: `TextGenerationClient` port의 구현이 `LlmTextGenerationAdapter`이며, Spring AI `ChatModel`을 OpenAI-compatible API로 호출한다. 기본 provider는 로컬 LLM gateway(`ollama`, 기본 모델 `qwen3:8b`, `postforge.llm.*` 설정). 호출 시간/token은 `ai_text_generation_*` metric으로 계측한다.

## 안전 가드 원리 (모든 AI 흐름 공통)

`AiSafetyGuard`는 모델 판단에 의존하지 않는 **deterministic 규칙**으로 LLM 호출 앞뒤를 감싼다.

- 입력 방어 `shouldRefuse(...)`: "대상(내부 프롬프트/secret/운영 정보) × 행위(보여줘/무시해)" **조합** 매칭으로 판단해 단일 키워드 오탐을 줄이고, 교육적 질문(`EDUCATIONAL_CONTEXT`)은 예외 처리한다. 거절이면 **LLM을 호출하지 않고** 고정 거절 메시지를 반환한다.
- 출력 방어 `sanitizeOutput(...)`: 응답에 API key 형태(`sk-...` 정규식), secret 할당문, 내부 프롬프트 dump marker가 보이면 **응답 전체를** 거절 메시지로 치환한다(부분 마스킹은 문맥 유추 여지가 남으므로).

한계 인지: 키워드/정규식 방어는 우회 가능하므로 완전한 방어가 아니라 defense-in-depth의 한 층이다. 상위 원칙은 "프롬프트/컨텍스트에 secret을 넣지 않는다"이다.

## POST /api/ai/chat — RAG 채팅

1. `ChatService.chat` — `aiSafetyGuard.shouldRefuse(message)` 통과 못 하면 즉시 거절 메시지 반환(LLM/검색 비용 0).
2. `SemanticSearchService.searchSimilar(message, 5)` — PgVector `vector_store`에서 유사 문서 top-5 검색. **검색이 unavailable이면 LLM을 호출하지 않고** "요청을 처리할 수 없습니다"로 종료한다(결과 0건과 store 장애를 `SearchOutcome`으로 구분).
3. 검색 결과를 system prompt에 넣어(`ChatPromptTemplate`) `TextGenerationClient.generate` 호출.
4. 응답을 `sanitizeOutput`으로 검사하고, null/blank면 고정 fallback 메시지로 대체한다.

원리: LLM adapter는 예외를 던지지 않고 null을 반환한다(로그 + degraded metric). 호출부는 null을 fallback으로 처리하므로 LLM 장애가 5xx로 전파되지 않는다.

## POST /api/ai/generate — 게시글 초안

1. `PostDraftGenerationService.generate` — prompt뿐 아니라 **topic/title/summary/tags까지 varargs로 전부** `shouldRefuse`에 넣는다. 어떤 필드로 주입 시도가 들어와도 프롬프트에 섞이기 전에 걸러진다. 거절이면 제목/본문/요약이 거절 메시지인 응답을 반환한다.
2. LLM으로 초안 본문을 생성하고 `sanitizeOutput` 후, null이면 사용자의 원본 prompt를 본문으로 fallback한다.
3. title/summary는 사용자가 준 값을 우선하고 없으면 본문에서 유도(제목 100자/요약 500자 절단), tags는 중복 제거 후 최대 20개.
4. **초안은 저장되지 않는다.** 사용자가 게시하려면 별도로 `POST /api/posts`를 호출해야 한다 — AI 출력이 검토 없이 public 데이터가 되지 않게 하는 경계다.

## launch-news 초안 port

`LaunchNewsPostDraftGenerationService`는 `core`의 `LaunchNewsPostDraftGenerator` port 구현으로, `ingest`의 출시 뉴스 자동 게시가 모듈 경계를 넘어 호출한다. 실패는 `Optional.empty()`로 반환되고 호출부(`ingest`)가 `AI_GENERATION_FAILED` skip으로 처리한다. 흐름 전체는 [ingest-flows](./ingest-flows.md).

## 비용 경계

AI 호출은 명시적 사용자 요청(chat/generate) 또는 gated admin/system 경로(launch-news)에서만 발생한다. 게시글 목록/상세 같은 public read path는 AI를 호출하지 않는다. 정책: [AI Cost Policy](../../policy/ai-cost-policy.md)
