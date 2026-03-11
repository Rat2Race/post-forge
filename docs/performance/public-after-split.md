### 시나리오 하나로 했을 때

로그인까지 동일 비중으로 요청을 보내서 CPU 병목 발생
→ 로그인 과정에서 BCrypt(strength 10 = 2^10 = 1,024번 반복 해싱)가 CPU를 독점

근거
1. [Grafana] CPU Usage MAX 100%, Load MAX 12.0
   → 1vCPU에 대기 작업이 12개 쌓임
2. [Grafana] Rate가 부하 초반에 올라갔다가 정체/하락
   → CPU 포화로 더 이상 처리량을 늘릴 수 없음
3. [Grafana] Duration AVG 170ms vs MAX 3.51s (20배 차이)
   → CPU 줄 서기로 인해 뒤의 요청일수록 대기 시간 증가
4. [k6] 로그인(med 1.98s)이 다른 API(med 198~498ms)보다 4~10배 느림
   → BCrypt 해싱이 CPU를 점유하는 것이 병목의 원인

### 시나리오 두개로 했을 때

로그인 부분을 다른 시나리오에 다른 비중으로 요청을 설정하여 테스트 함
-> 로그인 요청이 줄어들어 CPU 병목이 심해지지 않음

med=90.44ms, max=1.58s, p(90)=308.88ms, p(95)=508.26ms
-> 하지만 posts를 조회하는데 시간이 너무 오래걸렸음

org.hibernate.SQL: DEBUG 통해 요청 1번에 11번의 쿼리가 나오는 것을 확인
-> N+1 문제 확인

# PostForge 부하 테스트 비교 분석

## 테스트 환경

| 항목 | 값 |
|------|-----|
| 서버 | Oracle Cloud ARM 1vCPU / 1GB RAM |
| 애플리케이션 | Spring Boot 3.x + PostgreSQL |
| 부하 도구 | k6 (30 VU, 4분) |
| 모니터링 | Prometheus + Grafana (Spring Boot Actuator / Micrometer) |

---

## 1. Before — 단일 시나리오 (로그인 혼합)

30 VU가 읽기 + 로그인을 동일 비중으로 반복 실행

### k6 결과

| 엔드포인트 | med | p95 | max | threshold (p95) | 결과 |
|-----------|-----|-----|-----|-----------------|------|
| GET /posts | 498ms | 1.89s | 4.08s | 500ms | ✗ 초과 |
| GET /posts?keyword | 297ms | 1.58s | 3.01s | 500ms | ✗ 초과 |
| GET /posts/{id} | 315ms | 1.60s | 3.81s | 500ms | ✗ 초과 |
| GET /posts/{id}/comments | 198ms | 1.28s | 2.30s | 500ms | ✗ 초과 |
| POST /auth/login | 1.98s | 3.52s | 4.59s | 800ms | ✗ 초과 |
| POST /auth/token/reissue | 191ms | 694ms | 1.60s | 500ms | ✗ 초과 |

| 항목 | 값 |
|------|-----|
| checks | 100% |
| 에러율 (expected_result:success) | 0.00% |
| RPS | 14.66 |
| 총 요청 수 | 3,588 |

### Grafana 지표

| 지표 | 값 | 판정 |
|------|-----|------|
| CPU Usage | max **100%** | 🔴 포화 |
| Load | max **12.0** | 🔴 1코어 대비 12배 |
| Duration AVG / MAX | 170ms / 3.51s | 🔴 20배 차이 |
| Rate | 부하 초반 상승 후 정체/하락 | 🔴 처리 한계 도달 |
| GC Pressure | 0.4% | ✅ 정상 |
| GC Pause max | 80ms | ✅ 정상 |
| Heap 사용률 | 45% | ✅ 여유 |
| Heap 패턴 | 톱니바퀴 | ✅ 정상 |
| Tenured Gen | 안정적 | ✅ 누수 없음 |
| BLOCKED 스레드 | 0 | ✅ 락 경합 없음 |

### 병목 분석

**원인: CPU 병목 (BCrypt)**

- 30 VU가 매 iteration마다 로그인 → BCrypt(strength 10 = 2^10 = 1,024번 반복 해싱) 실행
- 1vCPU에서 BCrypt 1건당 100~300ms CPU 점유 → 동시 요청이 쌓이면서 CPU 포화
- CPU가 BCrypt에 독점당하면서 읽기 API까지 줄 서서 대기 → 전체 API 응답 지연

**근거:**

1. CPU Usage 100%, Load 12.0 → 대기 작업이 12개 쌓임
2. Rate가 부하 초반 상승 후 정체 → CPU 포화로 처리량 한계
3. Duration AVG 170ms vs MAX 3.51s (20배 차이) → CPU 줄 서기로 뒤의 요청일수록 대기 시간 증가
4. 로그인(med 1.98s)이 다른 API(med 198~498ms)보다 4~10배 느림 → BCrypt가 병목의 원인

