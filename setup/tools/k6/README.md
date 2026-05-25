# k6 Setup

k6는 API의 성능 테스트와 부하 테스트를 위한 도구다.

## 설치

```bash
./setup/tools/k6/install.sh
```
설치 스크립트는 다음을 수행한다.

- 이미 설치된 `k6`가 있으면 그대로 사용한다.
- Linux/WSL 계열에서 `k6`가 없으면 기본적으로 GitHub release binary를 user-local 경로에 설치한다.
- macOS에서 `k6`가 없으면 `K6_BIN`으로 기존 binary를 지정하거나 system mode를 명시한다.
- apt/Homebrew 같은 system install은 `K6_INSTALL_MODE=system` 또는 `SETUP_INSTALL_MODE=system`일 때만 사용한다.

```bash
K6_INSTALL_MODE=system ./setup/tools/k6/install.sh
```

## 검증

```bash
./setup/tools/k6/verify.sh
```

## 공통 테스트 env

setup entrypoint는 `tests/.env`가 없으면 생성하고, 있으면 shell env를 덮어쓰지 않는 방식으로 로드한다. 이 파일은 `tests/**` ignore 정책에 따라 로컬 산출물로 취급한다.

```env
BASE_URL=http://localhost:8080
K6_SCRIPT_ROOT=tests/k6
K6_REPORT_ROOT=setup/reports/k6
K6_TARGET_NAME=local
VUS_COUNT=1
ITERATIONS=1
```

값을 비워두면 wrapper나 k6 script의 기본값을 사용한다.

## 간편 실행

리포트 옵션을 매번 직접 붙이지 않으려면 setup package 안의 wrapper를 사용한다. 기본 실행은 `K6_SCRIPT_ROOT` 아래 generated smoke script를 대상으로 실행하고, markdown 리포트/summary JSON/log를 `K6_REPORT_ROOT/<run-id>/` 아래에 남긴다.

```bash
./setup/run-k6
```

setup wrapper를 통해서도 같은 명령을 실행할 수 있다.

```bash
./setup/run.sh run-k6 smoke
```

대상 주소나 script만 바꾸면 리포트 이름과 경로는 자동으로 잡힌다.

```bash
BASE_URL=http://127.0.0.1:8080 ./setup/run-k6 manual
BASE_URL=http://127.0.0.1:8080 ./setup/run-k6 manual/performance.js
```

다른 프로젝트가 k6 script를 다른 위치에 둔다면 `--script-root` 또는 `K6_SCRIPT_ROOT`로 바꾼다.

```bash
./setup/run-k6 smoke --script-root performance/k6
```

기존 k6 script 환경변수(`PUBLIC_VUS`, `TEST_SECONDS`, `PERF_USER_ID`, `PERF_PASSWORD` 등)는 그대로 사용할 수 있다. native k6 옵션은 `--` 뒤에 둔다.

```bash
./setup/run-k6 manual -- --quiet
```

## 수동 실행

k6 script는 `tests/k6/env.js`에서 기본값을 import한다. 기본 `baseUrl`은 `http://localhost:8080`이다. 전체 setup wrapper로 Bruno + k6 generated smoke를 같이 실행하려면 repo root에서 실행한다.

```bash
BASE_URL=http://127.0.0.1:8080 ./setup/run.sh run-smoke
```

k6만 직접 실행하려면 먼저 `./setup/run.sh generate-tests`로 generated smoke script를 최신화한다.

```bash
k6 run tests/k6/generated/smoke.js
```

다른 주소를 계속 쓰려면 `tests/.env`의 `BASE_URL`을 수정한다. 일회성 실행에서는 shell env가 `tests/.env`보다 우선한다. generated smoke는 `tests/testing-policy.yml`에서 고른 GET endpoint 목록을 사용하며, 부하 조건은 `VUS_COUNT`, `ITERATIONS`로 조절한다.

```bash
BASE_URL=http://127.0.0.1:18080 VUS_COUNT=1 ITERATIONS=3 k6 run tests/k6/generated/smoke.js
```

`generated/` 아래 script는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 baseline/stress script는 `manual/` 아래에 둔다. manual script는 파일명을 직접 지정해서 실행한다.

```bash
BASE_URL=http://127.0.0.1:18080 VUS_COUNT=5 ITERATIONS=50 k6 run tests/k6/manual/<script-name>.js
```

k6 script는 `handleSummary()`로 테스트 종료 후 markdown 리포트와 summary JSON을 자동 생성한다. 기본 위치는 `docs/performance/`와 `docs/performance/k6/`이다. 파일명을 고정하려면 `K6_REPORT_NAME`을 지정한다.

```bash
K6_TARGET_NAME=staging K6_SCENARIO_NAME=smoke K6_REPORT_NAME=2026-05-04-staging-smoke k6 run tests/k6/generated/smoke.js
```

Git에 남길 성능 리포트는 `docs/performance/performance-report-template.md` 포맷을 기준으로 수동 해석을 보강한다. 원본 로그 전체보다 요약 수치, 실행 조건, artifact 경로, 결론을 남긴다. `tests`를 통째로 삭제하고 setup을 다시 돌리면 manual script도 사라지므로, 보존해야 하는 수동 성능 테스트는 커밋하거나 별도 백업한 뒤 재생성한다.
