# <YYYY-MM-DD> <target> <scenario> 성능 테스트 리포트

## 요약

| 항목 | 값 |
|---|---|
| 결론 | pass / caution / fail |
| 목적 | 예: staging smoke, prod read-only baseline |
| 주요 변화 | 예: p95 개선, error rate 증가, memory 증가 없음 |
| 다음 조치 | 예: 유지 / 재측정 / 병목 분석 / 롤백 검토 |

## 실행 정보

| 항목 | 값 |
|---|---|
| 테스트 ID | `<target>-<scenario>-<YYYYMMDD-HHMM>` |
| 실행 일시 | `<YYYY-MM-DD HH:mm KST>` |
| 실행 위치 | local / WSL / CI / perf-runner |
| 대상 환경 | local / staging / prod |
| 대상 URL | local / staging / prod 등으로 마스킹 |
| 앱 image tag | `<image-tag>` |
| 앱 commit | `<commit-hash>` |
| k6 version | `<k6 version>` |
| script | `tests/k6/generated/smoke.js` |
| 실행 명령 | `BASE_URL=<masked> k6 run ...` |

## 부하 조건

| 항목 | 값 |
|---|---|
| executor | shared-iterations / per-vu-iterations / constant-vus / ramping-vus |
| VUs | `<number>` |
| iterations | `<number>` |
| duration | `<duration>` |
| scenario | smoke / baseline / load / stress |
| data state | cold cache / warm cache / seeded DB / prod data |
| warm-up | yes / no |

## k6 결과

| 지표 | 값 |
|---|---:|
| checks | `<passed>/<total>` |
| http_reqs | `<count>` |
| requests/sec | `<rps>` |
| http_req_failed | `<rate>` |
| http_req_duration avg | `<ms>` |
| http_req_duration med | `<ms>` |
| http_req_duration p90 | `<ms>` |
| http_req_duration p95 | `<ms>` |
| http_req_duration p99 | `<ms>` |
| http_req_duration max | `<ms>` |

## Endpoint별 결과

| Endpoint | Count | Fail Rate | Avg | p95 | p99 | Max | 비고 |
|---|---:|---:|---:|---:|---:|---:|---|
| `GET /example` |  |  |  |  |  |  |  |

## 리소스 관측

코드 계측이 없으면 Docker/cAdvisor/node exporter/Grafana에서 확인 가능한 값만 기록한다.

| 지표 | Avg | Max | 비고 |
|---|---:|---:|---|
| app container CPU |  |  |  |
| app container memory |  |  |  |
| host CPU |  |  |  |
| host memory |  |  |  |
| network RX/TX |  |  |  |
| disk I/O |  |  |  |

## 관찰 및 해석

- 관찰:
- 병목 후보:
- 이전 측정 대비:
- 신뢰도:

## Artifact

| 종류 | 경로 |
|---|---|
| k6 summary JSON | `docs/performance/k6/<file>.json` |
| k6 console summary | `docs/performance/k6/<file>.md` |
| Grafana screenshot | `docs/performance/grafana/<file>.png` |
| 관련 PR/commit | `<link-or-hash>` |

## 민감정보 점검

- [ ] token/cookie/header가 없다.
- [ ] 요청/응답 body 원문이 없다.
- [ ] 운영 사용자 식별자가 없다.
- [ ] 내부 IP/domain이 필요 이상으로 노출되지 않는다.
- [ ] raw artifact 크기가 과도하지 않다.

## 결론

짧게 적는다.

```text
예: staging smoke 기준 error rate 0%, p95 120ms로 통과. CPU max 42%, memory 증가 추세 없음. 다음 baseline 테스트에서 VU 10으로 재측정한다.
```
