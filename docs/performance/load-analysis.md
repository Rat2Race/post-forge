# Historical 부하 분석 메모

> 이 문서는 초기 조사 흐름을 보존하는 포인터다. 수치 비교는 [k6 시나리오 결과](./k6-scenario-results.md)를 우선한다.

과거 Oracle Cloud ARM 1vCPU / 1GB 환경에서 로그인과 읽기를 같은 비중으로 호출하자 BCrypt가 CPU를 포화시켰다. 로그인 비중을 별도 시나리오로 분리한 뒤에도 `GET /posts`가 느렸고, SQL 로그에서 요청당 추가 쿼리 11개를 확인했다.

이 조사는 두 병목을 순서대로 분리했다는 데 의미가 있다.

1. CPU 병목: workload mix에서 로그인 비중을 분리한다.
2. 조회 병목: CPU 여유 상태에서 SQL count와 fetch 전략을 확인한다.

후속 결과와 원본은 [N+1 분석](./n-plus-one-analysis.md), [historical 시나리오 해석](./guest-split.md), [`k6/`](./k6/)에 보존한다. 현재 prod capacity를 나타내지 않는다.
