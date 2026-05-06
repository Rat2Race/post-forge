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
- Bruno native collection root인 `bruno.json`, `collection.bru` 생성
- `tests/bruno/api/generated`와 `tests/bruno/api/manual` 영역 생성

## 검증

```bash
./setup/tools/bruno/verify.sh
```

## 수동 실행

기본 `baseUrl`은 `http://localhost:8080`이다. 새 scaffold의 `local.example.bru`와 새로 생성되는 `local.bru`는 이 값을 사용한다. 다른 주소를 계속 쓰려면 `tests/bruno/api/environments/local.bru`의 `baseUrl:`만 수정한다. 인증이 필요한 draft/scenario 요청을 실행할 때는 `accessToken`도 채운다. `local.bru`는 local-only 파일이며 commit하지 않는다.

설치 후 새 Unix shell에서는 `bru` 명령이 바로 잡혀야 한다. 현재 열려 있는 shell에서 아직 잡히지 않으면 repo root에서 user-local PATH를 먼저 로드한다.

```bash
source ./setup/env.sh
bru --version
```

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru
```

특정 request 하나만 실행한다.

```bash
cd tests/bruno/api
bru run generated/smoke/001-get-posts.bru --env-file ./environments/local.bru
```

review가 필요한 draft/scenario 요청을 수동으로 확인한다.

```bash
cd tests/bruno/api
bru run generated/draft -r --env-file ./environments/local.bru
bru run generated/scenario -r --env-file ./environments/local.bru
```

로컬 서버 주소만 임시로 바꿔 실행하려면 환경 파일을 수정하지 않고 override할 수 있다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru --env-var baseUrl=http://127.0.0.1:18080
```

Bruno CLI 콘솔 출력은 성공/실패 요약 중심이다. `--verbose`를 붙여도 성공 요청의 request/response 본문이 항상 콘솔에 표시되지는 않는다.

```bash
cd tests/bruno/api
bru run generated/smoke/001-get-posts.bru --env-file ./environments/local.bru --verbose
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

`generated/` 아래 request는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 request는 `manual/` 아래에 둔다.
