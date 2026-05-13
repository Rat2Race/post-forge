# Docker Image Tests

작성일: 2026-05-12 13:23 KST

## 목적

기존 `Dockerfile`은 수정하지 않고, 비교용 Dockerfile 버전별 이미지가 정상적으로 빌드 산출물을 포함하고 실행 가능한지 확인한다.

## 테스트 대상

| 버전 | 파일 | 로컬 이미지 태그 | 기준 |
| --- | --- | --- | --- |
| v0 | `dockerfiles/versions/Dockerfile.v0` | `postforge:v0-check` | 단일 스테이지 빌드 |
| v1 | `dockerfiles/versions/Dockerfile.v1` | `postforge:v1-check` | 멀티 스테이지 빌드 |
| v2 | `dockerfiles/versions/Dockerfile.v2` | `postforge:v2-check` | Gradle 의존성 레이어 분리 |
| v3 | `dockerfiles/versions/Dockerfile.v3` | `postforge:v3-check` | v2 레이아웃 + `.dockerignore` 기반 컨텍스트 축소 |
| v4 | `dockerfiles/versions/Dockerfile.v4` | `postforge:v4-check` | BuildKit cache mount + Alpine Gradle build stage |

원본 `Dockerfile`은 테스트 대상으로만 비교했고, 파일 내용은 변경하지 않았다.

## 검증 환경

| 항목 | 값 |
| --- | --- |
| Docker Client/Server | 29.4.0 / 29.4.0 |
| Docker Buildx | v0.33.0-desktop.1 |
| Docker context | `desktop-linux` |
| Host architecture | arm64 |
| 임시 DB 이미지 | `pgvector/pgvector:0.8.2-pg18-trixie` |
| 임시 Redis 이미지 | `redis:7-alpine` |

## 검증 방법

이미지는 다음 형식으로 버전별 태그를 부여해 준비했다.

```bash
DOCKER_BUILDKIT=1 docker build -f dockerfiles/versions/Dockerfile.vN -t postforge:vN-check .
```

실행 smoke는 기존 로컬 `postforge-db`를 사용하지 않고, 임시 Docker network와 임시 PostgreSQL/PgVector, Redis 컨테이너를 생성해 진행했다. 애플리케이션 기본 설정의 mail health check는 dummy Gmail 계정으로 외부 SMTP 인증을 시도하므로, 이미지 실행 검증과 무관한 `MANAGEMENT_HEALTH_MAIL_ENABLED=false`만 smoke 환경에서 적용했다.

각 버전에서 확인한 항목은 다음과 같다.

- Spring Boot executable jar 존재 여부
- `java -Djarmode=tools -jar ... list-layers`로 jar layer 구조 확인
- `java -version` 실행 가능 여부
- 임시 DB/Redis 연결 후 `GET /actuator/health`가 `UP`을 반환하는지 확인

## 결과

| 버전 | Image ID | 이미지 크기 | jar layer | Java runtime | `/actuator/health` | Health 대기 |
| --- | --- | ---: | --- | --- | --- | ---: |
| v0 | `ebee4af80b59` | 1.78GB | PASS | PASS | PASS | 5s |
| v1 | `6a7ff91124d1` | 668MB | PASS | PASS | PASS | 5s |
| v2 | `72002082d575` | 668MB | PASS | PASS | PASS | 4s |
| v3 | `d8e6d2a326ac` | 668MB | PASS | PASS | PASS | 5s |
| v4 | `dd8051ba6f1c` | 668MB | PASS | PASS | PASS | 5s |

전체 버전이 jar 산출물 확인, Java 런타임 확인, 임시 인프라 기반 health smoke를 통과했다.

## 빌드 시간 재측정

2026-05-12 13:42 KST에 `hyperfine 1.20.0`으로 같은 조건에서 v0-v4의 첫 빌드와 리소스 변경 재빌드를 재측정했다.

측정 기준:

- 첫 빌드: Docker layer cache를 쓰지 않는 `--no-cache` 빌드
- 재빌드: `app/src/main/resources/docker-build-benchmark.txt`를 매번 갱신해 리소스 변경을 만든 뒤 측정
- 각 측정은 `--runs 3` 기준
- 재빌드는 `--warmup 1`을 추가해 측정 전 캐시 상태를 안정화
- 측정 후 `docker-build-benchmark.txt`는 원래 내용으로 복구

