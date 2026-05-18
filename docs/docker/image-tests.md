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

## 2026-05-18 Dockerfile.runtime layered jar 검증

`Dockerfile.runtime`을 Spring Boot 3.5의 `jarmode=tools` 기반 layer extraction 방식으로 변경한 뒤, 실제 runtime 이미지가 빌드되고 실행 가능한지 확인했다.

### 변경 후 runtime 이미지 구조

변경된 runtime Dockerfile은 `docker-build/app.jar`를 extractor stage에서 다음 명령으로 분해한다.

```bash
java -Djarmode=tools -jar application.jar extract --layers --destination extracted
```

그 뒤 runtime stage는 다음 레이어를 순서대로 복사한다.

| 레이어 | 목적 |
| --- | --- |
| `dependencies` | 외부 dependency jar |
| `spring-boot-loader` | Spring Boot loader |
| `snapshot-dependencies` | SNAPSHOT dependency jar |
| `application` | 애플리케이션 클래스와 리소스 |

최종 runtime 이미지는 fat jar 하나(`/app/app.jar`) 대신 `/app/application.jar`와 `/app/lib` extracted layout으로 실행한다.

### 검증 결과

| 항목 | 결과 | 근거 |
| --- | --- | --- |
| `:app:bootJar` | PASS | `bash gradlew :app:bootJar -PexcludeTags=integration --build-cache --no-daemon` |
| jar layer index | PASS | `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application` |
| runtime image build | PASS | `DOCKER_BUILDKIT=1 docker build --progress=plain -f Dockerfile.runtime -t postforge:runtime-layered-check .` |
| runtime image | PASS | `sha256:552c1e628a25`, `416081559` bytes |
| extracted layout | PASS | `/app/application.jar`, `/app/lib` 존재 |
| Java runtime | PASS | Temurin OpenJDK `21.0.11` |
| `/actuator/health` smoke | PASS | 임시 PostgreSQL/PgVector + Redis에서 `{"status":"UP"}` |

스모크 테스트는 임시 Docker network를 만들고, `application-prod.yml`의 datasource host가 `postgres`로 고정되어 있으므로 PostgreSQL 컨테이너에 `postgres` network alias를 부여해서 진행했다. 테스트 후 임시 컨테이너와 network는 정리했다.

### fat jar runtime 방식과 비교

비교 대상은 같은 `app/build/libs/app.jar`를 `docker-build/app.jar`로 복사한 뒤 빌드했다. `fat jar` 기준은 이전 `Dockerfile.runtime`과 같은 구조를 보관한 `dockerfiles/cache-ab/Dockerfile.after`이고, `layered` 기준은 변경 후 `Dockerfile.runtime`이다. 아래 값은 2026-05-18 현재 WSL/Linux Docker 환경에서의 단일 측정값이므로, 2026-05-12 macOS/arm64 측정값과 직접 비교하지 않는다.

| 항목 | fat jar runtime | layered runtime | 해석 |
| --- | ---: | ---: | --- |
| 빌드 입력 jar | `docker-build/app.jar` 94MB | `docker-build/app.jar` 94MB | 동일 산출물 사용 |
| 이미지 크기 | `416365796` bytes | `416081559` bytes | 사실상 동일 |
| warm build | 0.57s | 0.61s | 완전 캐시 상태에서는 차이 없음 |
| `--no-cache` build | 1.40s | 2.72s | layered는 jar extraction 비용이 추가됨 |
| 애플리케이션 레이어 | `COPY docker-build/app.jar` 98.2MB 단일 레이어 | `dependencies` 97.6MB + `application` 331kB 분리 | 코드 변경 시 최종 이미지 레이어 재사용에 유리 |
| 실행 파일 배치 | `/app/app.jar` | `/app/application.jar`, `/app/lib` | layered는 extracted layout 실행 |

비교 해석:

- 이 변경의 목적은 이미지 크기 축소나 로컬 `--no-cache` 빌드 속도 개선이 아니다.
- fat jar 방식은 jar 하나를 복사하므로 애플리케이션 코드만 바뀌어도 최종 이미지의 약 98MB jar 레이어가 새로 만들어진다.
- layered 방식은 최종 이미지에서 dependency와 application 레이어가 분리된다. 의존성이 그대로이고 애플리케이션 코드/리소스만 바뀌는 배포에서는 큰 dependency 레이어를 registry push/pull에서 재사용할 수 있고, 작은 application 레이어만 바뀌는 형태가 된다.
- Dockerfile build 과정에서는 `docker-build/app.jar`가 바뀌면 extractor stage가 다시 실행될 수 있다. 따라서 이 최적화는 "Docker build 명령 자체가 항상 빨라진다"가 아니라 "최종 이미지 레이어 구조가 배포 캐시에 유리해진다"로 보는 편이 정확하다.

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
