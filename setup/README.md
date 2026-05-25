# Testing Tool Setup

테스팅 도구를 설치하고 초기화하는 진입점이다. 대상 환경은 Unix 계열 shell이다.

지원 범위:

- Linux/Ubuntu shell
- WSL 내부 Linux shell
- macOS shell

## 구조

```text
setup/
  AGENTS.md
  CHANGELOG.md
  README.md
  VERSION
  manifest.yml
  env.sh
  run.sh
  run-k6
  doctor.sh
  install.sh
  pack.sh
  verify.sh
  reports/
  state/
  scripts/
  tools/
    bruno/
      AGENTS.md
      README.md
      tool.yml
      install.sh
      verify.sh
    k6/
      AGENTS.md
      README.md
      tool.yml
      install.sh
      verify.sh
```

테스트 정책과 도구별 산출물은 `tests/` 아래에 둔다.

```text
tests/
  .env
  testing-policy.yml
  testing-policy.schema.json
  bruno/api/generated/
  bruno/api/manual/
  k6/generated/
  k6/manual/
```

`tests/.env`는 setup이 없을 때 생성하는 공통 로컬 설정 파일이다. Bruno, k6, SQL draft 같은 테스트 도구가 같은 대상 URL과 실행 옵션을 공유할 때 사용한다. setup은 `tests/.gitignore`도 함께 생성해서 이 파일이 commit되지 않게 한다.

## Quickstart

다른 프로젝트에서 setup 패키지만 복사해서 쓸 때는 repo root에서 실행한다.

```bash
cp -a /path/to/testing-tool-setup/setup ./setup

./setup/install.sh
./setup/verify.sh
./setup/doctor.sh
BASE_URL=http://localhost:8080 ./setup/run-k6 smoke
```

`./setup/install.sh`는 기본적으로 user-local 설치만 사용한다. shell startup file은 수정하지 않고, `tests/.env`와 `tests/.gitignore`를 생성해 로컬 설정과 비밀값이 커밋되지 않게 한다. system package manager를 쓰려면 명시적으로 opt-in한다.

```bash
SETUP_INSTALL_MODE=system ./setup/install.sh
```

배포용 archive는 setup package 안에서 만들 수 있다.

```bash
./setup/pack.sh
```

## 명령

LLM agent에게 맡길 때는 다음 한 문장을 기본 진입점으로 사용한다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅해줘.
```

기본 진입점은 설치 권한이 큰 작업을 자동 실행하지 않고 `assess -> plan -> analyze-project -> sync-policy -> generate-tests` 순서로 진행한다. 도구 설치까지 필요하면 다음처럼 명시한다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅하고 도구 설치까지 해줘.
```

계획을 확인한다.

```bash
./setup/run.sh assess
./setup/run.sh plan
```

환경과 툴을 설치/초기화한다. `manifest.yml`의 enabled tool을 병렬로 실행한다.

```bash
./setup/install.sh
```

설치 스크립트는 기본적으로 user-local binary/npm prefix를 사용한다. shell startup file에는 PATH를 등록하지 않는다. setup wrapper는 실행 중에만 필요한 경로를 로드한다. `bru`, `k6`를 직접 실행해야 하면 repo root에서 `source ./setup/env.sh`를 먼저 실행한다. system install은 `SETUP_INSTALL_MODE=system`, `BRUNO_INSTALL_MODE=system`, `K6_INSTALL_MODE=system` 중 하나로 명시했을 때만 사용한다.

설치와 scaffold 상태를 검증한다.

```bash
./setup/verify.sh
```

로컬 진단을 실행한다.

```bash
./setup/doctor.sh
```

프로젝트 endpoint 후보를 수집한다. OpenAPI/Swagger 파일이 있으면 `paths`를 우선 사용하도록 inventory에 기록하고 controller scan은 건너뛴다. OpenAPI 파일이 없을 때만 controller route 후보를 수집한다.

```bash
./setup/run.sh analyze-project
```

수집된 inventory를 기준으로 `tests/testing-policy.yml`을 보수적으로 동기화한다. OpenAPI endpoint를 먼저 사용하고, 없거나 파싱할 수 없을 때 controller 후보로 fallback한다. 기존 endpoint class는 보존하고, 새 endpoint만 추가한다.

```bash
./setup/run.sh sync-policy
```

