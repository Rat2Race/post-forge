# AGENTS.md - Setup Agent

이 디렉터리는 테스팅 도구 설치와 초기 설정을 관리한다.

## 책임 범위

| 항목 | 역할 |
|---|---|
| `setup/AGENTS.md` | setup 작업의 메인 agent 규칙 |
| `setup/manifest.yml` | 전체 의도, enabled tool, 위험도, 산출물 선언 |
| `setup/manifest.yml stages` | OS별 setup stage와 package manager/risk notes 선언 |
| `setup/run.sh` | manifest를 읽고 선언된 작업만 실행하는 executor |
| `setup/install.sh` | `setup/run.sh install` wrapper |
| `setup/verify.sh` | `setup/run.sh verify` wrapper |
| `setup/TROUBLESHOOTING.md` | 환경별 연결/실행 문제와 예외 처리 기록 |
| `setup/state/**` | assess/plan/install/verify/smoke 실행 결과 |
| `setup/reports/**` | 사람이 읽는 분석/생성 보고서 |
| `tests/testing-policy.yml` | 도구 독립적인 smoke/scenario/draft 기준 |
| `tests/testing-policy.schema.json` | testing policy 검증 스키마 |
| `setup/tools/<tool>/AGENTS.md` | 툴별 sub-agent 규칙 |
| `setup/tools/<tool>/tool.yml` | 툴별 목적, 버전관리, 위험도, 산출물 선언 |
| `setup/tools/<tool>/install.sh` | 툴별 설치/scaffold executor |
| `setup/tools/<tool>/verify.sh` | 툴별 검증 executor |

## 규칙

| 원칙 | 지침 |
|---|---|
| 대상 환경 | Unix 계열 shell을 우선한다. Linux/Ubuntu shell, WSL 내부 Linux shell, macOS shell만 지원 범위로 본다. |
| Stage 우선 | OS별 차이는 script 내부 분기만 믿지 말고 `setup/manifest.yml`의 `stages`를 먼저 확인한다. |
| 선언 우선 | setup 변경 전 `setup/manifest.yml`을 먼저 읽고 수정한다. |
| 재실행 가능성 | 설치 스크립트는 idempotent하게 작성한다. 이미 생성된 파일은 가능한 한 건너뛴다. |
| 비밀값 | 비밀번호, JWT, OAuth secret, API key를 setup 파일이나 예시 collection에 직접 쓰지 않는다. |
| 실행 기록 | 실행 결과는 `setup/state/*.json`에 기록한다. state JSON은 commit하지 않는다. |
| Policy 우선 | Bruno/k6 산출물보다 `tests/testing-policy.yml`을 먼저 본다. |
| OpenAPI 우선 | `openapi.*` 또는 `swagger.*`가 있으면 해당 `paths`를 source of truth로 보고 controller route scan은 fallback으로만 사용한다. |
| Bruno 형식 | Bruno collection은 native `.bru` 형식을 사용한다. `bruno.json`, `collection.bru`, `folder.bru`, request `*.bru`가 기준이다. |
| 기본 baseUrl | setup 기본 `baseUrl`은 `http://localhost:8080`으로 고정한다. 예외 환경은 `BASE_URL`, Bruno `local.bru`, Bruno `--env-var baseUrl=...`, 또는 k6 `tests/k6/env.js`로 명시적으로 override한다. |
| k6 리포트 | k6 script는 `tests/k6/report.js`와 `handleSummary()`로 markdown/json 성능 리포트를 자동 생성한다. |
| 수동 보호 | Agent는 `generated/` 영역만 자동 수정하고 `manual/` 영역은 보호한다. |
| 성능 테스트 | k6는 성능/부하 테스트 전용이다. API smoke/scenario 테스트는 Bruno 영역에 둔다. |
| 확장 방식 | 새 툴은 `setup/tools/<tool>/` module로 추가하고 manifest에 등록한다. |

## 환경 사전 점검

setup executor는 Unix shell을 기준으로 한다. WSL이나 새 서버의 non-interactive shell에서는 shell startup file이 로드되지 않아 `nvm` Node가 PATH에 잡히지 않을 수 있다. 특히 `assess`에서 `npm: present`, `node: missing`이 함께 보이면 Windows npm이 WSL PATH로 새어 들어왔거나 `nvm`이 아직 로드되지 않은 상태로 본다.

