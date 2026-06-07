# PostForge k6

This directory is the source location for manually authored PostForge performance scripts.

- `postforge-benchmark.js`: shared runner for the current endpoint scenario registry.
- `api/`: endpoint scenario modules and future API-specific scripts.
- `lib/`: shared auth, env, fixture, and scenario helpers.

PostForge no longer runs k6 through a Spring Test Console. Run k6 directly from the CLI and use
Prometheus/Grafana for time-series storage, dashboards, and PNG exports.

## Manual Run

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

View-count Redis path smoke:

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

View-count SQL-only comparison uses the same k6 shape after restarting the app with SQL mode:

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

For authenticated scenarios, provide one of:

```bash
PERF_ACCESS_TOKEN=<access-token>
PERF_AUTH_HEADER="Bearer <access-token>"
PERF_USER_ID=<username> PERF_PASSWORD=<password>
```

## Prometheus / Grafana

k6 can send run metrics to a Prometheus remote-write endpoint:

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
k6 run -o experimental-prometheus-rw k6/postforge-benchmark.js
```

Use paired `RUN_GROUP` values across SQL-only and Redis-enabled runs so Grafana can compare the same
endpoint, load profile, and time window. `RUN_GROUP` is a k6/run-analysis value only; it must not be
added as a Spring application metric label.

Spring application metrics should be exposed through Actuator/Micrometer, while host/container CPU
and memory should come from the deployment's exporter stack such as node_exporter, cAdvisor, or a
managed cloud agent. The current v1 repo scope does not add Prometheus/Grafana/cAdvisor compose
files or Grafana dashboard JSON.

For `board.post.view-count`, inspect these app metrics after exercising the endpoint:

- `postforge_view_count_cache_requests_total`
- `postforge_view_count_db_loads_total`
- `postforge_view_count_operations_total`
- `postforge_view_count_operation_duration_seconds`

`SPRING_PROFILES_ACTIVE=perf-redis` enables the Redis cache, 24-hour view guard, and dirty-sync path.
`SPRING_PROFILES_ACTIVE=perf-sql` uses direct DB reads and atomic DB increments, without Redis cache,
Redis duplicate guard, or dirty-sync queue. Keep the same RPS, VUs, duration, endpoint, data set, and
auth setup when comparing the two profiles.
