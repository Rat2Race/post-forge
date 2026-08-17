# Historical 로그인 시나리오 분리 해석

> 이 문서는 과거 Oracle Cloud ARM 1vCPU / 1GB 실험의 해석만 보존한다.
> 비교 수치의 정본은 [k6 시나리오 결과](./k6-scenario-results.md)이며 현재 prod capacity 근거가 아니다.

## 남길 해석

- 로그인과 읽기를 같은 비중으로 섞은 30 VU 실험은 BCrypt가 CPU를 포화시켜 읽기 병목을 가렸다.
- 읽기 25 VU와 로그인 5 VU로 분리하자 CPU 영향이 줄었고, `GET /posts`만 상대적으로 느려 추가 쿼리 11개의 N+1을 찾을 수 있었다.
- 두 실행은 요청 구성이 다르므로 RPS 차이를 코드 최적화 효과로 해석하지 않는다. 목적은 병목을 분리하는 것이었다.
- 당시 Grafana 메모에는 `Duration AVG / MAX = 172ms / 182ms`가 남았지만 정확한 panel metric 정의가 보존되지 않았다. 이 값은 raw k6의 `http_req_duration max`와 비교하거나 성능 주장에 사용하지 않는다.

## 근거

- 비교 요약: [k6 시나리오 결과](./k6-scenario-results.md)
- N+1 후속 분석: [N+1 분석](./n-plus-one-analysis.md)
- raw before: [`K6-guest-mixed-1773126246934.md`](./k6/K6-guest-mixed-1773126246934.md)
- raw after: [`K6-guest-split-1773127642624.md`](./k6/K6-guest-split-1773127642624.md)
- Grafana snapshot: [`JVM (Micrometer)-guest-split-1773127642624.png`](<./grafana/JVM (Micrometer)-guest-split-1773127642624.png>)