이 경우 임의로 Windows npm으로 Bruno를 설치하지 말고, 같은 shell에서 Node manager를 먼저 로드한 뒤 전체 순서를 다시 실행한다.

```bash
source "$HOME/.nvm/nvm.sh" 2>/dev/null || true
source ./setup/env.sh 2>/dev/null || true
./setup/run.sh assess
```

도구 설치까지 요청된 경우에도 `assess`, `install`, `verify`, `sync-policy`, `generate-tests`는 같은 방식으로 `nvm`과 `setup/env.sh`를 로드한 shell에서 실행한다.

```bash
source "$HOME/.nvm/nvm.sh" 2>/dev/null || true
source ./setup/env.sh 2>/dev/null || true
./setup/run.sh install
./setup/run.sh verify
```

## Git/ignore 상태 해석

| 상태 | 해석 |
|---|---|
| `tests/`가 `.gitignore`로 ignored | 의도된 상태다. generated Bruno/k6 산출물과 policy 초안은 로컬 재생성 산출물로 취급한다. setup이 생성하는 `tests/**` 전체는 재생성 가능한 로컬 산출물로 취급한다. |
| `tests/bruno/api/environments/local.bru` ignored | 의도된 상태다. secret이 들어갈 수 있으므로 commit하지 않는다. |
| `setup/state/**` ignored | 의도된 상태다. 실행 기록용이며 commit하지 않는다. |
| `setup/reports/**` ignored | 의도된 상태다. 사람이 읽는 실행 보고서지만 run output으로 취급한다. |

## 라우팅

| 요청/작업 | 라우팅 |
|---|---|
| Bruno 설치, API 수동 테스트, smoke collection | `setup/tools/bruno/`, `tests/bruno/` |
| k6 설치, 성능 테스트, 부하 테스트 | `setup/tools/k6/`, `tests/k6/` |
| 전체 환경 진단 | `setup/run.sh assess` |
| 전체 설치 계획 확인 | `setup/run.sh plan` |
| enabled tool 병렬 설치 | `setup/run.sh install` |
| 전체 scaffold/도구 검증 | `setup/run.sh verify` |
| 프로젝트 endpoint 후보 수집 | `setup/run.sh analyze-project` |
| policy 동기화 | `setup/run.sh sync-policy` |
| generated 테스트/SQL draft 생성 | `setup/run.sh generate-tests` |
| smoke 테스트 자동 실행 | `setup/run.sh run-smoke` |
| smoke/scenario/draft 분류 판단 | `tests/testing-policy.yml` |
| 툴별 세부 정책 수정 | 해당 `setup/tools/<tool>/AGENTS.md` |

## OpenAPI 우선 분석

`./setup/run.sh analyze-project`는 먼저 `openapi.yml`, `openapi.yaml`, `openapi.json`, `swagger.yml`, `swagger.yaml`, `swagger.json`을 찾는다. 하나 이상 발견되면 inventory의 `discoveryMode`를 `openapi`로 기록하고 controller route scan을 건너뛴다. 이 경로는 큰 Spring 프로젝트에서 분석 시간을 줄이기 위한 기본 경로다.

`./setup/run.sh sync-policy`는 OpenAPI `paths`에서 method/path를 읽어 `tests/testing-policy.yml`을 만든다. OpenAPI endpoint를 파싱하지 못했거나 OpenAPI 파일이 없을 때만 controller 후보를 fallback으로 사용한다.

## 기본 프롬프트 계약

