### Batch size 설정 후 테스트 (인증 X)
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

     checks................................: 100.00% ✓ 7416      ✗ 0
     data_received.........................: 15 MB   62 kB/s
     data_sent.............................: 908 kB  3.8 kB/s
     group_duration........................: avg=79.55ms  min=11.18ms  med=25.78ms  max=7.28s   p(90)=134.74ms p(95)=214.46ms
     http_req_blocked......................: avg=157.43µs min=2.56µs   med=13.58µs  max=79.79ms p(90)=19.37µs  p(95)=20.91µs
     http_req_connecting...................: avg=48.58µs  min=0s       med=0s       max=43.81ms p(90)=0s       p(95)=0s
     http_req_duration.....................: avg=60.16ms  min=10.33ms  med=14.66ms  max=7.27s   p(90)=91.05ms  p(95)=180.75ms
       { expected_response:true }..........: avg=60.61ms  min=10.33ms  med=14.73ms  max=7.27s   p(90)=91.67ms  p(95)=183.29ms
     ✓ { name:GET /posts?keyword }.........: avg=46.68ms  min=11.44ms  med=14.74ms  max=5.21s   p(90)=80.27ms  p(95)=99.82ms
     ✓ { name:GET /posts }.................: avg=55.53ms  min=11.69ms  med=14.94ms  max=7.25s   p(90)=86.46ms  p(95)=104.47ms
     ✓ { name:GET /posts/{id} }............: avg=60.06ms  min=12.21ms  med=15.47ms  max=7.27s   p(90)=82.71ms  p(95)=97.98ms
     ✓ { name:GET /posts/{id}/comments }...: avg=37.65ms  min=10.33ms  med=12.26ms  max=3.18s   p(90)=85.38ms  p(95)=92.53ms
     ✓ { name:POST /auth/login }...........: avg=359.52ms min=161.46ms med=281.23ms max=3.42s   p(90)=583.58ms p(95)=864.34ms
     ✓ { name:POST /auth/token/reissue }...: avg=42.11ms  min=10.84ms  med=12.12ms  max=3.08s   p(90)=71.7ms   p(95)=83.91ms
     http_req_failed.......................: 2.76%   ✓ 166       ✗ 5848
     ✓ { expected_result:success }.........: 0.00%   ✓ 0         ✗ 5608
     http_req_receiving....................: avg=159.59µs min=33.15µs  med=146.5µs  max=2.66ms  p(90)=213.62µs p(95)=229.38µs
     http_req_sending......................: avg=45.88µs  min=8.62µs   med=40.69µs  max=1.3ms   p(90)=58.26µs  p(95)=77.82µs
     http_req_tls_handshaking..............: avg=83.51µs  min=0s       med=0s       max=28.36ms p(90)=0s       p(95)=0s
     http_req_waiting......................: avg=59.95ms  min=10.18ms  med=14.44ms  max=7.27s   p(90)=90.88ms  p(95)=180.59ms
     http_reqs.............................: 6014    24.906127/s
     iteration_duration....................: avg=3.35s    min=3.05s    med=3.12s    max=10.32s  p(90)=4.26s    p(95)=4.39s
     iterations............................: 1605    6.64688/s
     vus...................................: 2       min=0       max=30

running (4m01.5s), 00/30 VUs, 1605 complete and 0 interrupted iterations
logins  ✓ [======================================] 0/5 VUs    4m0s
readers ✓ [======================================] 00/25 VUs  4m0s
```
SSH, Docker logs 안켜서 CPU 사용률이 낮음