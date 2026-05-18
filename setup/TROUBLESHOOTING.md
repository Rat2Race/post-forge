# Testing Setup Troubleshooting

이 문서는 기본 setup 흐름을 바꾸지 않고, 특정 로컬 환경에서만 필요한 예외 처리를 기록한다.

## Windows에서 앱 실행, WSL에서 Bruno/k6 실행

일반적인 WSL 개발에서는 프로젝트와 앱 서버를 모두 WSL 안에서 실행하므로 `BASE_URL=http://localhost:8080`이 그대로 동작한다.

다만 Windows에서 앱 서버를 실행하고 WSL에서 `bru`, `k6`, `./setup/run.sh run-smoke`를 실행하면 `localhost` 의미가 달라진다.

| 위치 | `localhost`가 가리키는 곳 |
|---|---|
| Windows 브라우저, PowerShell | Windows host |
| WSL shell | WSL VM 내부 |

그래서 Windows 브라우저에서 `http://localhost:8080`이 정상이어도 WSL에서 아래 명령은 실패할 수 있다.

```bash
curl -I http://127.0.0.1:8080
curl -I http://localhost:8080
```

### Windows host URL 찾기

WSL에서 Windows host는 보통 default gateway 주소로 접근할 수 있다.

```bash
WIN_HOST="$(ip route | awk '/default/ {print $3; exit}')"
echo "$WIN_HOST"
curl -I "http://$WIN_HOST:8080"
```

응답이 오면 해당 주소를 `BASE_URL`로 사용한다.

```bash
BASE_URL="http://$WIN_HOST:8080" ./setup/run.sh run-smoke
```

Bruno를 수동 실행할 때는 환경 파일을 수정하지 않고 `baseUrl`만 override할 수 있다.

```bash
cd tests/bruno/api
WIN_HOST="$(ip route | awk '/default/ {print $3; exit}')"
bru run --tags=smoke --exclude-tags=draft \
  --env-file ./environments/local.bru \
  --env-var baseUrl="http://$WIN_HOST:8080"
```

이 주소를 계속 쓰는 서버라면 `tests/bruno/api/environments/local.bru`의 `baseUrl:`을 같은 값으로 바꾼다. 기본값은 계속 `http://localhost:8080`으로 둔다.

k6를 직접 실행할 때도 같은 값을 쓴다. 일회성 실행은 `BASE_URL`을 넘긴다.

```bash
WIN_HOST="$(ip route | awk '/default/ {print $3; exit}')"
BASE_URL="http://$WIN_HOST:8080" k6 run tests/k6/generated/smoke.js
```

같은 Windows host 주소를 계속 쓰는 환경이면 `tests/k6/env.js`의 `baseUrl`을 `http://$WIN_HOST:8080` 값으로 바꾼다.

### 확인 포인트

- `curl -I "http://$WIN_HOST:8080"`이 `500`을 반환해도 연결은 성공한 것이다. root path(`/`)가 앱에서 실패했을 수 있으므로 smoke endpoint(`/posts` 등)를 별도로 확인한다.
- `curl -I "http://$WIN_HOST:8080/posts"`가 성공하면 WSL에서 Windows 앱 서버로 접근 가능한 상태다.
- Windows 방화벽이나 서버 bind 설정에 따라 gateway 접근이 막힐 수 있다. 이 경우 앱 서버가 `127.0.0.1`에만 bind되어 있는지, Windows 방화벽이 WSL vEthernet 접근을 막는지 확인한다.
- 이 우회는 Windows host에서 앱을 실행하고 WSL에서 테스트만 돌리는 경우의 예외 처리다. 기본 문서의 `localhost` 값을 대체하는 일반 규칙이 아니다.