사용자가 다음처럼 요청하면 기본 초기 테스트 환경 세팅으로 해석한다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅해줘.
```

이 요청은 도구 설치보다 프로젝트 분석과 generated 테스트 생성까지의 재현 가능한 세팅을 우선한다. CLI 설치 검증은 도구 설치 요청이 있거나 CLI가 이미 설치된 경우에만 수행한다.

| 단계 | 명령 | 목적 | 산출물 |
|---|---|---|---|
| 1 | 읽기 | `setup/AGENTS.md`, `setup/manifest.yml`, `tests/testing-policy.yml` 확인 | 판단 근거 |
| 2 | `./setup/run.sh assess` | 현재 OS/stage/도구 상태 진단 | `setup/state/assess-result.json` |
| 3 | `./setup/run.sh plan` | enabled tool과 stage 확인 | `setup/state/plan-result.json` |
| 4 | `./setup/run.sh analyze-project` | OpenAPI 우선 endpoint 후보 수집, 없으면 controller fallback | `setup/state/project-inventory.json` |
| 5 | `./setup/run.sh sync-policy` | OpenAPI paths 또는 controller 후보 기준 policy 동기화 | `setup/state/policy-sync-result.json` |
| 6 | `./setup/run.sh generate-tests` | Bruno/k6 generated 산출물과 JPA SQL draft 생성 | `setup/state/generated-tests-result.json` |
| 7 | 조건부 `./setup/run.sh verify` | CLI가 이미 있으면 설치/scaffold 검증 | `setup/state/verify-result.json` |
| 8 | 보고 | endpoint/policy/generated 결과, CLI 설치 필요 여부, 필요한 env/secret, 실행 명령 정리 | 최종 응답 |

`./setup/install.sh`는 기본 프롬프트에서 자동 실행하지 않는다. Bruno/k6 CLI 설치는 global npm install, apt repository, sudo, Homebrew, network 같은 중간/높은 위험 작업을 포함할 수 있으므로 사용자가 "도구 설치까지" 또는 "install까지"라고 명시했을 때만 실행한다.

## 도구 설치 프로토콜

| 단계 | 명령 | 목적 | 산출물 |
|---|---|---|---|
| 1 | 읽기 | `setup/manifest.yml`에서 enabled tool, risk, outputs 확인 | 판단 근거 |
| 2 | `./setup/run.sh assess` | 현재 OS/stage/테스팅 도구 상태 진단 | `setup/state/assess-result.json` |
| 3 | `./setup/run.sh plan` | OS stage와 실행 대상 확인 | `setup/state/plan-result.json` |
| 4 | `./setup/run.sh install` | enabled tool 설치/scaffold 생성 | `setup/state/install-result.json` |
| 5 | `./setup/run.sh verify` | 설치와 scaffold 검증 | `setup/state/verify-result.json` |
| 6 | 보고 | 경고, 보류 사유, 사용자가 직접 입력할 secret 정리 | 최종 응답 |

사용자가 다음처럼 요청하면 도구 설치까지 포함한다.

```text
setup/AGENTS.md 기준으로 초기 테스트 환경을 세팅하고 도구 설치까지 해줘.
```

이 경우 순서는 `assess -> plan -> install -> verify -> analyze-project -> sync-policy -> generate-tests -> verify`다.

## 프로젝트 분석 프로토콜

| 단계 | 명령/파일 | 목적 | 산출물 |
|---|---|---|---|
| 1 | `./setup/run.sh analyze-project` | OpenAPI 파일 우선 수집, OpenAPI가 없을 때만 Controller/route 후보 수집 | `setup/state/project-inventory.json` |
| 2 | `./setup/run.sh sync-policy` | OpenAPI paths 우선, 기존 class 보존, 새 endpoint 추가, stale/conflict 표시 | `setup/state/policy-sync-result.json` |
| 3 | `tests/testing-policy.yml` | endpoint별 `class`, `confidence`, `reason`, `reviewRequired` 판단 | policy diff |
| 4 | `./setup/run.sh generate-tests` | policy 기준 Bruno/k6 generated 산출물과 JPA SQL draft 생성 | `setup/state/generated-tests-result.json` |
| 5 | `tests/bruno/api/generated` | policy 기준 Bruno generated request 생성 | generated collection |
| 6 | `tests/k6/env.js`, `tests/k6/report.js`, `tests/k6/generated` | k6 기본 환경 모듈, 리포트 helper, policy 기준 k6 smoke script 생성 | env module, report helper, generated script |
| 7 | `tests/sql/generated`, `tests/sql/manual` | JPA entity 기반 SQL draft 생성, manual SQL 보호 | SQL draft |
| 8 | `setup/reports/testing-setup-report.md` | 낮은 confidence, reviewRequired, 보류 사유 정리 | report |

## Smoke 실행 프로토콜

| 단계 | 명령/파일 | 목적 | 산출물 |
|---|---|---|---|
| 1 | `BASE_URL` 확인 | 기본값은 `http://localhost:8080` | 실행 대상 |
| 2 | `./setup/run.sh run-smoke` | Bruno smoke와 k6 smoke 자동 실행 | `setup/state/smoke-run-result.json` |
| 3 | `setup/reports/testing-run-report.md` | 결과, 병목, 개선 방향 정리 | report |

