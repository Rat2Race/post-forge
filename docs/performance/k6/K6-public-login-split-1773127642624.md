### 로그인 시나리오 분리 후 테스트 결과 (인증 X)

```
         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/

     execution: local
        script: public_test.js
        output: -

     scenarios: (100.00%) 2 scenarios, 30 max VUs, 4m30s max duration (incl. graceful stop):
              * logins: Up to 5 looping VUs for 4m0s over 4 stages (gracefulRampDown: 30s, exec: loginScenario, gracefulStop: 30s)
              * readers: Up to 25 looping VUs for 4m0s over 4 stages (gracefulRampDown: 30s, exec: browseScenario, gracefulStop: 30s)

     █ 게시글 목록

       ✓ [GET /posts] 200 OK
       ✓ [GET /posts] has content

     █ 게시글 상세

       ✓ [GET /posts/{id}] 200 or 404
       ✓ [GET /comments] 200 OK

     █ 게시글 검색

       ✓ [GET /posts?keyword] 200 OK

     █ 로그인

       ✓ [POST /auth/login] 200 or 401

     █ 토큰 재발급

       ✓ [POST /token/reissue] responds

     checks................................: 100.00% ✓ 7118      ✗ 0
     data_received.........................: 14 MB   58 kB/s
     data_sent.............................: 821 kB  3.4 kB/s
     group_duration........................: avg=131.7ms  min=9.98ms   med=80.38ms  max=3s       p(90)=300.46ms p(95)=490.82ms
     http_req_blocked......................: avg=149.71µs min=2.79µs   med=13.59µs  max=72.93ms  p(90)=19.15µs  p(95)=20.68µs
     http_req_connecting...................: avg=42.92µs  min=0s       med=0s       max=9.61ms   p(90)=0s       p(95)=0s
     http_req_duration.....................: avg=100.15ms min=9.75ms   med=48.46ms  max=3s       p(90)=210.18ms p(95)=385.99ms
       { expected_response:true }..........: avg=84.47ms  min=10.5ms   med=40.76ms  max=1.58s    p(90)=195.81ms p(95)=294.34ms
     ✓ { name:GET /posts?keyword }.........: avg=78.01ms  min=11.42ms  med=31.27ms  max=1.08s    p(90)=192.53ms p(95)=290.58ms
     ✗ { name:GET /posts }.................: avg=142.1ms  min=17.22ms  med=90.44ms  max=1.58s    p(90)=308.88ms p(95)=508.26ms
     ✓ { name:GET /posts/{id} }............: avg=63.71ms  min=12.05ms  med=19.1ms   max=1.04s    p(90)=174.45ms p(95)=204.99ms
     ✓ { name:GET /posts/{id}/comments }...: avg=54.05ms  min=10.5ms   med=15.63ms  max=612.76ms p(90)=103.22ms p(95)=189.51ms
     ✗ { name:POST /auth/login }...........: avg=599.57ms min=136.34ms med=475.77ms max=3s       p(90)=1.2s     p(95)=1.38s
     ✓ { name:POST /auth/token/reissue }...: avg=35.95ms  min=9.75ms   med=11.2ms   max=296.79ms p(90)=89.16ms  p(95)=104.04ms
     http_req_failed.......................: 6.72%   ✓ 388       ✗ 5384
     ✓ { expected_result:success }.........: 0.00%   ✓ 0         ✗ 5384
     http_req_receiving....................: avg=163.67µs min=33.57µs  med=152.72µs max=942.5µs  p(90)=216.42µs p(95)=230µs
     http_req_sending......................: avg=45.02µs  min=7.4µs    med=40.95µs  max=421.79µs p(90)=57.25µs  p(95)=71.51µs
     http_req_tls_handshaking..............: avg=82.73µs  min=0s       med=0s       max=20.89ms  p(90)=0s       p(95)=0s
     http_req_waiting......................: avg=99.94ms  min=9.6ms    med=48.23ms  max=3s       p(90)=209.99ms p(95)=385.76ms
     http_reqs.............................: 5772    23.754825/s
     iteration_duration....................: avg=3.5s     min=3.06s    med=3.29s    max=7.1s     p(90)=4.3s     p(95)=4.62s
     iterations............................: 1540    6.337912/s
     vus...................................: 2       min=0       max=30

running (4m03.0s), 00/30 VUs, 1540 complete and 0 interrupted iterations
logins  ✓ [======================================] 0/5 VUs    4m0s
readers ✓ [======================================] 00/25 VUs  4m0s
```