# 2026-10-19 Spring Boot 4.1·Spring AI 2.0 업그레이드 전후 비교

## 목적과 결론

Spring Boot 3.5.14·Spring AI 1.0.7에서 Spring Boot 4.1.1·Spring AI 2.0.1로 올린 뒤 읽기 성능, 기동 시간, 배포 jar, 코드가 어떻게 달라졌는지 같은 장비·같은 데이터·같은 부하로 비교했다.

| 항목 | 전 | 후 | 판단 |
|---|---|---|---|
| 읽기 처리량·지연 | 1,802 / 1,866 req/s, p95 37.55 / 33.61ms | 1,815 / 1,877 req/s, p95 37.53 / 33.76ms | **차이 없음.** 번갈아 잰 두 쌍에서 처리량 차이 1% 이내, p95 차이 0.15ms 이내 |
| 기동 시간(중앙값) | 4.06초 | 4.52초 | 0.46초(11%) 늘었다 |
| 실행 jar | 87.2MB, 라이브러리 142개 | 148.8MB, 라이브러리 187개 | **61.6MB(71%) 늘었다.** OpenAI 공식 SDK(`openai-java-core`) 하나가 50.4MB |
| `ai` 모듈 코드 | 314줄 | 37줄 | 직접 만든 LLM 설정·지표 코드를 Spring AI 자동 구성과 기본 지표로 대체 |
| 지원 | Boot 3.5 OSS 지원 2026-06-30 종료 | Boot 4.1 OSS 지원 2027-07-31까지 | 보안 패치를 다시 받는다 |

## 커밋과 환경

| 구분 | commit | 버전 |
|---|---|---|
| 전 | `c4c5d6a5` | Spring Boot 3.5.14, Spring AI 1.0.7 |
| 중간(측정 안 함) | `0a57c75a` | Spring Boot 3.5.16, Spring AI 1.1.8 |
| 후 | `96f59215` | Spring Boot 4.1.1, Spring AI 2.0.1 |

- 장비: Apple M4 10코어, 메모리 24GB, macOS 26.6.2. 앱·PostgreSQL·Redis·k6가 한 장비에서 돈다. 같은 장비에 Prometheus·Grafana·LLM 게이트웨이 컨테이너도 떠 있었다(부하는 주지 않음).
- JVM: OpenJDK 21.0.10, `java -Xms512m -Xmx512m -jar app.jar`(local 프로필).
- 저장소: PostgreSQL 18.3(`pgvector/pgvector:0.8.2-pg18-trixie`), HikariCP 최대 10, Redis 7.4.8(DB 15번).
- 데이터: [seed.sql](./seed.sql). 계정 1,000, 글 20,000(태그 1개씩), 댓글 100,000, 글 좋아요 50,000. 실행마다 DB를 새로 만들고 Flyway 적용 뒤 넣는다.
- 부하: k6 v2.0.0, [read-mix.js](./read-mix.js). 20 VU로 30초 예열한 뒤 50 VU로 60초 측정하고, 측정 구간만 집계한다. think time 없는 closed model이다.
  - 목록 40%(`page` 0~49, `size` 20), 비로그인 상세 20%, 로그인 상세 15%, 댓글 목록 25%.
- 실행: [bench.sh](./bench.sh) `<label> <app.jar> <로그 디렉터리>`. 결과는 `<label>-summary.json`(k6), `<label>-k6.txt`, `<label>-proc.txt`(기동 시간·부하 직후 RSS)에 있다.

## 주요 라이브러리

| 라이브러리 | 전 | 후 |
|---|---|---|
| Spring Boot | 3.5.14 | 4.1.1 |
| Spring Framework | 6.2.18 | 7.0.9 |
| Spring Security | 6.5.10 | 7.1.1 |
| Spring Data Redis / Lettuce | 3.5.11 / 6.6.0 | 4.1.1 / 7.5.2 |
| Hibernate | 6.6.49 | 7.4.5 |
| Tomcat | 10.1.54 | 11.0.24 |
| Jackson | 2.21.2 | 3.1.5(앱), 2.22.1(OpenAI SDK·jjwt가 씀) |
| Flyway | 11.7.2 | 12.4.0 |
| springdoc | 2.8.17 | 3.1.1 |
| Spring AI | 1.0.7(`OpenAiApi`) | 2.0.1(OpenAI Java SDK 4.49.0, OkHttp) |
| Testcontainers | 1.x | 2.x |

## 결과

### 실행별 (측정 구간 60초)

