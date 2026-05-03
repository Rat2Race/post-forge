# Bruno Setup

Bruno는 API 수동 탐색과, 선별된 요청을 CLI 기반 smoke/scenario 검증으로 승격하기 위한 도구다.

## 설치

```bash
./setup/tools/bruno/install.sh
```

설치 스크립트는 다음을 수행한다.

- Node/npm 확인
- Bruno CLI 설치 확인 및 필요 시 `npm install -g @usebruno/cli`
- user-local 설치 시 shell startup file에 PATH 등록
- `tests/bruno/api` scaffold 생성
- `tests/bruno/api/generated`와 `tests/bruno/api/manual` 영역 생성

## 검증

```bash
./setup/tools/bruno/verify.sh
```

## 수동 실행

먼저 `tests/bruno/api/environments/local.bru`에 `baseUrl`을 설정한다. 인증이 필요한 draft/scenario 요청을 실행할 때는 `accessToken`도 채운다.

설치 후 새 Unix shell에서는 `bru` 명령이 바로 잡혀야 한다. 현재 열려 있는 shell에서 아직 잡히지 않으면 repo root에서 user-local PATH를 먼저 로드한다.

```bash
source ./setup/env.sh
bru --version
```

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru
```

`generated/` 아래 request는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 request는 `manual/` 아래에 둔다.