policy를 기준으로 Bruno/k6 generated 산출물과 JPA entity 기반 SQL draft를 만든다.

```bash
./setup/run.sh generate-tests
```

`generate-tests` 재생성 산출물:

- `tests/bruno/api/generated` — Bruno native generated requests
- `tests/k6/generated` — k6 generated smoke script
- `tests/sql/generated` — JPA entity에서 추론한 검토용 SQL draft
- `tests/sql/manual` — 사람이 다듬어 보관하는 수동 SQL 영역. 자동 덮어쓰기 금지

Bruno generated request는 native `.bru` 파일로 생성된다. collection root는 `tests/bruno/api/bruno.json`과 `tests/bruno/api/collection.bru`를 사용한다.
SQL draft는 setup이 실행하지 않는다. 필요한 경우 내용을 검토한 뒤 `tests/sql/manual`로 옮기거나 수정해서 수동으로 사용한다.

앱 서버가 실행 중이면 Bruno/k6 smoke를 자동 실행하고 결과 report를 만든다. setup의 기본 `baseUrl`은 `http://localhost:8080`으로 고정한다. `run-smoke`가 만든 k6 markdown/json은 `setup/reports/**`, `setup/state/**` 아래에 남겨 Git에는 보이지 않게 한다. k6를 단독 실행할 때만 기본값에 따라 `docs/performance/**`에 성능 리포트를 쓴다.

```bash
./setup/run.sh run-smoke
```

다른 주소를 일회성으로 쓰려면 `BASE_URL`만 override한다.

```bash
BASE_URL=http://127.0.0.1:8080 ./setup/run.sh run-smoke
```

k6만 단독 실행하려면 setup package 안의 shortcut을 사용한다. 리포트 관련 `K6_*` 값은 `tests/.env`에서 읽고, 기본 리포트 위치는 `setup/reports/k6/<run-id>/`이다.

```bash
./setup/run-k6 smoke
./setup/run-k6 manual
./setup/run-k6 manual/performance.js
```

다른 프로젝트에서 k6 script 위치가 다르면 `K6_SCRIPT_ROOT` 또는 `--script-root`로 바꾼다.

```bash
./setup/run-k6 smoke --script-root performance/k6
```

```bash
BASE_URL=http://127.0.0.1:18080 ./setup/run.sh run-smoke
```

`install.sh`, `verify.sh`는 모두 `run.sh`의 wrapper다.

## baseUrl 변경 루트

기본값은 `http://localhost:8080`이다. 사용자가 다른 서버 주소를 써야 할 때는 목적에 맞는 한 곳만 바꾼다.

| 목적 | 변경 위치 |
|---|---|
| 자동 smoke 실행에서 일회성 변경 | `BASE_URL=http://host:port ./setup/run.sh run-smoke` |
| Bruno 수동 실행에서 계속 사용할 로컬 값 변경 | `tests/bruno/api/environments/local.bru`의 `baseUrl:` |
| Bruno 수동 실행에서 일회성 변경 | `bru run ... --env-var baseUrl=http://host:port` |
| k6 script에서 계속 사용할 로컬 값 변경 | `tests/k6/env.js`의 `baseUrl` |
| k6 직접 실행에서 일회성 변경 | `BASE_URL=http://host:port k6 run tests/k6/generated/smoke.js` |

`tests/bruno/api/environments/local.bru`는 local-only 파일이므로 사용자 PC/서버에 맞게 수정해도 commit하지 않는다.
`tests/k6/env.js`도 k6 script용 로컬 기본값 파일이다. generated/manual k6 script는 필요한 값만 import하고, shell의 `BASE_URL`/`SMOKE_PATH`/`VUS_COUNT`/`ITERATIONS` 값이 있으면 그 값을 우선한다.

Bruno/k6를 직접 터미널에서 실행할 때 `bru: command not found`처럼 user-local 설치 경로가 잡히지 않으면, 현재 셸에서 다음을 먼저 실행한다.

```bash
source ./setup/env.sh
bru --version
k6 version
```

환경별 연결 문제가 있으면 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)를 먼저 확인한다. 특히 Windows에서 앱 서버를 띄우고 WSL에서 Bruno/k6만 실행하는 경우에는 WSL의 `localhost`가 Windows host를 가리키지 않는다.

## Bruno 수동 실행

먼저 로컬 환경 파일을 준비한다. `local.example.bru`의 기본 `baseUrl`은 `http://localhost:8080`이다.

