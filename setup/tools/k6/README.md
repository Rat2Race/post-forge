# k6 Setup

k6는 API의 성능 테스트와 부하 테스트를 위한 도구다.

## 설치

```bash
./setup/tools/k6/install.sh
```
설치 스크립트는 다음을 수행한다.

- macOS에서는 Homebrew가 있으면 `brew install k6`를 사용한다.
- Linux/WSL 계열에서는 apt 기반 설치를 시도한다. `sudo` 권한과 네트워크가 필요할 수 있다.
- sudo가 대화형 비밀번호를 요구하면 GitHub release binary를 `~/.local/bin/k6`에 설치한다.

## 검증

```bash
./setup/tools/k6/verify.sh
```

## 예시 실행

대상 앱이 실행 중일 때 `BASE_URL`을 지정해서 실행한다. 필요하면 `SMOKE_PATH`도 지정한다.

```bash
BASE_URL=http://localhost:8080 SMOKE_PATH=/ k6 run tests/k6/generated/smoke.js
```

`generated/` 아래 script는 `tests/testing-policy.yml`을 기준으로 Agent가 갱신할 수 있다. 사람이 직접 다듬는 baseline/stress script는 `manual/` 아래에 둔다.
