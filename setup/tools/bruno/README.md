# Bruno Setup

Bruno는 API 수동 탐색과, 선별된 요청을 CLI 기반 smoke/scenario 검증으로 승격하기 위한 도구다.

## 설치

```bash
./setup/tools/bruno/install.sh
```

설치 스크립트는 다음을 수행한다.

- Node/npm 확인
- Bruno CLI 설치 확인 및 필요 시 user-local npm prefix에 `@usebruno/cli` 설치
- user-local 설치 시 현재 실행 프로세스에서만 PATH 반영
- `tests/bruno/api` scaffold 생성
- Bruno native collection root인 `bruno.json`, `collection.bru` 생성
- `tests/bruno/api/generated`와 `tests/bruno/api/manual` 영역 생성

## 검증

```bash
./setup/tools/bruno/verify.sh
```

## 수동 실행

기본 `baseUrl`은 `http://localhost:8080`이다. 새 scaffold의 `local.example.bru`와 새로 생성되는 `local.bru`는 이 값을 사용한다. 다른 주소를 계속 쓰려면 `tests/bruno/api/environments/local.bru`의 `baseUrl:`만 수정한다. 인증이 필요한 수동 request를 만들 때는 `accessToken` 같은 로컬 전용 값을 `local.bru`에 채운다. `local.bru`는 local-only 파일이며 commit하지 않는다.

설치 스크립트는 shell startup file을 수정하지 않는다. `bru`를 직접 실행해야 하면 repo root에서 user-local PATH를 먼저 로드한다.

```bash
source ./setup/env.sh
bru --version
```

global npm 설치는 명시적으로 요청했을 때만 사용한다.

```bash
BRUNO_INSTALL_MODE=system ./setup/tools/bruno/install.sh
```

전체 setup wrapper로 generated smoke만 실행하려면 repo root에서 실행한다. 이 명령은 `tests/bruno/api/environments/local.bru`를 읽지 않고, `tests/.env` 또는 shell env의 `BASE_URL`만 담은 임시 env를 `setup/state/` 아래에 만들어 사용한다. `BRUNO_FILTER_TAGS`, `BRUNO_EXCLUDE_TAGS`, `BRUNO_VERBOSE`도 `tests/.env`에서 조정할 수 있다.

```bash
BASE_URL=http://127.0.0.1:8080 ./setup/run.sh run-smoke
```

Bruno CLI를 직접 실행하려면 먼저 `./setup/run.sh generate-tests`로 generated request를 최신화한 뒤 collection root에서 실행한다.

```bash
cd tests/bruno/api
bru run --tags=smoke --exclude-tags=draft --env-file ./environments/local.bru
```

특정 request 하나만 실행한다.

```bash
cd tests/bruno/api
bru run generated/smoke/001-get-posts.bru --env-file ./environments/local.bru
```

review가 필요한 draft/scenario 요청을 수동으로 확인한다. generated request는 기본적으로 `auth: none`으로 생성되므로, 인증이 필요한 API는 `manual/` 아래에 별도 request를 만들고 로컬 env 값을 연결한다.

```bash
cd tests/bruno/api
bru run generated/draft -r --env-file ./environments/local.bru
bru run generated/scenario -r --env-file ./environments/local.bru
```

사람이 관리하는 manual request만 실행한다.

```bash
cd tests/bruno/api
bru run manual -r --env-file ./environments/local.bru
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

`generated/` 아래 request는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 request는 `manual/` 아래에 둔다. `tests`를 통째로 삭제하고 setup을 다시 돌리면 manual 파일도 사라지므로, 보존해야 하는 수동 request는 커밋하거나 별도 백업한 뒤 재생성한다.
