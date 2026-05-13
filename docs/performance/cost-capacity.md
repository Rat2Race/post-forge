# PostForge 비용 및 수용량 계산 가이드

작성일: 2026-05-12 KST

이 문서는 PostForge 백엔드의 요청량, 서버 부하, 클라우드 비용, 전기요금을 계산하는 기준 문서다. 숫자는 현재 저장된 k6/Grafana 리포트와 2026-05-12 기준 공식 가격 자료를 근거로 한다. 클라우드 단가와 전기요금은 바뀔 수 있으므로 실제 견적에는 최신 가격표와 청구서를 다시 확인한다.

## 용어

| 용어 | 의미 | PostForge에서의 해석 |
|---|---|---|
| RPS | Requests Per Second. HTTP 요청을 초당 몇 건 처리하는지 나타내는 값 | `/posts`, `/auth/login` 같은 API request 기준 |
| TPS | Transactions Per Second. 업무 트랜잭션을 초당 몇 건 처리하는지 나타내는 값 | 게시글 작성 1건이 로그인, 글 작성, 댓글 작성 등 여러 API를 부르면 TPS보다 RPS가 더 큼 |
| VU | Virtual User. k6 같은 부하 도구의 가상 사용자 | 실제 동시 사용자와 1:1로 같지 않음 |
| p95 | 요청 100개 중 95개가 이 시간 이하에 끝났다는 뜻 | 운영 응답시간 목표는 평균보다 p95를 우선 본다 |

관계식:

```text
RPS = TPS * 트랜잭션 1건당 API 요청 수
월 요청 수 = TPS * 2,592,000초
동시 처리 중 요청 수 ~= RPS * 평균 응답시간(초)
```

예시:

```text
1 TPS = 월 2,592,000 트랜잭션
1 TPS에서 트랜잭션 1건이 API 4개를 호출하면 4 RPS
25 RPS * p95 0.18초 = p95 기준 약 4.5개 요청이 동시에 처리 중
```

## 현재 검증된 성능 기준

근거 문서:

- `docs/performance/guest-split.md`
- `docs/performance/k6/K6-guest-batch-size-1773387011819.md`
- `docs/performance/load-analysis.md`
- `docs/performance/manual-runs/20260512-140100/run-summary.md`

환경:

| 항목 | 값 |
|---|---|
| 서버 | Oracle Cloud ARM 1vCPU / 1GB RAM |
| 앱 | Spring Boot 3.x |
| DB/Cache | PostgreSQL + Redis |
| 주요 병목 | 로그인 BCrypt CPU 사용량, 과거 `GET /posts` N+1 |

관측치:

| 시나리오 | 처리량 | CPU/Load | 응답시간 | 해석 |
|---|---:|---|---|---|
| 로그인까지 동일 비중으로 혼합 | 14.66 RPS | CPU 100%, Load 12.0 | 로그인 p95 3.52s | CPU 포화. 운영 불가에 가까움 |
| 읽기 25 VU + 로그인 5 VU | 23.75 RPS | CPU max 59%, Load 0.7 | 전체 안정 | 1GB VM에서 현실적 기준 |
| batch size 최적화 후 | 24.91 RPS | CPU 미관측 | 전체 avg 60.16ms, p95 180.75ms | API latency 기준 안정 |

운영 기준:

| 기준 | 권장 수치 |
|---|---:|
| 보수적 steady state | 20~25 RPS |
| CPU 80% 근처 이론 상한 | 약 32 RPS |
| 로그인 단독 상한 추정 | 약 4~5 RPS |
| 읽기 위주 단일 smoke 관측 | 77.26 RPS, 단 리소스 관측 없음 |

로그인은 BCrypt 때문에 다른 API보다 훨씬 비싸다. 로그인 비율이 높아지면 전체 수용량은 `GET /posts` 기준보다 크게 낮아진다.

## 서버 부하 계산법

CPU 부하를 요청당 비용으로 바꾸는 기본식:

```text
요청당 CPU-ms = CPU사용률 * vCPU수 / RPS * 1000
목표 CPU 사용률에서 가능한 RPS = 목표 CPU사용률 * vCPU수 / 요청당 CPU초
```

현재 검증값으로 계산:

