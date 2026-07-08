# PostForge 문서 안내

PostForge 문서는 내가 한국어로 읽고 판단할 수 있도록 설계 근거, 결정 기록, 성능 근거, 장애 대응을 분리해서 관리한다.
새 문서를 추가할 때는 아래 위치와 작성 원칙을 먼저 확인한다.

## 현재 제품 방향

2026-06-22 기준 문서의 제품 방향은 다음 세 흐름을 기준으로 맞춘다.

- `PRODUCT_LAUNCH_NEWS`: 신상품 출시 관련 뉴스만 자동 게시한다.
- `POST /api/price-checks`: 사용자가 입력한 상품 가격을 Naver Shopping 기준으로 response-only 판정한다.
- `post_purchase_vote`: 출시 뉴스 게시글에 `BUYABLE`, `UNSURE`, `WAIT` 구매 판단 투표를 붙인다.

가격 판정 결과가 배송비 포함 여부를 확정하지 못하면 `배송비 포함 여부 미확인` 경고를 유지한다.


## 문서 경계

중복을 줄이기 위해 문서별 canonical 책임을 다음처럼 둔다.

| 위치 | Canonical 책임 | 다른 문서에서 다룰 때 |
| --- | --- | --- |
| `api/` | endpoint, request/response DTO, HTTP status, auth/security label | 시나리오나 정책 문서에서는 링크만 건다 |
| `policy/` | 권한, 데이터 소유, 비용, 삭제, retention 같은 제품 invariant | endpoint 표나 모듈 의존성 설명을 반복하지 않는다 |
| `architecture/` | runtime/module boundary, dependency direction, infrastructure ownership | business rule은 `policy/`, HTTP contract는 `api/`로 넘긴다 |
| `usecase/` | actor별 시나리오와 acceptance flow | API 목록은 navigation hint로만 두고 상세 contract는 `api/`를 따른다 |
| `database/` | code-backed schema ownership, ERD, DBML, migration 판단 | 정책/시나리오 문서에서는 table 의미만 요약한다 |

## 구조

| 폴더 | 용도 |
| --- | --- |
| `architecture/` | 현재 시스템이 어떻게 동작하는지, 설계 경계와 흐름 정리. 모듈별 API 흐름은 [`architecture/flows/`](./architecture/flows/README.md) |
| `api/` | 모듈별 HTTP API 명세와 공통 요청/응답 규칙 |
| `decisions/` | 왜 그런 결정을 했는지 남기는 ADR |
| `troubleshooting/` | 장애 증상, 확인 순서, 복구 절차 |
| `database/` | schema ownership, ERD, DBML, migration 관련 판단 |
| `policy/` | 기능/운영/보안/비용 정책 |
| `usecase/` | 사용자/시스템 시나리오와 acceptance 흐름 |

## 작성 원칙

- 파일명은 소문자 kebab-case를 기본으로 한다. 예: `module-dependencies.md`
- 판단을 남기는 문서는 "결론", "근거", "트레이드오프", "후속 과제"를 빠뜨리지 않는다.
- 측정 문서는 사용 도구, 버전, 실행 명령, 환경, 원본 아티팩트 위치를 남긴다.
- 운영 문서는 먼저 볼 증상, 확인 순서, 복구 절차, 재발 방지 항목을 남긴다.
- 코드 경로, 설정 키, HTTP endpoint, token 이름처럼 정확성이 중요한 식별자는 영어를 유지한다.
