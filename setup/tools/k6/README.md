# k6 Setup

k6는 API의 성능 테스트와 부하 테스트를 위한 도구다.

## 설치

```bash
./setup/tools/k6/install.sh
```
설치 스크립트는 다음을 수행한다.

- macOS에서는 Homebrew가 있으면 `brew install k6`를 사용한다.
- Linux/WSL 계열에서는 apt 기반 설치를 시도한다. `sudo` 권한과 네트워크가 필요할 수 있다.
- sudo가 대화형 비밀번호를 요구하면 GitHub release binary를 `~/.local/bin/k6`에 설치한다.

## 검증

```bash
./setup/tools/k6/verify.sh
```

## 예시 실행

k6 script는 `tests/k6/env.js`에서 기본값을 import한다. 기본 `baseUrl`은 `http://localhost:8080`이다.

```bash
k6 run tests/k6/generated/smoke.js
```

다른 주소를 계속 쓰려면 `tests/k6/env.js`의 `baseUrl`을 수정한다. 일회성 실행에서는 shell env가 `env.js` 기본값보다 우선한다. 필요하면 `SMOKE_PATH`, `VUS_COUNT`, `ITERATIONS`도 같은 방식으로 넘긴다.

```bash
BASE_URL=http://127.0.0.1:18080 SMOKE_PATH=/ VUS_COUNT=1 ITERATIONS=3 k6 run tests/k6/generated/smoke.js
```

`generated/` 아래 script는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 baseline/stress script는 `manual/` 아래에 둔다.

k6 script는 `handleSummary()`로 테스트 종료 후 markdown 리포트와 summary JSON을 자동 생성한다. 기본 위치는 `docs/performance/`와 `docs/performance/k6/`이다. 파일명을 고정하려면 `K6_REPORT_NAME`을 지정한다.

```bash
K6_TARGET_NAME=staging K6_SCENARIO_NAME=smoke K6_REPORT_NAME=2026-05-04-staging-smoke k6 run tests/k6/generated/smoke.js
```

Git에 남길 성능 리포트는 `docs/performance/performance-report-template.md` 포맷을 기준으로 수동 해석을 보강한다. 원본 로그 전체보다 요약 수치, 실행 조건, artifact 경로, 결론을 남긴다.