| 버전 | 첫 빌드 mean ± σ | 첫 빌드 range | 재빌드 mean ± σ | 재빌드 range | 이미지 크기 |
| --- | ---: | ---: | ---: | ---: | ---: |
| v0 | 38.942s ± 0.424s | 38.454s - 39.221s | 40.129s ± 0.520s | 39.591s - 40.630s | 1.78GB |
| v1 | 34.647s ± 0.770s | 33.799s - 35.301s | 34.626s ± 0.560s | 34.055s - 35.175s | 668MB |
| v2 | 37.013s ± 2.332s | 35.100s - 39.611s | 18.877s ± 0.297s | 18.534s - 19.051s | 668MB |
| v3 | 37.567s ± 1.673s | 35.640s - 38.651s | 19.143s ± 0.401s | 18.680s - 19.386s | 668MB |
| v4 | 36.863s ± 0.717s | 36.090s - 37.508s | 6.314s ± 0.917s | 5.533s - 7.324s | 668MB |

재빌드 기준 개선율:

| 버전 | 재빌드 평균 | v0 대비 |
| --- | ---: | ---: |
| v0 | 40.129s | 기준 |
| v1 | 34.626s | 13.7% 빠름 |
| v2 | 18.877s | 53.0% 빠름 |
| v3 | 19.143s | 52.3% 빠름 |
| v4 | 6.314s | 84.3% 빠름 |

사용한 커맨드:

```bash
for v in 0 1 2 3 4; do
  hyperfine --runs 3 \
    --export-json /tmp/postforge-docker-bench/first-v${v}.json \
    "DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain -f dockerfiles/versions/Dockerfile.v${v} -t postforge:v${v}-first-bench ."
done
```

```bash
for v in 0 1 2 3 4; do
  hyperfine --warmup 1 --runs 3 \
    --prepare 'date +%s%N > app/src/main/resources/docker-build-benchmark.txt' \
    --export-json /tmp/postforge-docker-bench/rebuild-v${v}.json \
    "DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/versions/Dockerfile.v${v} -t postforge:v${v}-rebuild-bench ."
done
```

## 관찰 내용

- v0은 최종 이미지가 Gradle/JDK 빌드 환경까지 포함해서 1.78GB로 가장 크다.
- v1부터 런타임 stage가 `eclipse-temurin:21-jre`로 분리되어 668MB로 줄었다.
- v2와 v3는 최종 런타임 이미지 크기는 v1과 동일하지만, Dockerfile 레이어 구성과 build context 최적화 목적이 다르다.
- v4는 최종 런타임 베이스가 v1-v3와 같아 이미지 크기는 동일하지만, BuildKit cache mount로 재빌드 성능 개선을 노리는 버전이다.

## 재현 명령

이미지 빌드:

```bash
for v in 0 1 2 3 4; do
  DOCKER_BUILDKIT=1 docker build -f dockerfiles/versions/Dockerfile.v${v} -t postforge:v${v}-check .
done
```

이미지 크기 확인:

```bash
docker image ls postforge --format 'table {{.Repository}}:{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedSince}}'
```

jar layer 확인 예시:

```bash
docker run --rm --entrypoint sh postforge:v4-check \
  -c 'test -f /app/app.jar && java -Djarmode=tools -jar /app/app.jar list-layers'
```

v0은 jar 위치가 다르므로 다음 경로를 사용한다.

```bash
docker run --rm --entrypoint sh postforge:v0-check \
  -c 'test -f /workspace/app/build/libs/app.jar && java -Djarmode=tools -jar /workspace/app/build/libs/app.jar list-layers'
```

## 결론

현재 로컬 검증 기준으로 `dockerfiles/versions/Dockerfile.v0`부터 `dockerfiles/versions/Dockerfile.v4`까지 모두 실행 가능한 PostForge 이미지를 만든다. 원본 `Dockerfile`은 수정하지 않았고, 버전별 테스트 결과는 이 문서에 별도로 기록했다.