```text
CPU max 59%, 1 vCPU, 23.75 RPS
요청당 평균 CPU 비용 ~= 0.59 / 23.75 * 1000 = 24.8 CPU-ms/request

CPU 60% 목표 수용량 ~= 0.60 / 0.0248 = 24.2 RPS
CPU 80% 목표 수용량 ~= 0.80 / 0.0248 = 32.2 RPS
```

메모리는 요청 수보다 JVM heap, PostgreSQL buffer, Redis, OS page cache의 영향을 더 크게 받는다. 1GB RAM에서 같은 VM에 app + PostgreSQL + Redis를 모두 올리면 여유가 크지 않다.

권장 메모리 예산:

| 구성요소 | 권장 상한 |
|---|---:|
| JVM `-Xmx` | 384~512MB |
| PostgreSQL shared/cache 예산 | 128~200MB |
| Redis | 32~64MB |
| OS/파일캐시/네이티브 메모리 | 150~250MB |

합계가 1GB에 가까워지므로 swap 사용 여부를 반드시 확인한다. 저장된 Grafana 자료에서는 heap 사용률 45~48%, GC pressure 0.1~0.4%, BLOCKED thread 0으로 기록되어 JVM 내부 병목은 크지 않았다.

## 클라우드 VM 비용 계산

현재 PostForge 운영 형태는 Docker Compose 기반 VM이다. VM 방식에서는 요청 1건마다 클라우드 비용이 추가되지 않는다. 월 비용은 VM, 디스크, 네트워크, 외부 서비스 비용의 합계다.

OCI Always Free A1 기준:

```text
무료 한도: 월 3,000 OCPU-hour + 18,000 GB-hour
1 OCPU / 1GB / 30일 사용량 = 720 OCPU-hour + 720 GB-hour
따라서 1 OCPU / 1GB는 Always Free 한도 안이면 compute 비용 $0
```

무료 한도가 없다고 가정한 A1 paid 계산식:

```text
월 VM 비용 =
OCPU수 * 720시간 * $0.01
+ 메모리GB * 720시간 * $0.0015
+ 유료 block volume/network/backup 비용
```

예시:

| VM | 월 compute 비용 |
|---|---:|
| OCI Always Free 안의 1 OCPU / 1GB | $0 |
| A1 paid 1 OCPU / 1GB, 무료 한도 없음 | $8.28 |
| A1 paid 4 OCPU / 24GB, 무료 한도 없음 | $54.72 |

월 요청 수별 VM 비용:

| TPS/RPS | 월 요청 수 | Always Free VM | A1 paid 1 OCPU/1GB | paid VM 요청 100만 건당 |
|---:|---:|---:|---:|---:|
| 1 | 2.59M | $0 | $8.28 | $3.194 |
| 5 | 12.96M | $0 | $8.28 | $0.639 |
| 10 | 25.92M | $0 | $8.28 | $0.319 |
| 25 | 64.80M | $0 | $8.28 | $0.128 |
| 50 | 129.60M | $0 | $8.28 | $0.064 |

VM은 고정비이므로 요청량이 늘수록 요청당 비용은 내려간다. 단, 서버가 버티는 한도 이상으로 요청을 넣으면 비용이 아니라 성능 장애가 먼저 발생한다.

## OCI Functions 비용 계산

Functions처럼 serverless로 옮기면 요청 수와 실행시간으로 비용이 붙는다.

공식 단가:

```text
월 2,000,000 invocation 무료
월 400,000 GB-second 무료
초과 invocation: $0.0000002 / invocation
초과 실행시간: $0.00001417 / GB-second
```

1GB 메모리 기준:

```text
월 비용 =
max(0, 요청수 - 2,000,000) * 0.0000002
+ max(0, 요청수 * 평균실행초 - 400,000) * 0.00001417
```

TPS별 예시:

| TPS | 월 요청 수 | Functions 100ms | Functions 200ms | Functions 500ms |
|---:|---:|---:|---:|---:|
| 1 | 2.59M | $0.12 | $1.80 | $12.81 |
| 5 | 12.96M | $14.89 | $33.25 | $88.35 |
| 10 | 25.92M | $35.84 | $72.57 | $182.76 |
| 25 | 64.80M | $98.71 | $190.54 | $466.00 |
| 50 | 129.60M | $203.50 | $387.14 | $938.07 |