```bash
cd tests/bruno/api
[ -f environments/local.bru ] || cp environments/local.example.bru environments/local.bru
```

기본 서버를 쓰면 수정 없이 generated smoke만 실행한다. 다른 주소를 계속 쓰려면 `environments/local.bru`의 `baseUrl:`만 바꾼다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru
```

환경 파일을 수정하지 않고 서버 주소만 override할 수도 있다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru --env-var baseUrl=http://127.0.0.1:18080
```

특정 request 하나만 실행한다.

```bash
cd tests/bruno/api
bru run generated/smoke/<request-file>.bru --env-file ./environments/local.bru
```

review가 필요한 draft/scenario request는 명시적으로 폴더를 지정해서 실행한다.

```bash
cd tests/bruno/api
bru run generated/draft -r --env-file ./environments/local.bru
bru run generated/scenario -r --env-file ./environments/local.bru
```

Bruno CLI 콘솔 출력은 성공/실패 요약 중심이다. `--verbose`를 붙여도 성공 요청의 request/response 본문이 항상 콘솔에 표시되지는 않는다.

```bash
cd tests/bruno/api
bru run generated/smoke/<request-file>.bru --env-file ./environments/local.bru --verbose
```

request URL, response status/header/body, test 결과를 자세히 보려면 reporter JSON을 남긴다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru --reporter-json ../../../setup/state/bruno-smoke-report.json
```

브라우저에서 보기 좋은 리포트가 필요하면 HTML reporter도 사용할 수 있다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru --reporter-html ../../../setup/reports/bruno-smoke-report.html
```

`./setup/run.sh run-smoke`로 자동 실행할 때는 Bruno stdout/stderr가 `setup/state/smoke-run-result.json`에 기록되고, Bruno reporter JSON은 `setup/state/bruno-smoke-report.json`에 기록된다. 요약 tail은 `setup/reports/testing-run-report.md`에 기록된다. runner debug 로그를 조금 더 켜려면 다음처럼 실행한다.

```bash
BRUNO_VERBOSE=true BASE_URL=http://localhost:8080 ./setup/run.sh run-smoke
```

## k6 수동 실행

k6 script는 `tests/k6/env.js`에서 기본값을 import한다. 기본 `baseUrl`은 `http://localhost:8080`이다.

```bash
k6 run tests/k6/generated/smoke.js
```

다른 주소를 계속 쓰려면 `tests/.env`의 `BASE_URL`을 바꾼다. 환경 파일을 수정하지 않고 일회성으로 바꿀 때는 shell env를 넘긴다.

```bash
BASE_URL=http://127.0.0.1:18080 k6 run tests/k6/generated/smoke.js
VUS_COUNT=1 ITERATIONS=3 BASE_URL=http://localhost:8080 k6 run tests/k6/generated/smoke.js
```

테스트가 끝나면 k6 `handleSummary()`가 자동으로 markdown 리포트와 summary JSON을 만든다. 기본 위치는 `docs/performance/`와 `docs/performance/k6/`이다. 파일명은 자동 생성되며, 고정하려면 `K6_REPORT_NAME`을 지정한다.

```bash
K6_TARGET_NAME=staging K6_SCENARIO_NAME=smoke K6_REPORT_NAME=2026-05-04-staging-smoke k6 run tests/k6/generated/smoke.js
```

Git에 남길 성능 리포트의 수동 보강 포맷은 `docs/performance/performance-report-template.md`를 따른다.

## WSL/nvm 실행 팁

WSL이나 CI/원격 서버의 non-interactive shell에서는 `~/.bashrc`가 로드되지 않아 `nvm` Node가 PATH에 없을 수 있다. `./setup/run.sh assess`에서 `npm: present`, `node: missing`이 같이 나오면 Windows npm이 먼저 잡혔거나 `nvm`이 로드되지 않은 상태일 가능성이 높다.

그럴 때는 같은 shell에서 다음을 먼저 실행한 뒤 setup 명령을 다시 실행한다.

```bash
source "$HOME/.nvm/nvm.sh" 2>/dev/null || true
source ./setup/env.sh 2>/dev/null || true
./setup/run.sh assess
./setup/run.sh install
./setup/run.sh verify
```

