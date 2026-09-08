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
| k6 public markdown | `docs/performance/runs/20260512-140100-smoke/20260512-140100-k6-public-smoke.md` |
| k6 public summary | `docs/performance/runs/20260512-140100-smoke/20260512-140100-k6-public-smoke-summary.json` |
| k6 auth markdown | `docs/performance/runs/20260512-140100-smoke/20260512-140100-k6-auth-smoke.md` |
| k6 auth summary | `docs/performance/runs/20260512-140100-smoke/20260512-140100-k6-auth-smoke-summary.json` |
| Bruno public JSON | `docs/performance/runs/20260512-140100-smoke/bruno-public-smoke.json` |
| Bruno auth JSON | `docs/performance/runs/20260512-140100-smoke/bruno-auth-smoke.json` |

Bruno HTML의 실행 결과는 위 JSON과 동일하며, 화면 표시용 `isHtml` 필드만 추가되어 있어 중복 HTML은 정리했다. HTML에만 있던 실행 메타데이터는 아래에 보존한다.

| 항목 | 값 |
|---|---|
| Bruno version | `usebruno v3.3.0` |
| Environment | `local.example` |
| Public report completed | `2026-05-12T05:01:30.401Z` |
| Auth report completed | `2026-05-12T05:01:43.927Z` |

## Notes

- Local PostgreSQL and Redis were already running.
- The Spring Boot app was started with `bash ./gradlew :app:bootRun` because `gradlew` did not have executable permission.
- A temporary local-only user was inserted to execute auth flows and removed after the run.
- Bruno auth reporter output was written with request/response bodies omitted and sensitive headers skipped.
