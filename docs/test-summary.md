# PostForge 테스트 결과 종합

작성일: 2026-05-12 KST

## 종합 결론

현재 `docs/`에 남아 있는 유효한 테스트 결과 기준으로, 최신 로컬 smoke와 Docker 이미지 실행 검증은 통과했다. 성능 이력에서는 로그인 BCrypt 부하와 `GET /posts` 조회 쿼리 문제가 주요 병목으로 확인되었고, 시나리오 분리 및 조회 최적화 이후 읽기 API p95가 크게 개선되었다.

| 영역 | 최신 판정 | 핵심 근거 |
| --- | --- | --- |
| Docker 이미지 실행 smoke | PASS | `dockerfiles/versions/Dockerfile.v0`-`v4` 모두 jar layer, Java runtime, `/actuator/health` 통과 |
| Docker 빌드 성능 | PASS | v4 재빌드 평균 6.314s, v0 대비 84.3% 빠름 |
| 2026-05-12 로컬 k6 smoke | PASS | public `21/21`, auth `93/93`, 실패율 0.00% |
| 2026-05-12 로컬 Bruno smoke | PASS | public requests `4/4`, auth requests `9/9`, 전체 tests `30/30` |
| 2026-05-04 prod generated smoke | PASS | `1000/1000` checks, p95 311.45ms, 실패율 0.00% |
| 장기 부하 병목 분석 | CAUTION | 과거 단일 시나리오에서 CPU 100%, 로그인 p95 3.52s, `GET /posts` N+1 확인 |

## 최신 로컬 수동 smoke

출처: `docs/performance/manual-runs/20260512-140100/run-summary.md`

| 항목 | 값 |
| --- | --- |
| 실행 일시 | `2026-05-12T14:01:00+09:00` |
| 대상 | local |
| 대상 URL | `http://localhost:8080` |
| 목적 | manual k6/Bruno performance scripts smoke execution |
| 결론 | pass |

### k6 요약

| 시나리오 | checks | http_reqs | 실패율 | avg | med | p95 | max | 판정 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| public smoke | 21/21 | 15 | 0.00% | 13.13ms | 11.26ms | 30.48ms | 35.44ms | PASS |
| auth smoke | 93/93 | 72 | 0.00% | 16.59ms | 11.32ms | 23.35ms | 215.29ms | PASS |

### k6 엔드포인트별 p95

| 시나리오 | 엔드포인트 | p95 |
| --- | --- | ---: |
| public | `GET /posts` | 17.00ms |
| public | `GET /posts?keyword` | 33.31ms |
| auth | `GET /posts` | 16.49ms |
| auth | `GET /posts?keyword` | 13.01ms |
| auth | `POST /posts` | 42.25ms |
| auth | `PUT /posts/{id}` | 22.18ms |
| auth | `POST /posts/{id}/comments` | 13.69ms |
| auth | `PUT /posts/{id}/comments/{commentId}` | 10.59ms |

### Bruno 요약

| 시나리오 | requests | tests | 실패 | 판정 |
| --- | ---: | ---: | ---: | --- |
| public smoke | 4/4 | 9/9 | 0 | PASS |
| auth smoke | 9/9 | 21/21 | 0 | PASS |

Bruno public 요청은 `GET /posts`, `GET /posts?keyword`, `GET /posts/{id}`, `GET /posts/{id}/comments`를 확인했다. Bruno auth 요청은 로그인, 게시글 생성/수정/좋아요/삭제, 댓글 생성/수정/좋아요/삭제를 확인했다.

## Docker 이미지 검증과 빌드 성능

출처: `docs/docker/image-tests.md`, `docs/docker/build.md`

### 이미지 실행 smoke

| 버전 | 이미지 크기 | jar layer | Java runtime | `/actuator/health` | Health 대기 | 판정 |
| --- | ---: | --- | --- | --- | ---: | --- |
| v0 | 1.78GB | PASS | PASS | PASS | 5s | PASS |
| v1 | 668MB | PASS | PASS | PASS | 5s | PASS |
| v2 | 668MB | PASS | PASS | PASS | 4s | PASS |
| v3 | 668MB | PASS | PASS | PASS | 5s | PASS |
| v4 | 668MB | PASS | PASS | PASS | 5s | PASS |

검증은 임시 PostgreSQL/PgVector와 Redis 컨테이너를 붙인 상태에서 수행되었고, 기존 로컬 DB에 의존하지 않았다.

### 빌드 시간 재측정

| 버전 | 첫 빌드 평균 | 재빌드 평균 | 이미지 크기 | v0 대비 재빌드 |
| --- | ---: | ---: | ---: | ---: |
| v0 | 38.942s | 40.129s | 1.78GB | 기준 |
| v1 | 34.647s | 34.626s | 668MB | 13.7% 빠름 |
| v2 | 37.013s | 18.877s | 668MB | 53.0% 빠름 |
| v3 | 37.567s | 19.143s | 668MB | 52.3% 빠름 |
| v4 | 36.863s | 6.314s | 668MB | 84.3% 빠름 |

운영 후보 관점에서는 v4가 이미지 크기를 668MB로 유지하면서 재빌드 시간을 가장 크게 줄였다.

## Generated prod smoke

출처: `docs/performance/2026-05-04-160737-prod-smoke.md`

