# Performance Reports

성능 테스트 결과를 Git에 남길 때는 원본 로그 전체보다 사람이 비교할 수 있는 요약 리포트를 우선한다.

## 저장 위치

| 종류 | 위치 | 비고 |
|---|---|---|
| 표준 리포트 | `docs/performance/YYYY-MM-DD-<scenario>.md` | 사람이 읽는 최종 기록 |
| k6 원문 요약 | `docs/performance/k6/` | 민감정보와 과도한 raw body는 제외 |
| Grafana 캡처 | `docs/performance/grafana/` | 중요한 대시보드 캡처만 보관 |
| 수동 실행 절차 | `docs/performance/manual-runbook.md` | 로컬/명시 대상 수동 성능 테스트 절차 |
| 지표 해석 가이드 | `docs/performance/metrics-guide.md` | k6/Grafana 주요 지표 해석 기준 |
| 작성 템플릿 | `docs/performance/report-template.md` | 새 리포트 작성 시 복사해서 사용 |
| 비용/수용량 계산 | `docs/performance/cost-capacity.md` | RPS/TPS, VM/Functions/전기세, API별 부하 추정 |

## 실행과 보관

현재 기준 성능 테스트는 Spring Test Console이나 별도 런처 없이 `k6/` 스크립트를 직접 실행한다.
시계열 지표와 대시보드는 Prometheus/Grafana에 맡기고, Git에는 사람이 비교할 수 있는 요약 리포트와
필요한 캡처만 남긴다.

기본 실행 예:

```bash
BASE_URL=http://127.0.0.1:8080 \
TARGET_ENDPOINT_KEY=board.post.list \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=50 \
K6_DURATION_SECONDS=60 \
API_VUS=20 \
RUN_GROUP=local-board-post-list-$(date +%Y%m%d%H%M%S) \
k6 run k6/postforge-benchmark.js
```

조회수 Redis 경로 측정 예:

```bash
SPRING_PROFILES_ACTIVE=perf-redis

BASE_URL=http://127.0.0.1:8080 \
TARGET_ENDPOINT_KEY=board.post.view-count \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=50 \
K6_DURATION_SECONDS=60 \
API_VUS=20 \
CACHE_STATE=REDIS_ENABLED \
RUN_GROUP=local-board-view-count-$(date +%Y%m%d%H%M%S) \
k6 run k6/postforge-benchmark.js
```

조회수 SQL-only 경로 측정 예:

```bash
SPRING_PROFILES_ACTIVE=perf-sql

BASE_URL=http://127.0.0.1:8080 \
TARGET_ENDPOINT_KEY=board.post.view-count \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=50 \
K6_DURATION_SECONDS=60 \
API_VUS=20 \
CACHE_STATE=SQL_ONLY \
RUN_GROUP=local-board-view-count-sql-$(date +%Y%m%d%H%M%S) \
k6 run k6/postforge-benchmark.js
```

Prometheus remote write를 쓰는 경우:

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
k6 run -o experimental-prometheus-rw k6/postforge-benchmark.js
```

## 파일명

파일명은 날짜, 대상, 목적이 보이게 작성한다.

```text
YYYY-MM-DD-<target>-<scenario>.md
YYYY-MM-DD-<target>-<scenario>-summary.json
```

예시:

```text
2026-05-04-staging-smoke.md
2026-05-04-staging-baseline.md
2026-05-04-prod-read-only-smoke.md
```

## 남기는 값

리포트에는 다음 값만 안정적으로 남긴다.

- 테스트 목적과 결론
- 대상 환경, image tag 또는 commit hash
- k6 script, executor, VUs, iterations, duration
- checks, request count, failure rate, RPS
- latency avg/med/p90/p95/p99/max
- 가능하면 CPU, memory, network, disk I/O 관측값
- Grafana screenshot, k6 summary JSON 같은 artifact 링크
- 다음 조치

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

1. k6 실행 조건과 `RUN_GROUP`을 기록한다.
2. Grafana에서 같은 시간대의 애플리케이션/리소스 지표를 확인하고 필요한 캡처를 저장한다.
3. `report-template.md`를 참고해 k6 결과와 Grafana 값을 필요한 만큼 표에 옮긴다.
4. 수치만 나열하지 말고 결론과 다음 조치를 적는다.
5. 민감정보가 없는지 확인한 뒤 Git에 포함한다.

`RUN_GROUP`은 k6 실행 묶음을 구분하기 위한 값이다. Spring app metric label에는 넣지 않는다.
`board.post.view-count`는 `SPRING_PROFILES_ACTIVE=perf-redis|perf-sql`로 실행 프로필을 바꾼 뒤
`postforge_view_count_*` metric과 함께 확인한다. Redis mode는 cache hit/miss와 DB fallback을,
SQL mode는 direct DB read/increment 비용을 같은 load profile에서 비교하기 위한 기준선이다.
