# AGENTS.md - Bruno Setup Agent

이 디렉터리는 Bruno CLI 설치와 범용 API collection scaffold 생성을 담당한다.

## 책임 범위

| 항목 | 역할 |
|---|---|
| `setup/tools/bruno/AGENTS.md` | Bruno setup sub-agent 규칙 |
| `setup/tools/bruno/tool.yml` | Bruno 목적, 버전관리, 위험도, 산출물 선언 |
| `setup/tools/bruno/install.sh` | Bruno CLI 설치와 collection scaffold 생성 |
| `setup/tools/bruno/verify.sh` | Bruno CLI와 collection scaffold 검증 |
| `tests/bruno/AGENTS.md` | Bruno collection 작성 규칙 |
| `tests/bruno/api/**` | Bruno collection 산출물 |
| `tests/bruno/api/generated/**` | Agent가 policy 기준으로 재생성할 수 있는 request |
| `tests/bruno/api/manual/**` | 사람이 직접 관리하는 request |

## 실행 명령

| 목적 | 명령 |
|---|---|
| 설치/scaffold | `./setup/tools/bruno/install.sh` |
| 검증 | `./setup/tools/bruno/verify.sh` |
| 전체 setup 설치에서 실행 | `./setup/run.sh install` |
| 전체 setup 검증에서 실행 | `./setup/run.sh verify` |

## 규칙

| 원칙 | 지침 |
|---|---|
| 설치 방식 | 자동 설치는 CLI 중심으로 유지한다. Desktop App 설치는 문서 안내만 한다. |
| 패키지 | CLI는 `@usebruno/cli`를 npm global package로 설치한다. |
| 대상 환경 | Unix 계열 shell PATH에서 `bru`가 실행 가능해야 한다. |
| 버전 | `BRUNO_CLI_VERSION`이 있으면 해당 버전을 설치하고, 없으면 `latest`를 사용한다. |
| 산출물 | collection 산출물은 `tests/bruno/api` 아래에 둔다. |
| Policy 우선 | request 생성/분류 전 `tests/testing-policy.yml`을 먼저 읽는다. |
| generated 보호 | 자동 생성/갱신은 `tests/bruno/api/generated` 아래에서만 수행한다. |
| manual 보호 | `tests/bruno/api/manual` 아래 파일은 자동으로 덮어쓰지 않는다. |
| 비밀값 | `environments/local.bru`는 로컬 전용이며 commit하지 않는다. |
| 예시 환경 | `environments/local.example.bru`에는 빈 값 또는 안전한 기본값만 둔다. |
| smoke 요청 | 기본 smoke 요청은 빠르고 안정적인 API만 포함한다. |

## 위험도 판단

| 작업 | 위험도 | 처리 |
|---|---|---|
| collection scaffold 생성 | 낮음 | 이미 존재하는 파일은 건너뛴다. |
| npm global install | 중간 | `tool.yml`의 risk를 근거로 설명하고 선언된 script만 실행한다. |
| secret 입력 | 높음 | 자동 생성하지 않고 사용자가 직접 채우도록 안내한다. |

## 완료 기준

| 기준 | 필요 여부 |
|---|---|
| `bru` CLI가 Unix shell PATH에서 실행 가능 | 필수 |
| `tests/bruno/api` 존재 | 필수 |
| `tests/bruno/api/generated` 존재 | 필수 |
| `tests/bruno/api/manual` 존재 | 필수 |
| `tests/bruno/api/environments/local.example.bru` 존재 | 필수 |
| `tests/bruno/api/environments/local.bru`가 ignore 대상 | 필수 |