PostForge처럼 Spring Boot 서버가 계속 떠 있고 PostgreSQL/Redis와 연결되는 구조는 Functions보다 VM이 단순하고 저렴하다. Functions는 짧고 드문 요청, 이벤트 처리, 비동기 작업에 더 적합하다.

## 전기세 계산

집이나 사무실에서 직접 서버를 돌리면 요청 수보다 서버 소비전력이 비용을 결정한다.

기본식:

```text
월 kWh = 평균소비전력(W) * 24시간 * 30일 / 1000
월 전기요금 증가분 ~= 월 kWh * 현재 청구서의 한계 단가(원/kWh)
```

한계 단가는 전체 사용량 구간에 따라 달라진다. 주택용은 누진 구간, 일반용/산업용은 계약전력, 계절, 시간대가 영향을 준다. 정확한 금액은 한전 계산기나 실제 고지서의 단가를 사용한다.

예시:

| 평균 소비전력 | 월 kWh | 150원/kWh | 200원/kWh | 300원/kWh |
|---:|---:|---:|---:|---:|
| 10W | 7.2kWh | 1,080원 | 1,440원 | 2,160원 |
| 15W | 10.8kWh | 1,620원 | 2,160원 | 3,240원 |
| 25W | 18.0kWh | 2,700원 | 3,600원 | 5,400원 |
| 40W | 28.8kWh | 4,320원 | 5,760원 | 8,640원 |
| 60W | 43.2kWh | 6,480원 | 8,640원 | 12,960원 |
| 100W | 72.0kWh | 10,800원 | 14,400원 | 21,600원 |

전기세는 거의 고정비다. 같은 서버가 1 TPS를 처리하든 10 TPS를 처리하든 평균 소비전력이 거의 같다면 월 전기세도 거의 같다. 고부하에서 CPU가 계속 올라가면 소비전력이 늘어날 수 있지만, 저전력 미니 PC나 ARM 보드에서는 증가폭이 VM 비용보다 작을 때가 많다.

## API별 비용 및 부하 추정

중요: VM에서는 API 호출마다 실제 청구 비용이 추가되지 않는다. 아래의 API별 비용은 월 VM 비용을 요청의 상대 부하로 나누어 본 내부 원가 배분값이다.

가정:

- 1GB 메모리 Functions로 옮겼을 때의 비용은 평균 응답시간을 실행시간으로 본 단순 계산이다.
- VM 내부 원가 배분은 A1 paid 1 OCPU/1GB `$8.28/month`, 25 RPS, 월 64.8M 요청을 기준으로 한다.
- API별 CPU-ms는 실제 profiler가 아니라 latency-weighted 추정이다. 정확한 API별 CPU 비용은 Java Flight Recorder, async-profiler, Micrometer endpoint tag, cAdvisor CPU metric을 붙여야 한다.

부하 테스트 포함 API:

| API | avg ms | p95 ms | 추정 CPU-ms/request | 60% CPU 단독 RPS | 80% CPU 단독 RPS | VM 배분 비용 / 1M req | Functions 비용 / 1M req |
|---|---:|---:|---:|---:|---:|---:|---:|
| `GET /posts` | 55.53 | 104.47 | 22.9 | 26.2 | 34.9 | $0.118 | $0.99 |
| `GET /posts?keyword` | 46.68 | 99.82 | 19.3 | 31.1 | 41.5 | $0.099 | $0.86 |
| `GET /posts/{id}` | 60.06 | 97.98 | 24.8 | 24.2 | 32.3 | $0.128 | $1.05 |
| `GET /posts/{id}/comments` | 37.65 | 92.53 | 15.5 | 38.6 | 51.5 | $0.080 | $0.73 |
| `POST /auth/login` | 359.52 | 864.34 | 148.5 | 4.0 | 5.4 | $0.764 | $5.29 |
| `POST /auth/token/reissue` | 42.11 | 83.91 | 17.4 | 34.5 | 46.0 | $0.089 | $0.80 |

Smoke-only API:

