# Docker Cache A/B

작성일: 2026-05-12 KST

## 목적

git 기록의 `b2cb42aa refactor: CI 성능 개선` 커밋을 기준으로, Docker/Gradle 캐시 적용 전후의 빌드 시간, 이미지 크기, CI 비용 proxy를 비교한다.

이 검증은 현재 작업트리의 기존 `Dockerfile`, `Dockerfile.runtime`을 수정하지 않고 `/private/tmp/postforge-cache-compare`에 각 커밋을 archive로 풀어서 수행했다.

## 비교 기준

| 구분 | 커밋 | 빌드 방식 |
| --- | --- | --- |
| before | `6a87b716ade9f50098df3e79dea606d2bd19dd4d` (`b2cb42aa^`) | `Dockerfile` 안에서 Gradle 빌드까지 수행한 뒤 runtime 이미지 생성 |
| after | `b2cb42aad560a0149608e5c4229687175fc83264` | `gradle :app:bootJar --build-cache`로 JAR 생성 후 `Dockerfile.runtime`으로 runtime 이미지 생성 |

## 관리 파일

메인 `Dockerfile`은 유지하고, A/B 재현용 Dockerfile은 `dockerfiles/cache-ab` 아래에 분리했다.

| 파일 | 용도 |
| --- | --- |
| `dockerfiles/cache-ab/Dockerfile.before` | before: Docker 안에서 Gradle 빌드까지 수행하는 최종 이미지 Dockerfile |
| `dockerfiles/cache-ab/Dockerfile.after-build` | after: `b2cb42aa`에 남아 있던 build-stage-only Dockerfile 보관본 |
| `dockerfiles/cache-ab/Dockerfile.after` | after: Gradle로 만든 `docker-build/app.jar`를 복사하는 runtime 이미지 Dockerfile |

`after` 커밋의 핵심 변화:

- Docker 이미지 빌드 전에 Gradle 빌드를 분리
- Gradle build cache 사용
- `Dockerfile.runtime`으로 이미 만들어진 `docker-build/app.jar`만 runtime 이미지에 복사
- CI에서 Docker build cache를 사용할 수 있는 구조로 변경

## 검증 환경

| 항목 | 값 |
| --- | --- |
| Docker | Client/Server 29.4.0 |
| Buildx | v0.33.0-desktop.1 |
| Docker host | Docker Desktop, linux/arm64 |
| 측정 도구 | `hyperfine 1.20.0` |
| 반복 횟수 | 각 항목 `--runs 3` |
| 임시 경로 | `/private/tmp/postforge-cache-compare` |

Docker credential helper의 macOS keychain 상호작용을 피하기 위해 측정 명령에는 임시 `HOME`, `DOCKER_CONFIG`, `DOCKER_HOST`를 사용했다.

## 측정 시나리오

| 시나리오 | 의미 |
| --- | --- |
| `--no-cache` | Docker layer cache를 쓰지 않는 빌드. after는 커밋 의도대로 Gradle build cache는 유지한다. |
| 변경 없음 | 소스 변경 없이 다시 실행하는 완전 warm cache 상황 |
| 리소스 변경 | `app/src/main/resources/docker-build-benchmark.txt`만 매 run 변경한 재빌드 |

비용 proxy는 실제 과금액이 아니라 CI billable time 관점의 소요 분(`평균 초 / 60`)으로 기록했다.

## 결과

| 시나리오 | before 평균 | after 평균 | 변화 | before 비용 proxy | after 비용 proxy |
| --- | ---: | ---: | ---: | ---: | ---: |
| `--no-cache` | 35.554s ± 0.323s | 6.070s ± 0.349s | 82.9% 빠름 | 0.593분 | 0.101분 |
| 변경 없음 | 0.237s ± 0.056s | 3.505s ± 0.349s | 1378.3% 느림 | 0.004분 | 0.058분 |
| 리소스 변경 | 7.585s ± 0.020s | 5.623s ± 0.077s | 25.9% 빠름 | 0.126분 | 0.094분 |

해석:

- `--no-cache`에서는 after가 훨씬 빠르다. Docker 안에서 Gradle 전체 빌드를 반복하지 않고, Gradle build cache를 활용한 뒤 가벼운 runtime Dockerfile만 빌드하기 때문이다.
- 변경 없는 완전 warm cache에서는 before가 더 빠르다. before는 Docker layer cache만 확인하면 끝나지만, after는 Gradle 명령 실행과 JAR 복사 단계가 항상 포함된다.
- 리소스 변경 재빌드에서는 after가 약 25.9% 빠르다. 변경된 리소스에 필요한 JAR 재생성만 수행하고 Docker runtime 이미지 빌드는 얇게 끝난다.