---

## 2. After — 시나리오 분리 (읽기 25 VU + 로그인 5 VU)

실제 트래픽 비율에 맞게 읽기 25명, 로그인 5명으로 분리

### k6 결과

| 엔드포인트 | med | p95 | max | threshold (p95) | 결과 |
|-----------|-----|-----|-----|-----------------|------|
| GET /posts | 90ms | 508ms | 1.58s | 500ms | ✗ 근소 초과 |
| GET /posts?keyword | 31ms | 290ms | 1.08s | 500ms | ✓ 통과 |
| GET /posts/{id} | 19ms | 205ms | 1.04s | 500ms | ✓ 통과 |
| GET /posts/{id}/comments | 16ms | 189ms | 613ms | 500ms | ✓ 통과 |
| POST /auth/login | 476ms | 1.38s | 3.00s | 1000ms | ✗ 초과 |
| POST /auth/token/reissue | 11ms | 104ms | 297ms | 500ms | ✓ 통과 |

| 항목 | 값 |
|------|-----|
| checks | 100% |
| 에러율 (expected_result:success) | 0.00% |
| RPS | 23.75 |
| 총 요청 수 | 5,772 |

### Grafana 지표

| 지표 | 값 | 판정 |
|------|-----|------|
| CPU Usage | max **59%** | ✅ 여유 |
| Load | max **0.7** | ✅ 정상 |
| Duration AVG / MAX | 172ms / 182ms | ✅ 안정적 |
| Rate | 부하에 비례하여 상승 | ✅ 여유 있음 |
| GC Pressure | 0.1% | ✅ 정상 |
| GC Pause max | 80ms | ✅ 정상 |
| Heap 사용률 | 48% | ✅ 여유 |
| Heap 패턴 | 톱니바퀴 | ✅ 정상 |
| Tenured Gen | 안정적 | ✅ 누수 없음 |
| BLOCKED 스레드 | 0 | ✅ 락 경합 없음 |

---

## 3. Before / After 비교

### Grafana 지표 비교

| 지표 | before | after | 변화 |
|------|--------|-------|------|
| CPU Usage max | 100% | **59%** | 🟢 41%p 감소 |
| Load max | 12.0 | **0.7** | 🟢 17배 감소 |
| Duration MAX | 3.51s | **182ms** | 🟢 19배 개선 |
| Rate 패턴 | 정체/하락 | 비례 상승 | 🟢 처리 여유 |
| RPS | 14.66 | **23.75** | 🟢 1.6배 증가 |

### k6 엔드포인트별 비교 (p95)

| 엔드포인트 | before p95 | after p95 | 개선 |
|-----------|-----------|----------|------|
| GET /posts | 1.89s | **508ms** | **3.7배** |
| GET /posts?keyword | 1.58s | **290ms** | **5.4배** |
| GET /posts/{id} | 1.60s | **205ms** | **7.8배** |
| GET /posts/{id}/comments | 1.28s | **189ms** | **6.8배** |
| POST /auth/login | 3.52s | **1.38s** | **2.5배** |
| POST /auth/token/reissue | 694ms | **104ms** | **6.7배** |

### k6 엔드포인트별 비교 (med)

| 엔드포인트 | before med | after med | 개선 |
|-----------|-----------|----------|------|
| GET /posts | 498ms | **90ms** | **5.5배** |
| GET /posts?keyword | 297ms | **31ms** | **9.6배** |
| GET /posts/{id} | 315ms | **19ms** | **16.6배** |
| GET /posts/{id}/comments | 198ms | **16ms** | **12.4배** |
| POST /auth/login | 1.98s | **476ms** | **4.2배** |
| POST /auth/token/reissue | 191ms | **11ms** | **17.4배** |

---

## 4. 결론

### 병목 원인

1vCPU 환경에서 BCrypt 해싱(CPU-intensive)이 CPU를 독점하면서, 무관한 읽기 API까지 연쇄적으로 응답 지연이 발생했다.

### 검증 방법

코드 변경 없이 k6 시나리오의 로그인 비중만 현실적으로 조정(30 VU 전원 → 5 VU)하여, CPU 병목 해소 후 읽기 API 성능을 비교했다.

### 핵심 수치

- CPU: 100% → 59% (41%p 감소)
- Load: 12.0 → 0.7 (17배 감소)
- 읽기 API p95: 평균 3.7~7.8배 개선
- RPS: 14.66 → 23.75 (1.6배 증가)

### 추가 발견

시나리오 분리 후 CPU 병목이 해소된 상태에서, GET /posts(게시글 목록)만 med 90ms로 다른 API(16~31ms) 대비 3~5배 느린 것을 확인했다. 
CPU가 여유 있는 상태에서 특정 API만 느린 것은 해당 API의 쿼리 수준 문제(N+1 등)가 의심되며, 추가 확인이 필요하다.