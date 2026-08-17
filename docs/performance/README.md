# 성능 리포트

성능 테스트 결과를 Git에 남길 때는 원본 로그 전체보다 사람이 비교할 수 있는 요약 리포트를 우선한다.

## 현재 상태

이 repo에는 현재 전용 부하 테스트 모듈이나 runner를 두지 않는다.
Gradle `:app:smoke` task와 `app/src/smoke` source set은 2026-06-11 기준 제거되었다.
예전 `tests/k6/**`, `tests/bruno/**`, `setup/**`, 앱 repo의 부하 테스트 runner는 더 이상 실행 표면이 아니지만, 과거 k6/Grafana/manual run/raw 리포트는 정량 근거로 계속 보관한다.

운영 host 체급은 [prod-environment-spec.md](./prod-environment-spec.md)에 기록되어 있지만, Intel N100 prod 환경에서 동일 k6 시나리오로 새 capacity baseline을 아직 잡지 않았다.
따라서 과거 Oracle ARM 1vCPU / 1GB 환경의 20~25 RPS 수치는 병목 분석의 historical baseline이지 현재 prod 상한이 아니다.
현재 capacity 주장을 하려면 별도 수동/CI 성능 실험으로 k6/Bruno와 Prometheus/컨테이너 지표를 같은 시간 구간에 수집해야 한다.

## 문서 역할

| 역할 | 문서 | 사용 원칙 |
|---|---|---|
| 비교 요약 | `k6-scenario-results.md` | 시나리오별 수치 비교의 정본 |
| 집중 분석 | `n-plus-one-analysis.md`, `redis-cache-benchmark.md` | 원인과 실험별 결론의 정본 |
| 실행 증거 | 날짜가 붙은 리포트, `k6/`, `grafana/`, `manual-runs/` | 당시 실행을 보존하는 historical evidence |
| 학습·계산 참고 | `docs/learning/` | 해석법·계산법·작성 형식 참고. 포트폴리오 성능 주장의 근거로 사용하지 않음 |

## 저장 위치

| 종류 | 위치 | 비고 |
|---|---|---|
| 요약 리포트 | `docs/performance/n-plus-one-analysis.md`, `redis-cache-benchmark.md`, `k6-scenario-results.md` | 주요 병목/벤치마크를 사람이 비교하기 쉬운 표준 문서로 정리 |
| 표준 리포트 | `docs/performance/YYYY-MM-DD-<scenario>.md` | 사람이 읽는 최종 기록 |
| k6 원문 요약 | `docs/performance/k6/` | 과거 실행 결과의 정량 원문. 현재 capacity로 재해석할 때는 환경 차이를 표시 |
| Grafana 캡처 | `docs/performance/grafana/` | JVM/CPU/메모리 관측 근거 캡처. 삭제하지 않고 historical evidence로 보관 |
| 수동 실행 원본 | `docs/performance/manual-runs/` | k6/Bruno JSON, HTML, markdown 원본 산출물 |
| 지표 해석 가이드 | `docs/learning/metrics-guide.md` | 일반적인 k6/Grafana 해석을 위한 학습 참고 |
| 작성 템플릿 | `docs/learning/report-template.md` | 새 리포트 작성 형식 참고 |
| 운영 서버 스펙 | `docs/performance/prod-environment-spec.md` | 현재 prod host/container 체급 기준선. 부하 결과가 아님 |
| 비용/수용량 계산 | `docs/learning/cost-capacity.md` | 공식과 historical input을 사용한 계산 참고. 현재 capacity 근거가 아님 |
| Historical 해석 | `docs/performance/load-analysis.md`, `guest-split.md` | 과거 조사 맥락만 보존한 포인터. 수치는 비교 요약을 우선 |

`k6/`, `grafana/`, `manual-runs/`는 당시 실행을 보존한 raw archive다. 문서 형식이나 현재 endpoint에
맞추기 위해 원본을 다시 쓰지 않고, 현재 해석은 `n-plus-one-analysis.md`,
`redis-cache-benchmark.md`, `k6-scenario-results.md`에 반영한다.

## 현재 검증 표면

현재 repo 안에서 바로 실행하는 검증은 Gradle test와 bootJar 생성이다.
성능 부하 실행은 전용 모듈로 유지하지 않으며, 필요할 때 수동 k6/Bruno 실행 또는 별도 CI smoke suite로 수행하고 산출물만 `docs/performance/`에 남긴다.

```bash
./gradlew test
./gradlew :app:bootJar
```

운영 target에 부하를 주는 실험은 허용된 host, VU/RPS/duration 상한, 실행자와 실행 시간, write 시나리오 여부, 전용 test data 사용 여부, 같은 시간 구간의 CPU/memory/GC/DB/Redis/HTTP metric 수집 여부를 리포트에 함께 남긴다.
새 성능 실험을 추가하면 실행 명령, 대상 환경, 원본 artifact 위치, Prometheus/Grafana 관측 시간을 이 README에 함께 갱신한다.

## 파일명

파일명은 날짜, 대상, 목적이 보이게 작성한다.

```text
YYYY-MM-DD-<target>-<scenario>.md
YYYY-MM-DD-<target>-<scenario>-summary.json
```

예시:

```text
2026-06-22-staging-smoke.md
2026-06-22-staging-baseline.md
2026-06-22-prod-read-only-smoke.md
```

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

초저지연 튜닝 후보는 HTTP latency만으로 판단하지 않는다. 앱 내부 metric으로 외부 source fetch, DB persist, LLM generation을 분리해 남긴다.

| Metric | 용도 |
|---|---|
| `external_source_fetch_seconds` | Naver 등 외부 source API 호출 시간 |
| `external_source_db_persist_seconds` | source 결과를 DB/vector store에 반영하는 시간 |
| `ai_text_generation_seconds` | OpenAI/vLLM 등 LLM 생성 호출 시간 |
| `ai_text_generation_prompt_tokens` | LLM input token 처리량 계산 |
| `ai_text_generation_completion_tokens` | LLM output TPS 계산 |

사용자 RPS와 LLM TPS 계산식은 [cost-capacity.md](../learning/cost-capacity.md)의 외부 API / LLM 계측 기준을 따른다.

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

1. k6 실행 후 자동 생성된 markdown 리포트를 확인한다.
2. 최신 focused report인 `n-plus-one-analysis.md`, `redis-cache-benchmark.md`, `k6-scenario-results.md` 중 가장 비슷한 문서 구조를 따른다.
3. k6 summary와 Grafana/Prometheus 값을 필요한 만큼 표에 옮긴다.
4. historical baseline인지 current capacity claim인지 명확히 표시한다.
5. 수치만 나열하지 말고 결론과 다음 조치를 적는다.
6. 민감정보가 없는지 확인한 뒤 Git에 포함한다.