Agent가 도구 설치까지 맡는 경우에도 `assess`, `install`, `verify`, `sync-policy`, `generate-tests`는 이 prelude를 적용한 동일한 Unix shell에서 실행하는 것이 안전하다.

## OpenAPI 우선 모드

다음 파일 중 하나가 repo에 있으면 setup은 OpenAPI 기반으로 먼저 policy를 구성한다.

```text
openapi.yml
openapi.yaml
openapi.json
swagger.yml
swagger.yaml
swagger.json
```

OpenAPI가 발견되면 `setup/state/project-inventory.json`에 `discoveryMode: "openapi"`가 기록되고, 큰 프로젝트에서 오래 걸릴 수 있는 controller route scan은 생략된다. OpenAPI가 없거나 `paths`를 파싱할 수 없을 때만 controller scan 결과를 fallback으로 사용한다.

## Git/ignore 정책

`tests/`는 generated testing artifact 영역으로 intentionally ignored 상태다. `tests/testing-policy.yml`, `tests/bruno/**`, `tests/k6/**`가 Git에서 ignored로 보이는 것은 setup 실패가 아니다.

`setup/state/**`과 `setup/reports/**`도 실행 기록과 보고서라 commit 대상이 아니다. 반대로 `setup/`의 executor, manifest, tool module 파일은 setup 자체의 관리 표면이므로, 기존 작업트리 상태가 변경/삭제/추가로 보이더라도 agent가 임의로 복구하거나 정리하지 않는다.

## 툴 추가

새 툴을 추가할 때는 다음 구조를 만든다.

```text
setup/tools/<tool>/
  AGENTS.md
  README.md
  tool.yml
  install.sh
  verify.sh
```

그리고 기본 설치 대상이면 `setup/manifest.yml`의 `tools` 아래에 추가한다.

## 현재 대상

- `bruno`: API 수동 테스트와 smoke/scenario harness
- `k6`: 성능 테스트와 부하 테스트

## 주의

- `AGENTS.md`는 LLM agent가 초기세팅을 직접 맡을 때의 메인 프로토콜이다.
- 기본 프롬프트 `setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅해줘.`는 도구 설치를 자동 실행하지 않고 분석/policy/generated 생성까지 수행한다.
- 도구 설치까지 필요하면 "도구 설치까지"라고 명시한다.
- `manifest.yml`은 setup 의도, 위험도, 산출물을 선언한다.
- `manifest.yml`의 `stages`는 Linux/WSL 계열과 macOS의 package manager와 위험도를 나눈다.
- `tests/testing-policy.yml`은 Bruno/k6보다 우선하는 smoke/scenario/draft 기준이다.
- `sync-policy`는 기존 class를 덮어쓰지 않고 충돌 시 `suggestedClass`와 `reviewRequired`로 표시한다.
- `generate-tests`는 Bruno/k6/SQL `generated/` 영역을 재생성한다. Bruno smoke는 `reviewRequired: false`인 smoke만 포함하고, 나머지는 draft/scenario로 분리한다. SQL은 JPA entity 기반 draft만 만들고 실행하지 않는다.
- `run-smoke`는 Bruno/k6 smoke를 실행하고 `setup/state/smoke-run-result.json`, `setup/reports/testing-run-report.md`에 결과와 개선 방향을 남긴다.
- `generated/` 아래 파일은 Agent가 자동 갱신할 수 있고, `manual/` 아래 파일은 자동 덮어쓰기 금지다.
- `.gitkeep`만 들어있는 폴더는 의도된 placeholder다. 예를 들어 `manual/`은 사람이 나중에 직접 작성할 테스트를 위한 보호 영역이다.
- `tool.yml`은 툴별 목적, 버전관리 전략, 위험도, 산출물을 선언한다.
- `install.sh`는 도구 설치와 scaffold 생성만 담당한다.
- user-local 설치를 사용해도 `install.sh`는 shell startup file을 수정하지 않는다. 직접 `bru`, `k6`를 실행하려면 `source ./setup/env.sh` 또는 절대 경로를 사용한다.
- `verify.sh`는 앱이 실행 중이라는 가정 없이 가능한 검증만 한다. API smoke 실행은 Bruno CLI와 앱이 준비된 뒤 별도로 수행한다.
- `setup/state/**`은 실행 결과 기록용이며 commit하지 않는다.
- `setup/reports/**`는 실행 보고서 기록용이며 commit하지 않는다.
