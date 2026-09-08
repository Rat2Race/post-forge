# PostForge 문서 안내

PostForge는 뉴스 수집·분류·LLM 초안 작성·스케줄 자동 게시를 수행합니다. 매일 06:00(Asia/Seoul)에 전날 게시된 뉴스를 종합한 데일리 포스트를 발행하며, 메일 구독은 향후 계획입니다.

이 문서는 문서의 진입점이자 정본(canonical source) 경계를 정의합니다.
같은 표나 규칙을 여러 문서에 복사하지 않고, 소유 문서에 상세를 두고 나머지는 링크합니다.

## 빠르게 읽기

| 목적 | 읽는 순서 |
| --- | --- |
| 프로젝트 전체 파악 | [루트 README](../README.md) → [제품 범위와 향후 계획](./decisions/adr-005-subscription-information-service.md) |
| 뉴스·데일리 자동 게시 | [처리 흐름](./architecture/flows.md#ingest) → [스케줄 설정](./api/README.md#자동-게시-스케줄) |
| API 사용 | [API 명세](./api/README.md) → 실행 중 Swagger UI |
| 요청 내부 흐름 | [요청 흐름](./architecture/flows.md) |
| 모듈 경계 검토 | [Module Dependency Policy](./architecture/module-dependencies.md) → [ADR-003](./decisions/adr-003-modular-monolith.md) |
| 데이터 검토 | [Schema Ownership](./database/schema-ownership.md) → [MVP ERD](./database/postforge-mvp-erd.md) |
| 성능 근거 | [성능 리포트](./performance/README.md) |

## 정본 경계

| 위치 | 정본 책임 | 다른 문서의 원칙 |
| --- | --- | --- |
| [`api/`](./api/README.md) | endpoint, DTO, HTTP status, 인증 조건 | 시나리오에는 관련 API 문서 링크만 둔다 |
| [`policy.md`](./policy.md) | 권한, 데이터 소유, 비용, 삭제, retention invariant | API 표나 모듈 의존성을 반복하지 않는다 |
| [`architecture/`](./architecture/) | runtime 흐름, 모듈 경계, infrastructure ownership | business rule과 HTTP contract는 각 정본으로 넘긴다 |
| [`architecture/flows.md`](./architecture/flows.md) | 요청부터 저장소까지의 내부 처리 흐름 | endpoint 세부 규격은 API 문서로 넘긴다 |
| [`database/`](./database/) | schema ownership, ERD, Flyway 규칙 | 정책 문서에는 필요한 table 의미만 쓴다 |
| [`decisions/`](./decisions/) | 선택 이유, 대안, 트레이드오프 | 현재 구조 설명은 architecture 문서로 넘긴다 |
| [`performance/`](./performance/README.md) | 측정 방법, 결과, historical baseline | 현재 capacity와 과거 수치를 명확히 구분한다 |

## 작성 원칙

- 파일명은 소문자 kebab-case를 기본으로 합니다.
- 코드·설정·endpoint와 어긋난 설명을 발견하면 정본부터 수정합니다.
- 판단 문서는 결론, 근거, 트레이드오프, 후속 과제를 구분합니다.
- 측정 문서는 환경, 실행 명령, commit/image, 원본 artifact 위치를 기록합니다.
- historical artifact는 당시 사실을 보존하고 현재 동작처럼 표현하지 않습니다.
- 향후 계획은 현재 구현과 구분하며, 대체된 제품 방향은 ADR의 과거 배경으로만 남깁니다.
- 내부 IP, credential, token, 운영 사용자 데이터는 문서에 남기지 않습니다.