| 항목 | 값 |
| --- | ---: |
| 테스트 ID | `2026-05-04-160737-prod-smoke` |
| 대상 환경 | prod |
| VUs | 100 |
| iterations | 1000 |
| duration | 12.94s |
| checks | 1000/1000 |
| http_reqs | 1000 |
| requests/sec | 77.26 |
| 실패율 | 0.00% |
| avg | 128.42ms |
| med | 105.73ms |
| p95 | 311.45ms |
| max | 503.39ms |

엔드포인트는 `GET /posts` 단일 smoke로 기록되어 있다. 같은 날짜의 `2026-05-04-153452-prod-smoke`는 `checks 0/0`, `http_reqs 0`이므로 통합 결론의 성공 근거에서는 제외했다.

## 부하 테스트 병목 이력

출처: `docs/performance/guest-split.md`, `docs/performance/load-analysis.md`

### 단일 시나리오 병목

30 VU가 읽기 API와 로그인을 같은 비중으로 반복했을 때 BCrypt 해싱이 1vCPU 환경을 포화시켰다.

| 지표 | 결과 |
| --- | --- |
| CPU Usage max | 100% |
| Load max | 12.0 |
| RPS | 14.66 |
| Duration max | 3.51s |
| `POST /auth/login` p95 | 3.52s |
| `GET /posts` p95 | 1.89s |

### 시나리오 분리 후

읽기 25 VU, 로그인 5 VU로 비율을 분리하자 CPU 병목이 크게 줄었다.

| 지표 | before | after | 변화 |
| --- | ---: | ---: | --- |
| CPU Usage max | 100% | 59% | 41%p 감소 |
| Load max | 12.0 | 0.7 | 17배 감소 |
| RPS | 14.66 | 23.75 | 1.6배 증가 |
| Duration max | 3.51s | 182ms | 19배 개선 |

| 엔드포인트 | before p95 | after p95 | 변화 |
| --- | ---: | ---: | --- |
| `GET /posts` | 1.89s | 508ms | 3.7배 개선 |
| `GET /posts?keyword` | 1.58s | 290ms | 5.4배 개선 |
| `GET /posts/{id}` | 1.60s | 205ms | 7.8배 개선 |
| `GET /posts/{id}/comments` | 1.28s | 189ms | 6.8배 개선 |
| `POST /auth/login` | 3.52s | 1.38s | 2.5배 개선 |
| `POST /auth/token/reissue` | 694ms | 104ms | 6.7배 개선 |

시나리오 분리 후에도 `GET /posts`가 다른 읽기 API보다 느렸고, 디버그 로그 분석에서 요청 1건당 추가 쿼리 11개가 발생하는 N+1 문제가 확인되었다.

## 조회 최적화 재검증 이력

출처: `docs/performance/k6/K6-guest-n1-fix-1773280400770.md`, `docs/performance/k6/K6-guest-batch-size-1773387011819.md`

| 단계 | checks | 전체 p95 | `GET /posts` p95 | `GET /posts/{id}` p95 | `POST /auth/login` p95 | 비고 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| N+1 해결 후 | 6042/6042 | 285.04ms | 195.83ms | 191.04ms | 1.40s | 로그인 threshold와 expected success 지표는 실패 |
| batch size 설정 후 | 7416/7416 | 180.75ms | 104.47ms | 97.98ms | 864.34ms | 문서에 CPU 사용률 관측 조건 차이가 기록됨 |

batch size 설정 후 읽기 API p95는 100ms 안팎으로 내려갔고, 로그인 p95도 1초 미만으로 낮아졌다.

## 남은 리스크와 해석 제한

- `docs/`에는 최신 Gradle unit/integration test 전체 실행 결과가 없다. 이 문서는 문서화된 smoke/성능/이미지 검증 결과만 종합한다.
- 일부 과거 k6 결과는 원문 콘솔 로그 형태로 저장되어 있어 threshold 설정과 실제 업무 판정을 별도로 해석해야 한다.
- prod generated smoke는 URL이 masked이고 CPU/memory 관측값이 없어, API 성공과 응답시간만 판단할 수 있다.
- `2026-05-04-153452-prod-smoke`는 pass로 적혀 있지만 요청 수가 0이므로 성공 증거로 사용하지 않는다.

## 원본 아티팩트

| 종류 | 경로 |
| --- | --- |
| 최신 실행 요약 | `docs/performance/manual-runs/20260512-140100/run-summary.md` |
| k6 public smoke | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-public-smoke.md` |
| k6 public summary | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-public-smoke-summary.json` |
| k6 auth smoke | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-auth-smoke.md` |
| k6 auth summary | `docs/performance/manual-runs/20260512-140100/20260512-140100-k6-auth-smoke-summary.json` |
| Bruno public JSON/HTML/log | `docs/performance/manual-runs/20260512-140100/bruno-public-smoke.*` |
| Bruno auth JSON/HTML/log | `docs/performance/manual-runs/20260512-140100/bruno-auth-smoke.*` |
| Docker 이미지 검증 | `docs/docker/image-tests.md` |
| Docker 빌드 비교 | `docs/docker/build.md` |
| prod generated smoke | `docs/performance/2026-05-04-160737-prod-smoke.md` |
| 부하 병목 분석 | `docs/performance/guest-split.md` |
| 병목 분석 보고서 | `docs/performance/load-analysis.md` |
| N+1 개선 k6 | `docs/performance/k6/K6-guest-n1-fix-1773280400770.md` |
| batch size k6 | `docs/performance/k6/K6-guest-batch-size-1773387011819.md` |