`run-smoke`는 secret이 들어갈 수 있는 `tests/bruno/api/environments/local.bru`를 읽지 않는다. 대신 `BASE_URL`만 담은 임시 env를 `setup/state/` 아래에 만들고, generated smoke만 실행한다. `BASE_URL`을 지정하지 않으면 항상 `http://localhost:8080`을 사용한다. k6 script의 지속 기본값은 `tests/k6/env.js`에 두고, run-smoke와 CI는 shell env로 override한다. k6 단독 실행은 `handleSummary()`로 `docs/performance/`와 `docs/performance/k6/`에 성능 리포트를 생성한다. CI에서 실패 코드를 원하면 `SMOKE_FAIL_ON_ERROR=true ./setup/run.sh run-smoke`를 사용한다.

## 위험도 판단

| 작업 | 위험도 | 처리 |
|---|---|---|
| scaffold 생성 | 낮음 | manifest/tool module에 선언돼 있으면 자동 진행 가능 |
| 새 generated 파일 생성 | 낮음 | `generated/` 아래에만 생성 |
| 기존 policy에 새 endpoint 추가 | 중간 | 기존 class를 유지하고 새 항목은 `reviewRequired` 기준으로 표시 |
| 기존 policy class와 새 추정 class 충돌 | 중간 | 기존 class를 유지하고 `suggestedClass`, `reviewRequired`로 표시 |
| generated 테스트/SQL draft 재생성 | 낮음 | `generated/` 영역만 재생성하고 `manual/`은 건드리지 않음 |
| smoke 테스트 실행 | 낮음 | generated smoke만 실행하고 결과는 state/report에 기록 |
| global npm install | 중간 | Bruno CLI 설치처럼 부작용을 설명하고 선언된 script만 실행 |
| apt repository 추가, sudo 사용 | 높음 | k6 Linux 설치처럼 부작용과 필요 권한을 명확히 설명 |
| Homebrew install | 중간 | macOS에서 Homebrew 상태를 먼저 확인 |
| manual 파일 수정 | 높음 | 자동 수정하지 않고 보고서에 보류 사유로 남김 |
| secret 누락 | 높음 | 자동 생성하지 말고 사용자가 직접 채워야 할 항목으로 보고 |
| 실패 발생 | 중간 | 임의 우회하지 말고 tool `AGENTS.md`, `tool.yml`, command output, state JSON 기준으로 분류 |
| 완료 보고 | 낮음 | `assess`, `plan`, `verify` state와 install 결과 또는 보류 사유를 근거로 한다 |

## 완료 기준

| 기준 | 필요 여부 |
|---|---|
| `setup/state/assess-result.json` | 필수 |
| `setup/state/plan-result.json` | 필수 |
| 설치가 필요한 툴의 install 결과 또는 명확한 보류 사유 | 필수 |
| `setup/state/verify-result.json` | 도구 설치 요청 또는 CLI가 이미 있는 경우 필수 |
| 남은 경고 정리 | 필수 |
| 사용자가 직접 입력해야 할 secret 정리 | 필요 시 |
| `setup/state/project-inventory.json` | 프로젝트 분석 시 필수 |
| `setup/state/policy-sync-result.json` | policy 동기화 시 필수 |
| `setup/state/generated-tests-result.json` | 테스트 생성 시 필수 |
| `setup/reports/testing-setup-report.md` | 프로젝트 분석 시 필수 |
| `setup/state/smoke-run-result.json` | smoke 실행 시 필수 |
| `setup/reports/testing-run-report.md` | smoke 실행 시 필수 |
