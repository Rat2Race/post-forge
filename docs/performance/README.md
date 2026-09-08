# 성능 리포트

성능 테스트 결과를 Git에 남길 때는 원본 로그 전체보다 사람이 비교할 수 있는 요약 리포트를 우선한다.

## 현재 상태

이 repo에는 현재 전용 부하 테스트 모듈이나 runner를 두지 않는다.
Gradle `:app:smoke` task와 `app/src/smoke` source set은 2026-06-11 기준 제거되었다.
예전 `tests/k6/**`, `tests/bruno/**`, `setup/**`, 앱 repo의 부하 테스트 runner는 더 이상 실행 표면이 아니지만, 과거 k6/Grafana/manual run/raw 리포트는 정량 근거로 계속 보관한다.

운영 host의 2026-06-17 관측값은 [서버 환경 기록](./prod-environment-spec.md)에 있다. 과거 Oracle ARM 실행과 2026-08-21 로컬 실행은 환경이 달라 현재 운영 수용량으로 해석하지 않는다.
보관된 측정은 주로 게시판·인증 API를 대상으로 한다. 현재 제품인 뉴스 수집·LLM 가공·자동 게시·06시 데일리 종합의 전체 처리 성능을 입증하는 자료는 아니다.
현재 수용량을 판단하려면 같은 commit과 환경에서 k6/Bruno, Prometheus, 컨테이너 지표를 같은 시간 구간에 수집해야 한다.

## 문서와 저장 위치

| 역할 | 문서 | 사용 원칙 |
|---|---|---|
| 비교·원인 분석 | [results.md](./results.md) | 시나리오 분리, N+1 개선, Redis 조회수 실험의 요약 |
| 실행 증거 | [runs/](./runs/) | 실행별 Markdown·JSON·Grafana 캡처를 함께 보관 |
| 서버 환경 기록 | [prod-environment-spec.md](./prod-environment-spec.md) | 수집 시점의 host/container 스펙. 부하 결과가 아님 |

## 보관된 실행

| 실행 | 내용 | 해석 범위 |
|---|---|---|
| [2026-03-10 guest mixed](./runs/20260310-160406-guest-mixed/) | 로그인·읽기 혼합 | Oracle ARM의 초기 CPU 병목 |
| [2026-03-10 guest split](./runs/20260310-162722-guest-split/) | 로그인·읽기 시나리오 분리 | 요청 구성 변경에 따른 병목 분리 |
| [2026-03-11 auth CRUD](./runs/20260311-171731-auth-crud/) | 인증 후 CRUD | 당시 인증·쓰기 경로의 실행 기록 |
| [2026-03-12 N+1 개선](./runs/20260312-105320-guest-n1-fix/) | 조회 쿼리 개선 후 재측정 | 개선 전후 비교 근거 |
| [2026-03-13 batch size](./runs/20260313-163011-guest-batch-size/) | batch fetch 설정 후 재측정 | 조회 성능 후속 비교 |
| [2026-04-09 Redis 조회수](./runs/20260409-post-cache/) | 상세·목록 before/after | 상세 개선과 목록 악화를 함께 보존 |
| [2026-05-04 prod smoke](./runs/20260504-160737-prod-smoke/) | 자동 생성 k6 리포트 | commit·리소스 관측이 빠진 당시 실행 기록 |
| [2026-05-12 local smoke](./runs/20260512-140100-smoke/run-summary.md) | k6·Bruno public/auth | 로컬 기능·응답 확인 |
| [2026-08-21 local baseline](./runs/20260821-004500-local-baseline/run-summary.md) | 분야 필터·데일리 도입 후 조회 측정 | M-series 로컬 환경, LLM은 mock |

`runs/`의 endpoint·스크립트·migration 이름은 실행 당시 값이다. 현재 형식에 맞춰 측정 원문을 다시 쓰지 않고, 현재 해석은 `results.md`와 실행 요약에 남긴다.
파일 형식이 같거나 다르다는 이유만으로 원본을 삭제하지 않는다. 중복 산출물을 정리할 때는 결과와 실행 메타데이터가 남는지 먼저 확인한다.

## 현재 검증 표면

현재 repo 안에서 바로 실행하는 검증은 Gradle test와 bootJar 생성이다.
성능 부하 실행은 전용 모듈로 유지하지 않으며, 필요할 때 수동 k6/Bruno 실행 또는 별도 CI smoke suite로 수행하고 산출물을 `docs/performance/runs/<run>/`에 남긴다.

