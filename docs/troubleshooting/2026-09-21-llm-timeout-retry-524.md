# 뉴스 수동 발행 중 LLM 타임아웃과 Cloudflare 524

## 증상

`POST /api/admin/launch-news/manual` 호출에서 `HttpTimeoutException: Request cancelled`와
`503: llm gateway busy`가 발생했고, 클라이언트는 Cloudflare 524를 받았다.

## 원인

백엔드의 채팅 응답 대기는 기본 60초였지만, 첫 Ollama 생성은 약 68초 걸렸다.
게이트웨이는 180초까지 기다리고 동시 처리 한도는 1개였다.
백엔드가 먼저 타임아웃된 뒤에도 기존 생성이 계속돼 재시도가 `busy`로 거절됐다.
이후 재시도까지 누적되면서 전체 처리 시간이 128.7초로 늘어나 Cloudflare 응답 대기 제한을 넘었다.

| 시간(UTC, 2026-09-21) | 확인한 내용 |
| --- | --- |
| 02:01:18 | 문서 6개 벡터 저장 완료, 생성 시작 |
| 02:02:18 | 백엔드 60초 타임아웃 |
| 02:02:20 | 재시도에 게이트웨이 `503 busy` 반환 |
| 02:02:27 | 첫 Ollama 생성 완료 |
| 02:03:22 | Cloudflare 연결 취소 로그, 클라이언트에서 524 확인 |
| 02:03:26 | 후속 생성 성공, 게시글 ID 1 저장, 백엔드 200 기록 |

백엔드에서 200을 기록해도 이미 연결이 종료됐다면 클라이언트는 그 응답을 받지 못한다.
이번에는 발행 자체가 실패한 것이 아니라 결과 전달이 타임아웃됐다.

## 당시 로그

사용자가 제공한 로그에서 발췌했다. 반복되는 호출 스택은 생략했다.
Cloudflare 로그에는 연결 취소가 기록됐으며, HTTP 524는 클라이언트 응답에서 별도로 확인했다.

```text
org.springframework.web.client.ResourceAccessException: I/O error on POST request for "http://10.0.0.1:8088/v1/chat/completions": Request cancelled
Caused by: java.net.http.HttpTimeoutException: Request cancelled
postforge-app         | 	at org.springframework.http.client.JdkClientHttpRequest$TimeoutHandler.handleCancellationException(JdkClientHttpRequest.java:287) ~[spring-web-6.2.18.jar:6.2.18]

postforge-app         | 2026-09-21T02:02:20.828Z  WARN [requestId=3d27bec2-cffe-4d66-a320-690911088688] 1 --- [nio-8080-exec-7] o.springframework.ai.retry.RetryUtils    : Retry error. Retry count:2
postforge-app         | org.springframework.ai.retry.TransientAiException: 503 - {"detail":"llm gateway busy"}

postforge-cloudflare  | 2026-09-21T02:03:22Z ERR  error="Incoming request ended abruptly: context canceled" connIndex=2 event=1 ingressRule=0 originService=http://postforge-app:8080
postforge-cloudflare  | 2026-09-21T02:03:22Z ERR Request failed error="Incoming request ended abruptly: context canceled" connIndex=2 dest=https://api.iamrat.dev/api/admin/launch-news/manual event=0 ip=198.41.200.53 type=http

postforge-app         | 2026-09-21T02:03:26.453Z  INFO [requestId=3d27bec2-cffe-4d66-a320-690911088688] 1 --- [nio-8080-exec-7] d.i.support.web.RequestLoggingFilter     : http_request method=POST uri=/api/admin/launch-news/manual status=200 elapsedMs=128684.535
```

## 대응 방향

생성 시간에 맞춰 백엔드·게이트웨이 타임아웃과 재시도 횟수를 함께 조정해야 한다.
동기 요청은 수집·생성·재시도 전체가 외부 프록시 제한 안에 끝나도록 설계한다.
장시간 또는 여러 기사 발행은 작업 등록과 결과 조회를 분리하는 비동기 처리를 검토한다.
이 문서는 원인 확인 기록이다. 이후 0be32ca5에서 발행 경로에 별도 LLM 클라이언트를 두어 읽기 제한 시간(`LLM_PUBLISHING_READ_TIMEOUT`, 기본 210초)을 분리하고 재시도를 1회로 줄였다.
남은 채팅 모델(`LlmConfig.llmChatModel`)도 2026-10-02부터 자체 재시도를 끈다(`maxAttempts(1)`). 503을 돌려주는 서버로 확인하면 요청은 1번만 가고 바로 실패한다(`LlmConfigTest`).

참고: [Cloudflare 524 설명](https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-524/).
