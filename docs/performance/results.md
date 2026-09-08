# 성능 분석 결과

이 문서는 과거 게시판·인증 API의 성능 분석 요약이다. 주요 부하 비교는 Oracle ARM 1vCPU/1GB 실행이며 로컬 smoke는 별도 표시한다. 현재 뉴스 수집·LLM 가공·자동 게시·데일리 종합의 처리 성능이나 운영 수용량을 입증하지 않는다.
전체 실행 목록과 환경별 범위는 [성능 리포트 안내](./README.md), 원본은 `runs/<run>/`에서 확인한다.

## k6 시나리오 결과

### 요약

초기 부하 테스트는 로그인과 읽기를 같은 비중으로 섞어 1vCPU 서버를 BCrypt로 포화시켰다.
읽기 25 VU와 로그인 5 VU로 분리하자 실제 읽기 병목을 더 정확히 볼 수 있었고, 이후 N+1과 batch size 개선 효과를 추적했다.

### 환경

| 항목 | 값 |
| --- | --- |
| 서버 | Oracle Cloud ARM 1vCPU / 1GB RAM |
| 애플리케이션 | Spring Boot 3.x |
| DB/Cache | PostgreSQL + Redis |
| 도구 | k6, Prometheus, Grafana |

### 시나리오 비교

| 지표 | 단일 시나리오 | 읽기 25 VU + 로그인 5 VU | 변화 |
| --- | ---: | ---: | ---: |
| CPU Usage max | 100% | 59% | 41%p 감소 |
| Load max | 12.0 | 0.7 | 17배 감소 |
| RPS | 14.66 | 23.75 | 1.6배 증가 |

과거 Grafana 메모의 `Duration MAX=182ms`는 panel metric 정의가 보존되지 않아 비교표에서 제외했다. raw k6의 `http_req_duration max`와 같은 지표로 해석하지 않는다.
두 실행은 요청 구성이 다르므로 RPS 차이를 코드 최적화 효과로 해석하지 않는다. 목적은 병목 분리였다.
원본: [`runs/20260310-160406-guest-mixed/`](./runs/20260310-160406-guest-mixed/), [`runs/20260310-162722-guest-split/`](./runs/20260310-162722-guest-split/)

| 엔드포인트 | 이전 p95 | 이후 p95 | 변화 |
| --- | ---: | ---: | ---: |
| `GET /posts` | 1.89s | 508ms | 3.7배 개선 |
| `GET /posts?keyword` | 1.58s | 290ms | 5.4배 개선 |
| `GET /posts/{id}` | 1.60s | 205ms | 7.8배 개선 |
| `GET /posts/{id}/comments` | 1.28s | 189ms | 6.8배 개선 |
| `POST /auth/login` | 3.52s | 1.38s | 2.5배 개선 |
| `POST /auth/token/reissue` | 694ms | 104ms | 6.7배 개선 |

### 후속 재검증

| 실행 | checks | http_reqs/RPS | p95 | 비고 |
| --- | ---: | ---: | ---: | --- |
| N+1 개선 | 6042/6042 | 4628 / 19.13 RPS | 285.04ms | 로그인 threshold와 expected success metric은 실패 |
| batch size 설정 | 7416/7416 | 6014 / 24.91 RPS | 180.75ms | CPU는 같은 방식으로 관측하지 않음 |
| 수동 public smoke | 21/21 | 아티팩트 요약 | 30.48ms | 로컬 smoke이며 수용량 benchmark는 아님 |
| 수동 auth smoke | 93/93 | 아티팩트 요약 | 요약에 기록 | 로컬 smoke이며 수용량 benchmark는 아님 |

원본: [`runs/20260312-105320-guest-n1-fix/`](./runs/20260312-105320-guest-n1-fix/), [`runs/20260313-163011-guest-batch-size/`](./runs/20260313-163011-guest-batch-size/), [`runs/20260512-140100-smoke/`](./runs/20260512-140100-smoke/)

### 숫자 읽는 법

- 로그인은 BCrypt 때문에 CPU-heavy하다. 로그인 비율이 높으면 읽기 API까지 queueing delay를 겪는다.
- k6 threshold 실패와 업무 성공 여부는 구분한다. 일부 과거 실행은 expected-result metric이 테스트 설정 때문에 실패로 보일 수 있다.

## N+1 분석

### 요약

`GET /posts`는 로그인 BCrypt CPU 병목을 제거한 뒤에도 다른 읽기 API보다 느렸다.
디버그 SQL 로그에서 요청 1건당 추가 쿼리 11개가 확인되어 N+1 병목으로 분리했다.

