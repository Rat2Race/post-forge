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
  README.md
  manifest.yml
  env.sh
  run.sh
  install.sh
  verify.sh
  reports/
  state/
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
  testing-policy.yml
  testing-policy.schema.json
  bruno/api/generated/
  bruno/api/manual/
  k6/generated/
  k6/manual/
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

설치 스크립트가 user-local binary 경로를 사용하면 Unix shell startup file에 PATH를 등록한다. 설치 후 새 Linux/WSL/macOS shell에서는 `bru`, `k6` 명령을 바로 사용할 수 있어야 한다.

설치와 scaffold 상태를 검증한다.

```bash
./setup/verify.sh
```

프로젝트 endpoint 후보를 수집한다. 이 명령은 policy를 직접 분류하지 않고 Agent가 해석할 inventory만 만든다.

```bash
./setup/run.sh analyze-project
```

수집된 inventory를 기준으로 `tests/testing-policy.yml`을 보수적으로 동기화한다. 기존 endpoint class는 보존하고, 새 endpoint만 추가한다.

```bash
./setup/run.sh sync-policy
```

policy를 기준으로 Bruno/k6 generated 산출물을 만든다.

```bash
./setup/run.sh generate-tests
```

앱 서버가 실행 중이면 Bruno/k6 smoke를 자동 실행하고 결과 report를 만든다. `BASE_URL` 기본값은 `http://localhost:8080`이다.

```bash
./setup/run.sh run-smoke
```

다른 주소를 쓰려면:

```bash
BASE_URL=http://localhost:8080 ./setup/run.sh run-smoke
```

`install.sh`, `verify.sh`는 모두 `run.sh`의 wrapper다.

Bruno/k6를 직접 터미널에서 실행할 때 `bru: command not found`처럼 user-local 설치 경로가 잡히지 않으면, 현재 셸에서 다음을 먼저 실행한다.

```bash
source ./setup/env.sh
bru --version
k6 version
```

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
- `generate-tests`는 `generated/` 영역만 재생성한다. Bruno smoke는 `reviewRequired: false`인 smoke만 포함하고, 나머지는 draft/scenario로 분리한다.
- `run-smoke`는 Bruno/k6 smoke를 실행하고 `setup/state/smoke-run-result.json`, `setup/reports/testing-run-report.md`에 결과와 개선 방향을 남긴다.
- `generated/` 아래 파일은 Agent가 자동 갱신할 수 있고, `manual/` 아래 파일은 자동 덮어쓰기 금지다.
- `.gitkeep`만 들어있는 폴더는 의도된 placeholder다. 예를 들어 `manual/`은 사람이 나중에 직접 작성할 테스트를 위한 보호 영역이다.
- `tool.yml`은 툴별 목적, 버전관리 전략, 위험도, 산출물을 선언한다.
- `install.sh`는 도구 설치와 scaffold 생성만 담당한다.
- user-local 설치를 사용한 경우 `install.sh`는 사용자가 Unix shell에서 `bru`, `k6`를 바로 실행할 수 있도록 shell startup file에 PATH를 등록한다.
- `verify.sh`는 앱이 실행 중이라는 가정 없이 가능한 검증만 한다. API smoke 실행은 Bruno CLI와 앱이 준비된 뒤 별도로 수행한다.
- `setup/state/*.json`은 실행 결과 기록용이며 commit하지 않는다.
- `setup/reports/*.md`는 실행 보고서 기록용이며 commit하지 않는다.
