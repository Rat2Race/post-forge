# Manual Performance Smoke Run

| 항목 | 값 |
|---|---|
| 실행 일시 | `2026-05-12T14:01:00+09:00` |
| 대상 | `local` |
| 대상 URL | `http://localhost:8080` |
| 목적 | manual k6/Bruno performance scripts smoke execution |
| 결론 | pass |

## 실행 결과

| 도구 | 시나리오 | 결과 |
|---|---|---|
| k6 | public smoke | checks `21/21`, `http_req_duration p95=30.48 ms` |
| k6 | auth smoke | checks `93/93`, `http_req_failed=0%` |
| Bruno | public smoke | requests `4/4`, tests `9/9` |
| Bruno | auth smoke | requests `9/9`, tests `21/21` |

## Artifacts

| 종류 | 경로 |
|---|---|
| k6 public markdown | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-public-smoke.md` |
| k6 public summary | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-public-smoke-summary.json` |
| k6 public log | `docs/performance/manual-runs/20260512-140100/k6-public-smoke.log` |
| k6 auth markdown | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-auth-smoke.md` |
| k6 auth summary | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-auth-smoke-summary.json` |
| k6 auth log | `docs/performance/manual-runs/20260512-140100/k6-auth-smoke.log` |
| Bruno public JSON | `docs/performance/manual-runs/20260512-140100/bruno-public-smoke.json` |
| Bruno public HTML | `docs/performance/manual-runs/20260512-140100/bruno-public-smoke.html` |
| Bruno public log | `docs/performance/manual-runs/20260512-140100/bruno-public-smoke.log` |
| Bruno auth JSON | `docs/performance/manual-runs/20260512-140100/bruno-auth-smoke.json` |
| Bruno auth HTML | `docs/performance/manual-runs/20260512-140100/bruno-auth-smoke.html` |
| Bruno auth log | `docs/performance/manual-runs/20260512-140100/bruno-auth-smoke.log` |

## Notes

- Local PostgreSQL and Redis were already running.
- The Spring Boot app was started with `bash ./gradlew :app:bootRun` because `gradlew` did not have executable permission.
- A temporary local-only user was inserted to execute auth flows and removed after the run.
- Bruno auth reporter output was written with request/response bodies omitted and sensitive headers skipped.
