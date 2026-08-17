# PostForge 포트폴리오 안내

> 이 문서는 채용 담당자와 기술 리뷰어가 PostForge를 빠르게 파악하도록 돕는 진입점이다.
> 세부 근거는 각 링크의 기존 문서에 있고, 이 문서는 그 위에 읽는 순서와 맥락만 얹는다.

## 30초 요약

PostForge는 Spring Boot 3 / Java 21 기반의 **DDD-lite 모듈러 모놀리스** 커뮤니티 백엔드다.
게시판 도메인(게시글·댓글·좋아요·조회수·파일) 위에 **신상품 출시 뉴스 자동 게시**, **가격 판정 기반**, **구매 판단 투표**를 얹었다. Naver Shopping 검색 종료 대응으로 가격 판정은 대체 소스 연결 전까지 명시적으로 비활성화했다.
11개 모듈의 허용 의존성을 명시하고, 역방향 결합은 `core` 포트로 끊었다. 인증·조회수·아웃박스·외부 연동에는 장애 정책과 트레이드오프를 남겼다.

## 이 프로젝트가 증명하려는 것

- **경계 있는 설계** — 기능 모듈이 허용된 application API 또는 `core` 포트로 협력하고 서로의 구현에는 결합하지 않는다.
- **근거 있는 성능 개선** — 병목을 추측이 아니라 CPU / SQL count / p95로 분리하고 before/after를 남겼다.
- **장애를 전제한 인프라 판단** — Redis·외부 API·LLM 호출마다 실패 시 동작(fail-closed / degraded)을 명시했다.
- **정직한 한계 기록** — 알려진 레이스 컨디션, 미완성 확장 지점, 측정의 범위를 숨기지 않고 문서화했다.

## 누구를 위한 안내

| 시간이 이만큼 있다면 | 이 순서로 읽으면 된다 |
| --- | --- |
| 5분 (채용 담당자) | 이 문서 → [엔지니어링 하이라이트](./engineering-highlights.md)의 요약 표 |
| 30분 (백엔드 리뷰어) | 하이라이트 전체 → [모듈 의존성](../architecture/module-dependencies.md) → [성능 리포트](../performance/README.md) |
| 깊이 (아키텍처 리뷰어) | [ADR 3편](../decisions/) → [이벤트 아웃박스](../architecture/event-driven-outbox.md) → [트레이드오프와 한계](./trade-offs-and-limitations.md) |

## 시스템 한눈에

| 구분 | 내용 |
| --- | --- |
| 언어 / 런타임 | Java 21, Spring Boot 3.x |
| 구조 | 11 모듈 모듈러 모놀리스 (`app` `auth` `board` `source` `ingest` `catalog` `price` `ai` `messaging` `core` `support`) |
| 저장소 | PostgreSQL(pgvector 포함), Redis |
| 인증 | JWT access + Redis refresh token rotation, OAuth2, 이메일 인증 |
| AI / RAG | Spring AI, OpenAI, PgVector 문서 검색·초안 생성 foundation |
| 비동기 | 트랜잭셔널 아웃박스(`messaging`), optional relay 확장 지점 |
| 배포 | Docker layered jar, GitHub Actions, Docker Hub runtime image |
| 관측 | Micrometer / Prometheus, Grafana, 외부 fetch·DB persist·LLM 계측 timer |

## 내가 설명할 수 있는 깊이

포트폴리오에서 중요한 건 코드 줄 수가 아니라 "이 결정을 왜 이렇게 했는지"를 설명할 수 있는 범위다.

- **직접 설계·구현하고 코드 레벨까지 설명 가능**
  - 인증 흐름: JWT stateless access + Redis refresh rotation, OAuth2 exchange-code handoff, 이메일 인증
  - 게시판 도메인: 게시글·댓글/대댓글·좋아요·조회수·파일 업로드, 작성자 소유권 검증
  - 모듈 경계 정책: `core` 계약과 `support` 인프라 분리, 기능 모듈 간 직접 결합 제거
- **구조와 설계 의도를 설명 가능** (상세는 [하이라이트](./engineering-highlights.md) 참고)
  - 트랜잭셔널 아웃박스로 dual-write 문제를 다루는 방식
  - Redis 조회수 버퍼링과 dirty rename 동기화
  - 성능 병목 분리 절차와 before/after 근거
- **확장 지점으로 남겨둔 foundation** (완성 주장 아님)
  - 아웃박스 relay / 외부 MQ adapter
  - AI 초안 생성 guardrail과 상품 수집 파이프라인

## 함께 보면 좋은 문서

- 결정 기록 — [ADR-001 조회수 Redis](../decisions/adr-001-use-redis-for-view-count.md) · [ADR-002 refresh rotation](../decisions/adr-002-refresh-token-rotation.md) · [ADR-003 모듈러 모놀리스](../decisions/adr-003-modular-monolith.md)
- 아키텍처 — [모듈 의존성](../architecture/module-dependencies.md) · [이벤트 아웃박스](../architecture/event-driven-outbox.md) · [Redis 캐시 전략](../architecture/redis-cache-strategy.md) · [인증](../architecture/authentication.md)
- 성능 — [성능 리포트 인덱스](../performance/README.md) · [N+1 분석](../performance/n-plus-one-analysis.md) · [Redis 캐시 벤치마크](../performance/redis-cache-benchmark.md)
- API / 데이터 — [API 개요](../api/README.md) · [스키마 오너십](../database/schema-ownership.md)
- 운영 — [트러블슈팅](../troubleshooting/) · [프로젝트 README](../../README.md)
