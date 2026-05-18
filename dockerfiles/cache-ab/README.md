# Docker Cache A/B Files

These Dockerfiles mirror the files used for the Docker cache A/B comparison in
`docs/docker/cache-ab.md`.

| File | Role |
| --- | --- |
| `Dockerfile.before` | A: before `b2cb42aa`; builds the jar inside Docker and emits the final runtime image |
| `Dockerfile.after-build` | B: after `b2cb42aa`; historical build-stage-only Dockerfile from the commit |
| `Dockerfile.after` | B: after `b2cb42aa`; runtime image Dockerfile used in the A/B benchmark |

Run from the repository root.

```bash
DOCKER_BUILDKIT=1 docker build \
  -f dockerfiles/cache-ab/Dockerfile.before \
  -t postforge:history-before .
```

```bash
./gradlew :app:bootJar -PexcludeTags=integration --build-cache --no-daemon
mkdir -p docker-build
cp app/build/libs/app.jar docker-build/app.jar
DOCKER_BUILDKIT=1 docker build \
  -f dockerfiles/cache-ab/Dockerfile.after \
  -t postforge:history-after .
```
