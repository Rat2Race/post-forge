# ADR-009 release/postforge에서 학습 플랫폼(study)을 떼어 낸다

상태: **Accepted** (2026-10-19). 이 브랜치(`release/postforge`)에서 [ADR-008](./adr-008-switch-to-learning-platform.md)의 제품 정의(사용자 자료 기반 학습 플랫폼)를 대체한다. ADR-008의 뉴스 퇴역 결정은 그대로다. 학습 플랫폼은 `release/postforge-study`에서 이어 간다.

## 배경

- 2026-10-19에 두 주제를 브랜치로 나눴다. `release/postforge`는 게시판 서비스로 두고 구현·측정·트러블슈팅 과정을 기술 블로그 글로 남긴다. 학습 플랫폼은 `release/postforge-study`에서 따로 만든다.
- 두 브랜치가 같은 지점(`aca2fe7b`)에서 갈라져, 게시판 브랜치에도 `study` 모듈과 학습 표·화면·문서가 그대로 있었다.
- 같은 날 게시판을 단일 모듈로 다시 짜는 작업을 시작했다가 모듈러 모놀리스를 유지하기로 하고 되돌렸다. 그 작업은 `backup/board-single-module` 브랜치에 보관했다.

## 결정

- `study` 모듈과 그에 딸린 코드·화면·표·문서를 이 브랜치에서 지운다. 모듈 구조(`app`, `core`, `support`, `auth`, `board`, `ai`)와 나머지 기능은 그대로 둔다.
- `ai` 모듈은 LLM 호출 경로(`TextGenerationClient`, LLM 설정·adapter·metric)만 남긴다. 게시판에 AI 기능을 더할 때 이 경로를 쓴다.
- 학습 표는 `V0000`을 고치지 않고 `V0001__drop_study_tables.sql`로 지운다. 이미 `V0000`이 적용된 DB의 checksum을 깨지 않기 위해서다.

## 지운 것

| 대상 | 이유 |
|---|---|
| `study` 모듈, `/study.html` | 학습 플랫폼은 `release/postforge-study`가 맡는다 |
| `core`의 `StudyAssistant`·`QuestionDraft` | study와 ai 사이의 계약이라 study가 빠지면 쓰는 곳이 없다 |
| `ai`의 `StudyAssistantService`, 학습 프롬프트 3개, `PromptResourceLoader` | 학습 문제·AI 학생 질문·꼬리질문 전용이다. 프롬프트 로더는 학습 프롬프트만 읽었다 |
| `/api/study/**` 보안 규칙과 OpenAPI study 그룹 | 엔드포인트가 사라졌다 |
| `study_sources`, `study_questions`, `study_records` 표 | 쓰는 코드가 없다 |
| 문서의 학습 절(API 명세 Study, 요청 흐름 Study, 정책의 학습 권한·AI 호출 지점, 표 소유권) | 설명하던 기능이 사라졌다 |

## 결과

- 운영 DB에 학습 데이터가 있으면 이 브랜치 이미지를 배포할 때 `V0001`이 지운다. 남겨야 하면 배포 전에 세 표를 백업한다.
- `ai` 모듈을 쓰는 기능이 아직 없다. 그래도 LLM 설정은 기동 조건으로 남아, 운영은 `LLM_GATEWAY_BASE_URL`·`LLM_CHAT_MODEL`이 없으면 뜨지 않는다.
- `ai` 모듈은 더 이상 `core`를 참조하지 않는다. 허용 의존성 표와 `ModuleBoundaryTest`에서 `ai`의 허용 대상을 비웠다.