| 순서 | 실행 | 요청 | req/s | 중앙값(ms) | p95(ms) | p99(ms) | 최대(ms) | 실패 | 기동(초) | RSS(MB) |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | before-1 | 106,499 | 1,775 | 26.96 | 38.39 | 55.12 | 371 | 0.00% | 3.95 | 461 |
| 2 | before-2 | 107,851 | 1,798 | 27.00 | 38.01 | 50.82 | 130 | 0.00% | 4.21 | 456 |
| 3 | after-1 | 97,601 | 1,627 | 28.46 | 48.30 | 69.59 | 358 | 0.00% | 6.73 | 455 |
| 4 | after-2 | 103,789 | 1,730 | 27.51 | 42.73 | 57.85 | 180 | 0.00% | 4.44 | 483 |
| 5 | before-3 | 108,123 | 1,802 | 26.91 | 37.55 | 51.36 | 145 | 0.00% | 4.18 | 755 |
| 6 | after-3 | 108,909 | 1,815 | 26.70 | 37.53 | 52.68 | 138 | 0.00% | 4.50 | 566 |
| 7 | after-4 | 112,629 | 1,877 | 26.06 | 33.76 | 47.66 | 186 | 0.00% | 4.54 | 587 |
| 8 | before-4 | 111,980 | 1,866 | 26.39 | 33.61 | 46.65 | 95 | 0.00% | 3.77 | 754 |

### 측정 순서가 결과를 바꿨다

처음에는 전 2회(1~2번)를 잰 뒤 업그레이드하고 후 2회(3~4번)를 쟀다. 이 네 번만 보면 후가 처리량은 3~9% 낮고 p95는 11~26% 높아 업그레이드가 느려진 것처럼 보였다.

같은 장비에서 전·후를 번갈아 다시 쟀다. 5→6번은 전→후, 7→8번은 후→전 순서다.

| 쌍 | 전 | 후 |
|---|---|---|
| 5·6번 | 1,802 req/s, p95 37.55ms | 1,815 req/s, p95 37.53ms |
| 7·8번 | 1,866 req/s, p95 33.61ms | 1,877 req/s, p95 33.76ms |

번갈아 재자 차이가 사라졌다. 같은 jar도 측정 시점에 따라 처리량이 전은 1,775~1,866, 후는 1,627~1,877 req/s로 움직였다. 그래서 1~4번의 차이는 업그레이드가 아니라 측정 시점의 장비 상태 차이로 본다. 업그레이드 전후 비교는 한쪽을 몰아서 재지 않고 번갈아 재야 한다.

4회씩의 중앙값(전 1,800 req/s·p95 37.78ms, 후 1,772 req/s·p95 40.13ms)에도 1~4번의 치우침이 섞여 있다. 차이가 없다는 판단은 번갈아 잰 두 쌍을 근거로 한다.

RSS는 456~755MB로 같은 jar에서도 크게 달라 비교하지 않는다. 힙은 512MB로 고정했고, RSS는 부하 직후 한 번 읽은 값이다.

### 기동 시간

전은 3.77~4.21초(중앙값 4.06초), 후는 4.44~6.73초(중앙값 4.52초)다. 6.73초는 새 jar를 처음 띄운 실행이다. 이를 빼도 후가 0.3~0.5초 느리다.

### 실행 jar

| | 전 | 후 |
|---|---|---|
| 크기 | 87,171,538 bytes(87.2MB) | 148,810,410 bytes(148.8MB) |
| `BOOT-INF/lib` jar 수 | 142 | 187 |
| 가장 큰 라이브러리 | `hibernate-core` 11.5MB | `openai-java-core` 50.4MB |

- 늘어난 크기 대부분은 Spring AI 2.0의 OpenAI 모듈이 쓰는 공식 SDK다. `openai-java-core` 50.4MB에 Kotlin 런타임(`kotlin-reflect` 3.5MB, `kotlin-stdlib`), OkHttp·Okio, `jackson-module-kotlin`이 따라온다.
- 앱은 Jackson 3을 쓰지만 OpenAI SDK와 `jjwt-jackson`이 Jackson 2를 써서 두 버전이 함께 들어간다.
- `spring-ai-starter-model-openai`는 `spring-boot-starter-webclient`도 끌어온다. 이 때문에 Reactor Netty와 HTTP/3·QUIC 네이티브 라이브러리(운영체제별)까지 들어와 jar 수가 211개가 됐다. OpenAI 모듈은 OkHttp로 부르므로 WebClient를 쓰지 않는다. 그래서 제외했고, 187개로 줄었다.

### 코드

- 업그레이드 커밋(`96f59215`)은 25개 파일, +78 −359줄이다. Java만 보면 13개 파일, +28 −310줄이다.
- `ai` 모듈 main 코드는 314줄에서 37줄이 됐다.
  - `LlmConfig`·`LlmProperties`·`LlmTextGenerationMetrics`를 지웠다.
  - 연결 설정은 `spring.ai.openai.*`로 옮겼다(base-url, 빈 api-key면 인증 헤더 없음, timeout, max-retries 0).
  - 어댑터는 `ChatModel.call(SystemMessage, UserMessage)` 한 줄과 실패 시 null 반환만 남았다.
  - 호출 지표는 Spring AI 기본 관측 지표로 바뀌었다. 로컬 Ollama `qwen3:8b`를 실제로 불러서 아래 지표가 남는 것을 확인했다.
    - `gen_ai.client.operation`: 호출 시간, `error` 태그
    - `gen_ai.client.token.usage`: input·output·total
    - `okhttp.requests`
