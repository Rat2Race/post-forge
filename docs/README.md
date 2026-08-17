# PostForge 문서 안내

이 문서는 PostForge 문서의 진입점이자 정본(canonical source) 경계를 정의합니다.
같은 표나 규칙을 여러 문서에 복사하지 않고, 소유 문서에 상세를 두고 나머지는 링크합니다.

## 빠르게 읽기

| 목적 | 읽는 순서 |
| --- | --- |
| 프로젝트 전체 파악 | [루트 README](../README.md) → [포트폴리오 안내](./portfolio/README.md) |
| API 사용 | [API 명세](./api/README.md) → 실행 중 Swagger UI |
| 요청 내부 흐름 | [모듈별 API 흐름](./architecture/flows/README.md) |
| 모듈 경계 검토 | [Module Dependency Policy](./architecture/module-dependencies.md) → [ADR-003](./decisions/adr-003-modular-monolith.md) |
| 데이터 검토 | [Schema Ownership](./database/schema-ownership.md) → [MVP ERD](./database/postforge-mvp-erd.md) |
| 장애 대응 | [Troubleshooting](./troubleshooting/) |
| 성능 근거 | [성능 리포트](./performance/README.md) |
| 학습·설계 참고 | [`learning/`](./learning/) |

## 정본 경계

| 위치 | 정본 책임 | 다른 문서의 원칙 |
| --- | --- | --- |
| [`api/`](./api/README.md) | endpoint, DTO, HTTP status, 인증 조건 | 시나리오에는 관련 API 문서 링크만 둔다 |
| [`policy/`](./policy/) | 권한, 데이터 소유, 비용, 삭제, retention invariant | API 표나 모듈 의존성을 반복하지 않는다 |
| [`architecture/`](./architecture/) | runtime 흐름, 모듈 경계, infrastructure ownership | business rule과 HTTP contract는 각 정본으로 넘긴다 |
| [`architecture/flows/`](./architecture/flows/README.md) | 요청부터 저장소까지의 내부 처리 흐름 | endpoint 세부 규격은 API 문서로 넘긴다 |
| [`database/`](./database/) | schema ownership, ERD, DBML, Flyway 규칙 | 정책 문서에는 필요한 table 의미만 쓴다 |
| [`decisions/`](./decisions/) | 선택 이유, 대안, 트레이드오프 | 현재 구조 설명은 architecture 문서로 넘긴다 |
| [`performance/`](./performance/README.md) | 측정 방법, 결과, historical baseline | 현재 capacity와 과거 수치를 명확히 구분한다 |
| [`troubleshooting/`](./troubleshooting/) | 증상, 확인 순서, 복구, 재발 방지 | 설계 설명을 반복하지 않는다 |
| [`portfolio/`](./portfolio/README.md) | 리뷰어를 위한 요약과 근거 탐색 순서 | 상세 주장은 정본 문서에 링크한다 |
| [`learning/`](./learning/) | 학습·설계·작성 참고자료 | 현재 동작이나 포트폴리오 증거로 사용하지 않는다 |

## 증거와 참고자료

포트폴리오의 기술 주장은 현재 코드·테스트와 아래 측정 문서로 증명한다.

| 자료 | 용도 |
| --- | --- |
| [API 명세](./api/README.md) | 현재 controller/DTO/security 계약 |
| [모듈 의존성](./architecture/module-dependencies.md) | 현재 허용 모듈 경계 |
| [Schema Ownership](./database/schema-ownership.md) | 현재 데이터 소유권과 migration 기준 |
| [성능 리포트](./performance/README.md) | 측정 결과와 raw artifact 탐색 |

다음 파일은 학습·설계·작성 참고자료이며 포트폴리오 증거로 사용하지 않는다.

| 문서 | 분류 | 이유 |
| --- | --- | --- |
| [Metrics Guide](./learning/metrics-guide.md) | 학습 자료 | k6/Grafana 지표를 해석하기 위한 일반 개념과 체크리스트 |
| [Cost/Capacity Guide](./learning/cost-capacity.md) | 학습 자료 | 비용·수용량 계산식과 가정을 연습하기 위한 계산 참고서 |
| [Performance Report Template](./learning/report-template.md) | 작성 도구 | 실행 결과가 아니라 새 리포트의 입력 양식 |
| [User Scenarios](./learning/postforge-user-scenarios.md) | 설계 참고 | actor 흐름을 검토하기 위한 문서이며 API·정책의 정본이 아님 |
| [Historical Test Summary](./learning/test-summary.md) | 과거 요약 | 특정 시점 기록이며 현재 pass/fail 증거가 아님 |

## 작성 원칙

- 파일명은 소문자 kebab-case를 기본으로 합니다.
- 코드·설정·endpoint와 어긋난 설명을 발견하면 정본부터 수정합니다.
- 판단 문서는 결론, 근거, 트레이드오프, 후속 과제를 구분합니다.
- 측정 문서는 환경, 실행 명령, commit/image, 원본 artifact 위치를 기록합니다.
- historical artifact는 당시 사실을 보존하고 현재 동작처럼 표현하지 않습니다.
- 내부 IP, credential, token, 운영 사용자 데이터는 문서에 남기지 않습니다.
