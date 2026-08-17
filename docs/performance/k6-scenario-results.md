# k6 시나리오 결과

## 요약

초기 부하 테스트는 로그인과 읽기를 같은 비중으로 섞어 1vCPU 서버를 BCrypt로 포화시켰다.
읽기 25 VU와 로그인 5 VU로 분리하자 실제 읽기 병목을 더 정확히 볼 수 있었고, 이후 N+1과 batch size 개선 효과를 추적했다.

## 환경

| 항목 | 값 |
| --- | --- |
| 서버 | Oracle Cloud ARM 1vCPU / 1GB RAM |
| 애플리케이션 | Spring Boot 3.x |
| DB/Cache | PostgreSQL + Redis |
| 도구 | k6, Prometheus, Grafana |

## 시나리오 비교

| 지표 | 단일 시나리오 | 읽기 25 VU + 로그인 5 VU | 변화 |
| --- | ---: | ---: | ---: |
| CPU Usage max | 100% | 59% | 41%p 감소 |
| Load max | 12.0 | 0.7 | 17배 감소 |
| RPS | 14.66 | 23.75 | 1.6배 증가 |

과거 Grafana 메모의 `Duration MAX=182ms`는 panel metric 정의가 보존되지 않아 비교표에서 제외했다. raw k6의 `http_req_duration max`와 같은 지표로 해석하지 않는다.

| 엔드포인트 | 이전 p95 | 이후 p95 | 변화 |
| --- | ---: | ---: | ---: |
| `GET /posts` | 1.89s | 508ms | 3.7배 개선 |
| `GET /posts?keyword` | 1.58s | 290ms | 5.4배 개선 |
| `GET /posts/{id}` | 1.60s | 205ms | 7.8배 개선 |
| `GET /posts/{id}/comments` | 1.28s | 189ms | 6.8배 개선 |
| `POST /auth/login` | 3.52s | 1.38s | 2.5배 개선 |
| `POST /auth/token/reissue` | 694ms | 104ms | 6.7배 개선 |

## 후속 재검증

| 실행 | checks | http_reqs/RPS | p95 | 비고 |
| --- | ---: | ---: | ---: | --- |
| N+1 개선 | 6042/6042 | 4628 / 19.13 RPS | 285.04ms | 로그인 threshold와 expected success metric은 실패 |
| batch size 설정 | 7416/7416 | 6014 / 24.91 RPS | 180.75ms | CPU는 같은 방식으로 관측하지 않음 |
| 수동 public smoke | 21/21 | 아티팩트 요약 | 30.48ms | 로컬 smoke이며 수용량 benchmark는 아님 |
| 수동 auth smoke | 93/93 | 아티팩트 요약 | 요약에 기록 | 로컬 smoke이며 수용량 benchmark는 아님 |

## 숫자 읽는 법

- 로그인은 BCrypt 때문에 CPU-heavy하다. 로그인 비율이 높으면 읽기 API까지 queueing delay를 겪는다.
- 운영 수용량 판단에는 p95, RPS, CPU/Load를 함께 본다.
- k6 threshold 실패와 업무 성공 여부는 구분한다. 일부 과거 실행은 expected-result metric이 테스트 설정 때문에 실패로 보일 수 있다.

## 원본 아티팩트 보존 정책

과거 k6/Bruno/Grafana raw 산출물은 정량 근거이므로 `docs/performance/k6/`, `docs/performance/grafana/`, `docs/performance/manual-runs/`에 계속 보관한다.
이 문서는 원문 산출물을 빠르게 찾고 비교하기 위한 historical summary다.
새 capacity claim은 별도 수동/CI 성능 실험 리포트와 같은 시간대의 운영 지표를 함께 남긴다.
