
```

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: .\k6\post-read.js
        output: -

     scenarios: (100.00%) 3 scenarios, 150 max VUs, 1m20s max duration (incl. graceful stop):
              * warmup: 10 looping VUs for 10s (gracefulStop: 30s)
              * load: 50 looping VUs for 30s (startTime: 10s, gracefulStop: 30s)
              * spike: 100 looping VUs for 10s (startTime: 40s, gracefulStop: 30s)

INFO[0050]
========================================
  게시글 조회 API 부하 테스트 결과
========================================
  총 요청 수:     9918
  평균 응답 시간:  163.89 ms
  p90:            341.32 ms
  p95:            376.16 ms
  최대 응답 시간:  964.15 ms
  RPS:            198.36 req/s
  에러율:         0.00%
========================================  source=console

running (0m50.4s), 000/150 VUs, 9918 complete and 0 interrupted iterations
warmup ✓ [======================================] 10 VUs   10s
load   ✓ [======================================] 50 VUs   30s
spike  ✓ [======================================] 100 VUs  10s


         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: .\k6\post-list.js
        output: -

     scenarios: (100.00%) 3 scenarios, 150 max VUs, 1m20s max duration (incl. graceful stop):
              * warmup: 10 looping VUs for 10s (gracefulStop: 30s)
              * load: 50 looping VUs for 30s (startTime: 10s, gracefulStop: 30s)
              * spike: 100 looping VUs for 10s (startTime: 40s, gracefulStop: 30s)

INFO[0050]
========================================
  게시글 목록 API 부하 테스트 결과
========================================
  총 요청 수:     8548
  평균 응답 시간:  200.87 ms
  p50:            N/A ms
  p90:            480.66 ms
  p95:            512.94 ms
  p99:            N/A ms
  최대 응답 시간:  1198.40 ms
  RPS:            170.96 req/s
  에러율:         0.00%
========================================  source=console

running (0m50.6s), 000/150 VUs, 8548 complete and 0 interrupted iterations
warmup ✓ [======================================] 10 VUs   10s
load   ✓ [======================================] 50 VUs   30s
spike  ✓ [======================================] 100 VUs  10s
```