## 이미지 크기

| 이미지 | Docker content size | Docker disk usage |
| --- | ---: | ---: |
| before runtime image | 207,434,595 bytes | 674MB |
| after runtime image | 207,434,762 bytes | 674MB |

이미지 크기는 사실상 동일하다. 이 커밋의 이득은 최종 이미지 크기 축소가 아니라 CI 빌드 경로와 재빌드 시간 절감이다.

## 사용한 명령

before, `--no-cache`:

```bash
hyperfine --runs 3 \
  --export-json /private/tmp/postforge-cache-compare/results/before-no-cache.json \
  'env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain -f dockerfiles/cache-ab/Dockerfile.before -t postforge:history-before-no-cache .'
```

before, 변경 없음:

```bash
hyperfine --runs 3 \
  --export-json /private/tmp/postforge-cache-compare/results/before-warm-nochange.json \
  'env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/cache-ab/Dockerfile.before -t postforge:history-before-warm .'
```

before, 리소스 변경:

```bash
hyperfine --warmup 1 --runs 3 \
  --prepare 'date +%s%N > app/src/main/resources/docker-build-benchmark.txt' \
  --export-json /private/tmp/postforge-cache-compare/results/before-resource-change.json \
  'env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/cache-ab/Dockerfile.before -t postforge:history-before-resource .'
```

after, `--no-cache`:

```bash
hyperfine --runs 3 \
  --export-json /private/tmp/postforge-cache-compare/results/after-no-cache.json \
  'env GRADLE_USER_HOME=/private/tmp/postforge-cache-compare/gradle-user-home-after bash gradlew clean :app:bootJar -PexcludeTags=integration --build-cache --no-daemon --project-cache-dir /private/tmp/postforge-cache-compare/gradle-project-cache-after >/dev/null && mkdir -p docker-build && cp app/build/libs/app.jar docker-build/app.jar && env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain -f dockerfiles/cache-ab/Dockerfile.after -t postforge:history-after-no-cache . >/dev/null'
```

after, 변경 없음:

```bash
hyperfine --runs 3 \
  --export-json /private/tmp/postforge-cache-compare/results/after-warm-nochange.json \
  'env GRADLE_USER_HOME=/private/tmp/postforge-cache-compare/gradle-user-home-after bash gradlew :app:bootJar -PexcludeTags=integration --build-cache --no-daemon --project-cache-dir /private/tmp/postforge-cache-compare/gradle-project-cache-after >/dev/null && mkdir -p docker-build && cp app/build/libs/app.jar docker-build/app.jar && env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/cache-ab/Dockerfile.after -t postforge:history-after-warm . >/dev/null'
```

after, 리소스 변경:

```bash
hyperfine --warmup 1 --runs 3 \
  --prepare 'date +%s%N > app/src/main/resources/docker-build-benchmark.txt' \
  --export-json /private/tmp/postforge-cache-compare/results/after-resource-change.json \
  'env GRADLE_USER_HOME=/private/tmp/postforge-cache-compare/gradle-user-home-after bash gradlew :app:bootJar -PexcludeTags=integration --build-cache --no-daemon --project-cache-dir /private/tmp/postforge-cache-compare/gradle-project-cache-after >/dev/null && mkdir -p docker-build && cp app/build/libs/app.jar docker-build/app.jar && env HOME=/private/tmp/codex-home DOCKER_CONFIG=/private/tmp/codex-docker-config DOCKER_HOST=unix:///Users/rat/.docker/run/docker.sock DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/cache-ab/Dockerfile.after -t postforge:history-after-resource . >/dev/null'
```

## 결론

`b2cb42aa`의 캐시/CI 구조 변경은 최종 이미지 크기를 줄이지는 않았지만, Docker layer cache가 깨지는 상황과 리소스 변경 재빌드에서 시간을 줄였다. 다만 변경이 전혀 없는 로컬 warm build만 보면 before가 더 빠르므로, 이 변경의 효과는 "항상 더 빠른 Docker build"가 아니라 "CI에서 JAR 빌드와 runtime 이미지 빌드를 분리해 반복 빌드 비용을 줄이는 것"으로 보는 편이 정확하다.