### 기준 신호

| 시나리오 | `GET /posts` med | `GET /posts` p95 | 시스템 신호 |
| --- | ---: | ---: | --- |
| 로그인 혼합 30 VU | 498ms | 1.89s | CPU 100%, Load 12.0 |
| 읽기 25 VU + 로그인 5 VU | 90ms | 508ms | CPU max 59%, Load 0.7 |

시나리오를 분리하자 CPU 포화는 해소되었지만, `GET /posts`는 `GET /posts/{id}`나 `GET /posts/{id}/comments`보다 계속 느렸다.
CPU가 여유 있는 상태에서 특정 조회 API만 느리면 DB 쿼리 수와 fetch 전략을 우선 확인한다.

### 근거

- `org.hibernate.SQL: DEBUG` 로그에서 `GET /posts` 요청 1건당 추가 쿼리 11개가 관측되었다.
- 동일 시나리오에서 단건 조회와 댓글 조회는 p95 189-205ms까지 내려갔지만 목록 조회는 p95 508ms였다.
- N+1 개선 후 `GET /posts` p95는 195.83ms로 내려갔다.
- batch size 설정 후 `GET /posts` p95는 104.47ms로 추가 개선되었다.

### 재검증 결과

| 단계 | checks | 전체 p95 | `GET /posts` p95 | `GET /posts/{id}` p95 | 로그인 p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| N+1 해결 후 | 6042/6042 | 285.04ms | 195.83ms | 191.04ms | 1.40s |
| batch size 설정 후 | 7416/7416 | 180.75ms | 104.47ms | 97.98ms | 864.34ms |

`batch size 설정`은 `hibernate.default_batch_fetch_size: 1000`(`app/src/main/resources/application.yml`, `application-prod.yml`)이며, 재발 방지는 `board/src/test/java/dev/iamrat/board/integration/BoardNPlusOneRegressionTest.java`가 강제한다(게시글 목록·댓글 목록 조회의 Hibernate prepared statement 수가 page size 1→20에서 +2를 넘지 않아야 함).

## Redis 캐시 벤치마크

### 요약

게시글 상세 조회는 Redis 조회수 버퍼 적용 후 응답시간과 처리량이 크게 개선되었다.
게시글 목록은 같은 실험에서 악화되어, 목록 조회는 조회수 캐시와 별도로 쿼리/fetch 전략을 관리해야 한다.

### 시나리오

| 항목 | 값 |
| --- | --- |
| 스크립트 | `post-read.js`, `post-list.js` |
| 패턴 | warmup 10 VU 10s, load 50 VU 30s, spike 100 VU 10s |
| 최대 VU | 150 |
| 에러율 | 이전/이후 모두 0.00% |

### 결과

| API | 지표 | Before | After | 변화 |
| --- | --- | ---: | ---: | ---: |
| 게시글 상세 조회 | 총 요청 수 | 9,918 | 20,347 | 2.05배 |
| 게시글 상세 조회 | 평균 | 163.89ms | 26.08ms | 84.1% 감소 |
| 게시글 상세 조회 | p95 | 376.16ms | 63.68ms | 83.1% 감소 |
| 게시글 상세 조회 | RPS | 198.36 | 406.94 | 2.05배 |
| 게시글 목록 | 총 요청 수 | 8,548 | 5,987 | 30.0% 감소 |
| 게시글 목록 | 평균 | 200.87ms | 341.07ms | 69.8% 증가 |
| 게시글 목록 | p95 | 512.94ms | 820.93ms | 60.0% 증가 |
| 게시글 목록 | RPS | 170.96 | 119.74 | 30.0% 감소 |

원본: [2026-04-09 before/after](./runs/20260409-post-cache/)

### 해석

- Redis 조회수 버퍼는 조회수 update가 자주 발생하는 상세 조회에 효과가 있다.
- 목록 조회는 같은 이점을 얻지 못하며, 파생 데이터를 더 읽거나 리소스를 경쟁하면 오히려 나빠질 수 있다.
- key ownership과 sync 의미는 [Redis 캐시 전략](../architecture/redis-cache-strategy.md)을 따른다.
- 목록 조회 진단은 [N+1 분석](#n1-분석)을 함께 본다.

### 후속 점검

- 같은 commit에서 목록/상세 endpoint의 SQL count를 비교한다.
- k6 latency만 보지 말고 캐시 벤치마크 실행 시 CPU/Load도 같이 남긴다.
- 게시글 목록 최적화는 조회수 버퍼링 결정과 분리해서 판단한다.
