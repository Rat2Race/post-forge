# Manual Performance Runbook

수동 성능 테스트는 로컬 또는 명시한 대상 서버에만 실행한다. 운영 환경 고부하는 별도 승인 없이 실행하지 않는다.

## 기본값

| 값 | 기본 |
|---|---|
| 대상 URL | `http://localhost:8080` |
| k6 script | `tests/k6/manual/performance.js` |
| Bruno folder | `tests/bruno/api/manual/performance` |
| 리포트 위치 | `docs/performance/manual-runs/<run-id>/` |

## 리포트 디렉터리

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="docs/performance/manual-runs/$RUN_ID"
mkdir -p "$REPORT_DIR"
```

## k6 공개 읽기 부하

```bash
BASE_URL=http://localhost:8080 \
PUBLIC_VUS=5 \
DURATION=1m \
RAMP_UP=30s \
RAMP_DOWN=30s \
K6_TARGET_NAME=local \
K6_SCENARIO_NAME=manual-public \
K6_REPORT_NAME="$RUN_ID-k6-public" \
K6_REPORT_DIR="$REPORT_DIR" \
K6_SUMMARY_DIR="$REPORT_DIR" \
k6 run tests/k6/manual/performance.js 2>&1 | tee "$REPORT_DIR/k6-public.log"
```

남는 파일:

- `$REPORT_DIR/$RUN_ID-k6-public.md`
- `$REPORT_DIR/$RUN_ID-k6-public-summary.json`
- `$REPORT_DIR/k6-public.log`

## k6 인증 쓰기 부하

인증 쓰기 부하는 게시글과 댓글을 생성한 뒤 삭제한다. 로컬 전용 계정만 사용하고 비밀번호는 커맨드 히스토리에 남지 않게 주의한다.

```bash
read -r -s PERF_PASSWORD

BASE_URL=http://localhost:8080 \
RUN_AUTH_FLOW=true \
PERF_USER_ID=testuser1 \
PERF_PASSWORD="$PERF_PASSWORD" \
PUBLIC_VUS=1 \
AUTH_VUS=1 \
DURATION=30s \
RAMP_UP=10s \
RAMP_DOWN=10s \
K6_TARGET_NAME=local \
K6_SCENARIO_NAME=manual-auth \
K6_REPORT_NAME="$RUN_ID-k6-auth" \
K6_REPORT_DIR="$REPORT_DIR" \
K6_SUMMARY_DIR="$REPORT_DIR" \
k6 run tests/k6/manual/performance.js 2>&1 | tee "$REPORT_DIR/k6-auth.log"
```

남는 파일:

- `$REPORT_DIR/$RUN_ID-k6-auth.md`
- `$REPORT_DIR/$RUN_ID-k6-auth-summary.json`
- `$REPORT_DIR/k6-auth.log`

## Bruno 공개 API 반복 실행

```bash
cd tests/bruno/api

bru run manual/performance -r \
  --env-file ./environments/local.bru \
  --tags public \
  --iteration-count 20 \
  --delay 100 \
  --reporter-skip-response-body \
  --reporter-json "../../../$REPORT_DIR/bruno-public.json" \
  --reporter-html "../../../$REPORT_DIR/bruno-public.html" \
  2>&1 | tee "../../../$REPORT_DIR/bruno-public.log"

cd ../../..
```

남는 파일:

- `$REPORT_DIR/bruno-public.json`
- `$REPORT_DIR/bruno-public.html`
- `$REPORT_DIR/bruno-public.log`

## Bruno 인증 쓰기 시나리오

`local.bru` 또는 CLI `--env-var`로 `perfUserId`, `perfPassword`를 로컬 전용 값으로 넣는다. 실행 순서대로 로그인, 생성, 수정, 좋아요, 댓글, 정리 요청이 실행된다.

```bash
cd tests/bruno/api

bru run manual/performance -r \
  --env-file ./environments/local.bru \
  --tags auth \
  --env-var perfUserId=testuser1 \
  --env-var perfPassword="$PERF_PASSWORD" \
  --env-var perfMaxMs=1000 \
  --reporter-skip-body \
  --reporter-skip-headers Authorization \
  --reporter-skip-headers Cookie \
  --reporter-skip-headers Set-Cookie \
  --reporter-json "../../../$REPORT_DIR/bruno-auth.json" \
  --reporter-html "../../../$REPORT_DIR/bruno-auth.html" \
  2>&1 | tee "../../../$REPORT_DIR/bruno-auth.log"

cd ../../..
```

남는 파일:

- `$REPORT_DIR/bruno-auth.json`
- `$REPORT_DIR/bruno-auth.html`
- `$REPORT_DIR/bruno-auth.log`

## 빠른 로컬 검증

서버와 리포트 경로만 빠르게 확인할 때는 부하를 낮춘다.

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="docs/performance/manual-runs/$RUN_ID"
mkdir -p "$REPORT_DIR"

BASE_URL=http://localhost:8080 \
PUBLIC_VUS=1 \
DURATION=5s \
RAMP_UP=1s \
RAMP_DOWN=1s \
K6_TARGET_NAME=local \
K6_SCENARIO_NAME=manual-public-smoke \
K6_REPORT_NAME="$RUN_ID-k6-public-smoke" \
K6_REPORT_DIR="$REPORT_DIR" \
K6_SUMMARY_DIR="$REPORT_DIR" \
k6 run tests/k6/manual/performance.js 2>&1 | tee "$REPORT_DIR/k6-public-smoke.log"

cd tests/bruno/api
bru run manual/performance -r \
  --env-file ./environments/local.example.bru \
  --tags public \
  --iteration-count 1 \
  --reporter-skip-response-body \
  --reporter-json "../../../$REPORT_DIR/bruno-public-smoke.json" \
  --reporter-html "../../../$REPORT_DIR/bruno-public-smoke.html" \
  2>&1 | tee "../../../$REPORT_DIR/bruno-public-smoke.log"
cd ../../..
```
