# Performance Reports

성능 테스트 결과를 Git에 남길 때는 원본 로그 전체보다 사람이 비교할 수 있는 요약 리포트를 우선한다.

## 저장 위치

| 종류 | 위치 | 비고 |
|---|---|---|
| 표준 리포트 | `docs/performance/YYYY-MM-DD-<scenario>.md` | 사람이 읽는 최종 기록 |
| k6 원문 요약 | `docs/performance/k6/` | 민감정보와 과도한 raw body는 제외 |
| Grafana 캡처 | `docs/performance/grafana/` | 중요한 대시보드 캡처만 보관 |
| 작성 템플릿 | `docs/performance/performance-report-template.md` | 새 리포트 작성 시 복사해서 사용 |

## 자동 생성

k6 script는 `handleSummary()`로 테스트 종료 후 markdown 리포트와 summary JSON을 자동 생성한다.

기본 출력 위치:

```text
docs/performance/<auto-name>.md
docs/performance/k6/<auto-name>-summary.json
```

파일명과 대상 이름은 shell env로 고정할 수 있다.

```bash
K6_TARGET_NAME=staging \
K6_SCENARIO_NAME=smoke \
K6_REPORT_NAME=2026-05-04-staging-smoke \
k6 run tests/k6/generated/smoke.js
```

출력 위치를 바꾸려면 다음 값을 사용한다.

```bash
K6_REPORT_DIR=docs/performance \
K6_SUMMARY_DIR=docs/performance/k6 \
k6 run tests/k6/generated/smoke.js
```

## 파일명

파일명은 날짜, 대상, 목적이 보이게 작성한다.

```text
YYYY-MM-DD-<target>-<scenario>.md
YYYY-MM-DD-<target>-<scenario>-summary.json
```

예시:

```text
2026-05-04-staging-smoke.md
2026-05-04-staging-baseline.md
2026-05-04-prod-read-only-smoke.md
```

## 남기는 값

리포트에는 다음 값만 안정적으로 남긴다.

- 테스트 목적과 결론
- 대상 환경, image tag 또는 commit hash
- k6 script, executor, VUs, iterations, duration
- checks, request count, failure rate, RPS
- latency avg/med/p90/p95/p99/max
- 가능하면 CPU, memory, network, disk I/O 관측값
- Grafana screenshot, k6 summary JSON 같은 artifact 링크
- 다음 조치

## 제외할 값

Git에 남기는 리포트에는 민감정보를 넣지 않는다.

- Authorization header, cookie, token
- 요청/응답 body 원문
- 운영 DB 데이터나 사용자 식별자
- 내부 IP, 비공개 domain, 계정명
- 너무 큰 raw JSON/HTML 전체

필요하면 URL은 다음처럼 마스킹한다.

```text
https://prod.example.com -> prod
http://10.x.x.x:8080 -> private-prod
```

## 작성 순서

1. k6 실행 후 자동 생성된 markdown 리포트를 확인한다.
2. `performance-report-template.md`를 참고해 수동 해석과 리소스 관측값을 보강한다.
3. k6 summary와 Grafana 값을 필요한 만큼 표에 옮긴다.
4. 수치만 나열하지 말고 결론과 다음 조치를 적는다.
5. 민감정보가 없는지 확인한 뒤 Git에 포함한다.