```bash
./gradlew check -PexcludeTags=integration
./gradlew :app:bootJar
```

운영 target에 부하를 주는 실험은 허용된 host, VU/RPS/duration 상한, 실행자와 실행 시간, write 시나리오 여부, 전용 test data 사용 여부, 같은 시간 구간의 CPU/memory/GC/DB/Redis/HTTP metric 수집 여부를 리포트에 함께 남긴다.
새 성능 실험을 추가하면 실행 명령, 대상 환경, 원본 artifact 위치, Prometheus/Grafana 관측 시간을 이 README에 함께 갱신한다.

## 새 실행 기록

파일명은 날짜, 대상, 목적이 보이게 작성한다.

```text
docs/performance/runs/YYYYMMDD-HHMMSS-<target>-<scenario>/
  run-summary.md
  <scenario>-summary.json
  <scenario>-metrics.png
```

각 실행의 요약과 원본을 같은 디렉터리에 둔다. 기존 기록의 파일명은 유지한다.

## 남기는 값

리포트에는 다음 값만 안정적으로 남긴다.

- 테스트 목적과 결론
- 대상 환경, image tag 또는 commit hash
- k6 script, executor, VUs, iterations, duration
- checks, request count, failure rate, RPS
- latency avg/med/p90/p95/p99/max
- 가능하면 CPU, memory, network, disk I/O 관측값
- 외부 source fetch, DB persist, LLM generation timer와 token summary
- Grafana screenshot, k6 summary JSON 같은 artifact 링크
- 다음 조치

## 외부 Source / LLM 계측

뉴스 자동 게시의 처리 비용은 HTTP latency만으로 판단하지 않는다. 앱 내부 metric으로 외부 뉴스 수집, 벡터 적재, RAG 검색, LLM 생성을 분리해 남긴다.

| Metric | 용도 |
|---|---|
| `external_naver_fetch_seconds` | 네이버 호출 시간, outcome/status별 성공·실패 분리 |
| `external_naver_fetch_success_total` | 네이버 검색 성공 횟수 |
| `external_naver_fetch_failure_total` | 예외·HTTP status별 실패 횟수(429/500 포함) |
| `external_naver_fetch_items_total` | 정제·검증을 통과해 반환된 기사 수 |
| `ingest_documents_embeddings_stored_total` | 벡터 저장에 성공한 청크 수 |
| `ingest_documents_embeddings_failed_total` | 벡터 저장에 실패한 청크 수 |
| `ai_vector_search_success_total` | RAG 검색 성공 횟수 |
| `ai_vector_search_degraded_total` | RAG 검색 실패 횟수 |
| `external_source_db_persist_seconds` | source 결과를 DB/vector store에 반영하는 시간 |
| `ai_text_generation_seconds` | LLM 생성 호출 시간 |
| `ai_text_generation_prompt_tokens` | LLM input token 처리량 계산 |
| `ai_text_generation_completion_tokens` | LLM output TPS 계산 |

수집·적재·검색·생성 지표를 같은 시간 구간에서 비교하고, `displayCount`와 `dailyCap`을 별도로 기록한다. 데일리는 대상 분야·전날 게시글 수·생성 성공/건너뜀·완료 시간을 함께 남긴다. 스케줄은 [자동 게시 스케줄](../api/README.md#자동-게시-스케줄)을 따른다. 위 표는 계측 항목이며 실측 수치가 아니다. 외부 API를 호출하지 않는 테스트와 Naver/LLM 실호출 성능 측정을 구분한다.

## 제외할 값

Git에 남기는 리포트에는 민감정보를 넣지 않는다.

- Authorization header, cookie, token
- 요청/응답 body 원문
- 운영 DB 데이터나 사용자 식별자
- 내부 IP, 비공개 domain, 계정명
- 너무 큰 raw JSON/HTML 전체

필요하면 URL은 다음처럼 마스킹한다.

```text
https://prod.example.com -> prod
http://10.x.x.x:8080 -> private-prod
```

## 작성 순서

1. 실행 원본과 `run-summary.md`를 같은 `runs/<run>/`에 저장한다.
2. 환경·commit·명령·지표·실패·결론을 기록하고, 현재 수용량 측정인지 과거 비교인지 구분한다.
3. 비교할 근거가 생기면 [results.md](./results.md)에 원본 링크와 해석을 추가한다.
4. 이 문서의 실행 목록을 갱신하고, 민감정보가 없는지 확인한다.
