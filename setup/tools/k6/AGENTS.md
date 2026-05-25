# AGENTS.md - k6 Setup Agent

이 디렉터리는 k6 설치와 성능 테스트 scaffold 생성을 담당한다.

## 책임 범위

| 항목 | 역할 |
|---|---|
| `setup/tools/k6/AGENTS.md` | k6 setup sub-agent 규칙 |
| `setup/tools/k6/tool.yml` | k6 목적, 버전관리, 위험도, 산출물 선언 |
| `setup/tools/k6/install.sh` | k6 설치와 performance script scaffold 생성 |
| `setup/tools/k6/run.sh` | k6 script 간편 실행과 리포트 기본값 지정 |
| `setup/tools/k6/verify.sh` | k6 CLI와 smoke script 검증 |
| `setup/run-k6` | setup package 안에서 쓰는 k6 간편 실행 shortcut |
| `tests/.env` | 테스트 툴 공통 로컬 환경값 |
| `tests/k6/AGENTS.md` | k6 test script 작성 규칙 |
| `tests/k6/**` | k6 성능 테스트 산출물 |
| `tests/k6/generated/**` | Agent가 policy 기준으로 재생성할 수 있는 script |
| `tests/k6/manual/**` | 사람이 직접 관리하는 script |

## 실행 명령

| 목적 | 명령 |
|---|---|
| 설치/scaffold | `./setup/tools/k6/install.sh` |
| 검증 | `./setup/tools/k6/verify.sh` |
| setup shortcut 실행 | `./setup/run-k6 [smoke|manual|<script>]` |
| 간편 실행 | `./setup/tools/k6/run.sh [smoke|manual|<script>]` |
| 전체 setup 설치에서 실행 | `./setup/run.sh install` |
| 전체 setup 검증에서 실행 | `./setup/run.sh verify` |
| 전체 setup wrapper에서 간편 실행 | `./setup/run.sh run-k6 [smoke|manual|<script>]` |
| smoke script 실행 | `BASE_URL=<target-url> k6 run tests/k6/generated/smoke.js` |

## 규칙

| 원칙 | 지침 |
|---|---|
| 도구 목적 | k6는 성능/부하 테스트 전용으로 둔다. |
| 역할 분리 | API smoke나 기능 시나리오 테스트를 k6로 대체하지 않는다. |
| Policy 우선 | k6 script 생성 전 `tests/testing-policy.yml`을 먼저 읽는다. |
| generated 보호 | 자동 생성/갱신은 `tests/k6/generated` 아래에서만 수행한다. |
| manual 보호 | `tests/k6/manual` 아래 파일은 자동으로 덮어쓰지 않는다. |
| macOS 설치 | Homebrew가 있으면 `brew install k6`를 우선한다. |
| Linux/WSL 설치 | apt 기반 설치를 우선한다. |
| 타깃 선택 | script는 `BASE_URL` 환경변수로 대상 서버를 선택한다. |
| 공통 env | `tests/.env`를 로드하되 shell env와 CLI 옵션을 우선한다. 빈 값은 도구 기본값으로 fallback한다. |
| 운영 보호 | 운영 환경 고부하 테스트는 명시적 승인 없이 실행하지 않는다. |

## 위험도 판단

| 작업 | 위험도 | 처리 |
|---|---|---|
| `tests/k6` scaffold 생성 | 낮음 | 이미 존재하는 파일은 건너뛴다. |
| Homebrew install | 중간 | macOS에서 Homebrew 상태를 먼저 확인한다. |
| apt repository 추가 | 높음 | sudo와 repo 추가 부작용을 명확히 설명한다. |
| 고부하 테스트 실행 | 높음 | 운영 환경에는 명시적 승인 없이 실행하지 않는다. |

## 완료 기준

| 기준 | 필요 여부 |
|---|---|
| `k6` CLI가 Unix shell PATH에서 실행 가능 | 필수 |
| `tests/k6/generated/smoke.js` 존재 | 필수 |
| `tests/k6/manual` 존재 | 필수 |
| `tests/.env` 존재 | 필수 |
| `setup/run-k6`, `setup/tools/k6/run.sh` 실행 가능 | 필수 |
| smoke script가 `BASE_URL`을 사용 | 필수 |
| 운영 고부하 실행 금지 규칙 유지 | 필수 |
