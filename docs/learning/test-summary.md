# PostForge Historical Test Summary

작성일: 2026-05-12 KST

최신화: 2026-06-22 KST

> 이 문서는 과거 실행의 판정과 위치만 기록한다. 현재 코드의 최신 pass/fail 또는 현재 prod capacity를 증명하지 않는다.

## 날짜별 판정

| 날짜 | 실행 | 당시 판정 | 근거와 범위 |
|---|---|---|---|
| 2026-05-12 | local k6 public/auth smoke | PASS | public `21/21`, auth `93/93`; [실행 요약](../performance/manual-runs/20260512-140100/run-summary.md) |
| 2026-05-12 | local Bruno public/auth smoke | PASS | requests `4/4`, `9/9`; tests `9/9`, `21/21`; [실행 요약](../performance/manual-runs/20260512-140100/run-summary.md) |
| 2026-05-04 | generated prod `GET /posts` smoke | PASS, 제한적 | `1000/1000`, p95 311.45ms. commit과 리소스 지표 없음; [리포트](../performance/2026-05-04-160737-prod-smoke.md) |
| 2026-05-04 | generated prod smoke | INVALID | 요청 0건으로 성공 근거에서 제외; [리포트](../performance/2026-05-04-153452-prod-smoke.md) |
| historical | Oracle ARM 1vCPU / 1GB load | CAUTION | BCrypt CPU 포화와 `GET /posts` N+1을 분리한 과거 baseline; [비교 요약](../performance/k6-scenario-results.md) |

## 해석 경계

- local smoke는 단일 VU 기능 확인이며 수용량 benchmark가 아니다.
- generated prod smoke는 CPU, memory, image, commit이 없어 API 성공과 응답시간만 판단한다.
- 과거 1vCPU 수치는 현재 Intel N100 prod 상한으로 사용하지 않는다.
- raw k6, Grafana, Bruno, manual-run 산출물은 [성능 리포트 인덱스](../performance/README.md)의 archive 정책에 따라 보존한다.

조회 최적화의 단계별 수치는 [N+1 분석](../performance/n-plus-one-analysis.md), Redis A/B 결과는 [Redis 캐시 벤치마크](../performance/redis-cache-benchmark.md)를 본다.