- 나머지 코드 변경은 이렇다.
  - Jackson 3 패키지(`tools.jackson.databind.ObjectMapper`) 3곳
  - `@EntityScan`·`@AutoConfigureMockMvc` 새 패키지
  - 이름이 바뀐 스타터(`spring-boot-starter-webmvc`, `spring-boot-starter-security-oauth2-client`, `spring-boot-starter-flyway`, `spring-boot-starter-webmvc-test`)
  - Testcontainers 2 아티팩트(`testcontainers-postgresql`, `testcontainers-junit-jupiter`)
- deprecated API 경고는 다음과 같았다.
  - 3.5.16·1.1.8 단계: 0건
  - 4.1 단계: Spring Data Redis의 `TimeUnit`을 받는 TTL 메서드 9곳에서 경고가 났다. `Duration` 메서드로 바꿔 0건이 됐다.
- `spring-boot-properties-migrator`로 기동해 봤고, 이름이 바뀐 설정 키는 없었다.

### 기동 로그

- **사라짐:** Flyway가 "PostgreSQL 18.3 is newer than this version of Flyway"라고 경고하던 것이 Flyway 12.4.0에서 사라졌다.
- **정리:** Hibernate의 dialect 명시 경고(HHH90000025)는 업그레이드 전에도 있었다. 설정을 지워 없앴다.
- **새로 생김:** macOS에서만 Lettuce 7이 Netty DNS 리졸버의 macOS 네이티브를 찾지 못해 시스템 기본값을 쓴다는 경고가 난다. Linux 운영 환경과는 관계가 없다.

## 업그레이드하며 실제로 겪은 문제

1. **LLM 호출이 404가 됐다.**
   - 1.x의 `OpenAiApi`는 base URL 뒤에 `/v1/chat/completions`를 붙였는데, 2.0의 OpenAI SDK는 `/chat/completions`만 붙인다.
   - 로컬 Ollama에 실제로 불러 확인했다. `http://localhost:11434`이면 404, `/v1`을 붙이면 200이었다.
   - 환경변수(`LLM_GATEWAY_BASE_URL`, `LLM_CHAT_BASE_URL`)에는 지금처럼 호스트만 두고, `application.yml`에서 `/v1`을 붙였다. 서버의 `.env`는 바꾸지 않아도 된다.
2. **쓰지 않는 HTTP 클라이언트가 jar에 들어왔다.** 위 "실행 jar" 참고. `spring-boot-starter-webclient`를 제외해 jar 24개를 뺐다.
3. **jar가 71% 커졌다.** OpenAI SDK가 원인이고, 지금은 남은 비용이다. Docker 이미지도 그만큼 커진다.
4. **측정 순서 때문에 업그레이드가 느려진 것처럼 보였다.** 위 "측정 순서가 결과를 바꿨다" 참고.
5. **환경변수만 바꾼 테스트를 Gradle이 다시 돌리지 않았다.** LLM 주소를 바꿔 확인할 때 Gradle은 환경변수를 test 태스크 입력으로 보지 않아 이전 결과를 그대로 썼다. `--rerun`으로 다시 돌렸다.

## 스모크 테스트 밖에서 확인한 것

스모크 테스트는 읽기 API만 본다. 업그레이드 뒤 앱을 띄워 [manual-check.sh](./manual-check.sh)로 아래 요청을 보냈고, 모두 기대한 상태로 응답했다.

| 요청 | 응답 |
|---|---|
| 로그인 `POST /api/auth/login` | 200 |
| 글 작성·수정·삭제 | 201 / 200 / 204 |
| 댓글 작성, 좋아요 | 201 / 200 |
| 로그인 상세, 내 프로필 | 200 / 200 |
| 토큰 재발급 `POST /api/auth/token/reissue`(refresh cookie) | 200 |
| 로그아웃 | 204 |
| 인증 메일 발송 | 204, Mailpit 수신 0 → 1 |
| `GET /oauth2/authorization/google` | 302, accounts.google.com으로 이동 |
| `/actuator/prometheus` | 인증 없이 401, Basic 인증으로 200 |
| `/v3/api-docs`, `/swagger-ui.html`, `/` | 200 |

## 다음 조치

- 운영 이미지를 만들기 전에 크기 증가(약 60MB)를 감수할지 정한다. 줄이려면 OpenAI SDK를 쓰지 않는 경로가 필요하다. 예를 들어 게이트웨이가 지원한다면 Ollama 모듈을 쓸 수 있다.
- 운영 서버에서 같은 스크립트로 기준을 잰다. 로컬 단일 장비 수치는 운영 수용량이 아니다.
