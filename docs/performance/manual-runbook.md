# Manual Performance Runbook

수동 성능 테스트는 로컬 또는 명시한 대상 서버에만 실행한다. 운영 환경 고부하는 별도 승인 없이 실행하지 않는다.

## 기본값

| 값 | 기본 |
|---|---|
| 대상 URL | `http://localhost:8080` |
| k6 script | `k6/postforge-benchmark.js` |
| 리포트 위치 | `docs/performance/manual-runs/<run-id>/` |

## 리포트 디렉터리

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="docs/performance/manual-runs/$RUN_ID"
mkdir -p "$REPORT_DIR"
```

## k6 공개 읽기 부하

```bash
BASE_URL=http://localhost:8080 \
TARGET_ENDPOINT_KEY=board.post.list \
PERF_AUTH_MODE=NONE \
K6_TARGET_RPS=25 \
K6_DURATION_SECONDS=60 \
API_VUS=10 \
RUN_GROUP="$RUN_ID-board-post-list" \
k6 run k6/postforge-benchmark.js 2>&1 | tee "$REPORT_DIR/k6-public.log"
```

남는 파일:

- `$REPORT_DIR/k6-public.log`
- 필요 시 Grafana PNG 캡처

## k6 조회수 Redis 경로 부하

앱을 Redis mode로 실행한 뒤 같은 부하 조건을 기록한다.

```bash
SPRING_PROFILES_ACTIVE=perf-redis

BASE_URL=http://localhost:8080 \
TARGET_ENDPOINT_KEY=board.post.view-count \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=25 \
K6_DURATION_SECONDS=60 \
API_VUS=10 \
CACHE_STATE=REDIS_ENABLED \
RUN_GROUP="$RUN_ID-board-view-count" \
k6 run k6/postforge-benchmark.js 2>&1 | tee "$REPORT_DIR/k6-view-count.log"
```

확인할 app metric:

- `postforge_view_count_cache_requests_total`
- `postforge_view_count_db_loads_total`
- `postforge_view_count_operations_total`
- `postforge_view_count_operation_duration_seconds`

`RUN_GROUP`은 k6/Grafana에서 테스트 묶음을 구분할 때만 사용한다. Spring app metric label에는
넣지 않는다.

남는 파일:

- `$REPORT_DIR/k6-view-count.log`
- 필요 시 Grafana PNG 캡처

## k6 조회수 SQL-only 경로 부하

앱을 SQL mode로 재실행한 뒤 Redis 측정과 같은 RPS, VUs, duration, data set, auth 조건을 사용한다.
이 mode는 Redis cache, 24시간 view guard, dirty-sync queue 없이 DB 직접 read와 atomic increment를
사용한다.

```bash
SPRING_PROFILES_ACTIVE=perf-sql

BASE_URL=http://localhost:8080 \
TARGET_ENDPOINT_KEY=board.post.view-count \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=25 \
K6_DURATION_SECONDS=60 \
API_VUS=10 \
CACHE_STATE=SQL_ONLY \
RUN_GROUP="$RUN_ID-board-view-count-sql" \
k6 run k6/postforge-benchmark.js 2>&1 | tee "$REPORT_DIR/k6-view-count-sql.log"
```

비교할 app metric:

- `postforge_view_count_db_loads_total{cache_state="sql_only"}`
- `postforge_view_count_operations_total{cache_state="sql_only"}`
- `postforge_view_count_operation_duration_seconds{cache_state="sql_only"}`

남는 파일:

- `$REPORT_DIR/k6-view-count-sql.log`
- 필요 시 Grafana PNG 캡처

## k6 인증 쓰기 부하

인증이 필요한 endpoint는 토큰 또는 로컬 전용 계정을 사용한다. 비밀번호는 커맨드 히스토리에 남지 않게 주의한다.

```bash
read -r -s PERF_PASSWORD

BASE_URL=http://localhost:8080 \
TARGET_ENDPOINT_KEY=board.post.create \
PERF_AUTH_MODE=AUTO \
PERF_USERNAME=testuser1 \
PERF_PASSWORD="$PERF_PASSWORD" \
K6_TARGET_RPS=5 \
K6_DURATION_SECONDS=30 \
API_VUS=3 \
RUN_GROUP="$RUN_ID-board-post-create" \
k6 run k6/postforge-benchmark.js 2>&1 | tee "$REPORT_DIR/k6-auth.log"
```

남는 파일:

- `$REPORT_DIR/k6-auth.log`
- 필요 시 Grafana PNG 캡처

## Prometheus remote write

k6 결과를 Prometheus에도 남길 때는 같은 `RUN_GROUP`을 유지한다.

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
k6 run -o experimental-prometheus-rw k6/postforge-benchmark.js
```

## 빠른 로컬 검증

서버와 리포트 경로만 빠르게 확인할 때는 부하를 낮춘다.

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="docs/performance/manual-runs/$RUN_ID"
mkdir -p "$REPORT_DIR"

BASE_URL=http://localhost:8080 \
TARGET_ENDPOINT_KEY=board.post.view-count \
PERF_AUTH_MODE=AUTO \
K6_TARGET_RPS=1 \
K6_DURATION_SECONDS=5 \
API_VUS=1 \
CACHE_STATE=REDIS_ENABLED \
RUN_GROUP="$RUN_ID-board-view-count-smoke" \
k6 run k6/postforge-benchmark.js 2>&1 | tee "$REPORT_DIR/k6-public-smoke.log"
```