| API | avg ms | p95 ms | 추정 CPU-ms/request | 60% CPU 단독 RPS | 80% CPU 단독 RPS | VM 배분 비용 / 1M req | Functions 비용 / 1M req |
|---|---:|---:|---:|---:|---:|---:|---:|
| `POST /posts` | 20.59 | 42.24 | 8.5 | 70.6 | 94.1 | $0.044 | $0.49 |
| `PUT /posts/{id}` | 16.77 | 22.18 | 6.9 | 86.7 | 115.6 | $0.036 | $0.44 |
| `POST /posts/{id}/comments` | 10.13 | 13.69 | 4.2 | 143.5 | 191.3 | $0.022 | $0.34 |
| `PUT /posts/{id}/comments/{commentId}` | 7.79 | 10.59 | 3.2 | 186.5 | 248.6 | $0.017 | $0.31 |

해석:

- `POST /auth/login`은 가장 비싼 API다. BCrypt 때문에 읽기 API 대비 약 6배의 CPU 비용으로 추정된다.
- `GET /posts`는 과거 N+1 때문에 느렸지만 batch size 최적화 후 p95 약 104ms까지 내려갔다.
- 현재 1GB VM의 운영 병목은 메모리보다 CPU와 로그인 비율이다.

## 트래픽별 운영 판단

| 목표 트래픽 | 판단 |
|---:|---|
| 1~5 RPS | OCI Always Free VM으로 충분. 비용 $0 가능 |
| 10~20 RPS | 현재 구조로 가능. 로그인 비율과 DB 쿼리 확인 필요 |
| 25 RPS | 검증된 운영 기준 상단. 모니터링 필수 |
| 30~35 RPS | CPU 80% 근처. 장애 여유가 작음 |
| 50 RPS 이상 | 1 OCPU/1GB 단일 VM에서는 위험. A1 2 OCPU 이상 또는 DB 분리 권장 |
| 로그인 5 RPS 이상 | CPU 병목 가능성이 큼. rate limit, BCrypt cost 조정, 로그인 캐시/보호 정책 검토 |

## 새 테스트 후 다시 계산하는 방법

1. k6에서 `http_reqs`, `http_req_duration avg/p95`, endpoint별 duration을 기록한다.
2. Grafana나 cAdvisor에서 같은 구간의 app CPU 사용률과 memory 사용률을 기록한다.
3. 다음 산식을 적용한다.

```text
월 요청 수 = RPS * 2,592,000
요청당 CPU-ms = CPU사용률 * vCPU수 / RPS * 1000
목표 RPS = 목표CPU사용률 * vCPU수 / 요청당CPU초
VM 요청당 비용 = 월 VM 비용 / 월 요청 수
Functions 요청당 비용 = invocation 비용 + GB-second 비용
전기세 = 평균W * 720 / 1000 * 원/kWh
```

4. API별 정확한 비용이 필요하면 endpoint tag가 달린 metric 또는 profiler로 API별 CPU 시간을 직접 측정한다.

## 결론

현재 PostForge는 1 OCPU / 1GB RAM 기준으로 읽기 중심 20~25 RPS를 보수적인 운영 기준으로 잡는 것이 안전하다. OCI Always Free VM 안에서는 이 트래픽의 서버 compute 비용은 $0이다. paid VM으로 환산해도 1 OCPU / 1GB A1은 월 $8.28 수준이라 요청당 비용은 요청량이 많을수록 낮아진다.

반대로 Functions 방식은 100~200ms의 짧은 API만 있을 때는 저렴하지만, PostForge처럼 상시 실행 Spring Boot 앱과 DB 연결이 필요한 서버에는 VM 방식이 더 단순하고 비용 예측도 쉽다.

## 참고 자료

- Oracle Always Free Resources: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Oracle Arm-Based Compute pricing: https://www.oracle.com/cloud/compute/arm/
- Oracle Functions pricing: https://www.oracle.com/cloud/cloud-native/functions/
- KEPCO 전기요금표: https://home.kepco.co.kr/kepco/front/html/CY/E/E/CYEEHP00102.html
- KEPCO 전기요금 계산기: https://home.kepco.co.kr/kepco/front/html/CY/J/B/CYJBPP001_03.html
