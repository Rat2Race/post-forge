
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
  총 요청 수:     20347
  평균 응답 시간:  26.08 ms
  p90:            50.02 ms
  p95:            63.68 ms
  최대 응답 시간:  894.69 ms
  RPS:            406.94 req/s
  에러율:         0.00%
========================================  source=console

running (0m50.1s), 000/150 VUs, 20347 complete and 0 interrupted iterations
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
  총 요청 수:     5987
  평균 응답 시간:  341.07 ms
  p50:            N/A ms
  p90:            785.08 ms
  p95:            820.93 ms
  p99:            N/A ms
  최대 응답 시간:  1783.50 ms
  RPS:            119.74 req/s
  에러율:         0.00%
========================================  source=console

running (0m50.8s), 000/150 VUs, 5987 complete and 0 interrupted iterations
warmup ✓ [======================================] 10 VUs   10s
load   ✓ [======================================] 50 VUs   30s
spike  ✓ [======================================] 100 VUs  10s
```