# Docker Build

## 2026-05-12 Dockerfile 버전별 빌드 시간 측정

| 버전 | 첫 빌드 평균 | 재빌드 평균 | 이미지 크기 | 재빌드 개선율 |
| --- | ---: | ---: | ---: | ---: |
| v0 | 38.942s | 40.129s | 1.78GB | 기준 |
| v1 | 34.647s | 34.626s | 668MB | 13.7% 빠름 |
| v2 | 37.013s | 18.877s | 668MB | 53.0% 빠름 |
| v3 | 37.567s | 19.143s | 668MB | 52.3% 빠름 |
| v4 | 36.863s | 6.314s | 668MB | 84.3% 빠름 |

측정 커맨드는 아래 `측정 커맨드` 섹션에 기록했다.

---

### Dockerfile 설명

---
#### 도커 파일 최적화
1. 멀티스테이징 (빌드/실행 분리로 최종 이미지 크기 감소)
2. 의존성 캐싱 (의존성 파일 먼저 복사해서 레이어 캐시 활용)
3. ignore (불필요한 파일 제외로 빌드 컨텍스트 감소)
4. BuildKit (--mount=type=cache로 Gradle 캐시 유지)

#### 버전별 Dockerfile

| 파일 | 기준 |
| --- | --- |
| `dockerfiles/versions/Dockerfile.v0` | 단일 스테이지 |
| `dockerfiles/versions/Dockerfile.v1` | 멀티 스테이지 |
| `dockerfiles/versions/Dockerfile.v2` | 의존성 레이어 캐싱 |
| `dockerfiles/versions/Dockerfile.v3` | v2 + `.dockerignore` 적용 |
| `dockerfiles/versions/Dockerfile.v4` | v3 + BuildKit cache mount |
---

### 버전별 첫 빌드/재빌드 시간 측정 결과

2026-05-12에 `hyperfine`으로 같은 조건에서 재측정했다. 첫 빌드는 Docker layer cache를 쓰지 않는 `--no-cache` 빌드이고, 재빌드는 `app/src/main/resources/docker-build-benchmark.txt`를 매번 갱신해 리소스 변경을 만든 뒤 측정했다.

| 버전 | 변경사항 | 첫 빌드 평균 | 재빌드 평균 | 이미지 크기 | 재빌드 개선율 |
| --- | --- | ---: | ---: | ---: | ---: |
| v0 | 단일 스테이지 | 38.942s | 40.129s | 1.78GB | - (기준) |
| v1 | 멀티 스테이지 | 34.647s | 34.626s | 668MB | 13.7% 빠름 |
| v2 | 의존성 캐싱 | 37.013s | 18.877s | 668MB | 53.0% 빠름 |
| v3 | v2 + `.dockerignore` 적용 | 37.567s | 19.143s | 668MB | 52.3% 빠름 |
| v4 | BuildKit cache mount + Alpine | 36.863s | 6.314s | 668MB | 84.3% 빠름 |

#### 측정 커맨드

첫 빌드 측정:

```bash
for v in 0 1 2 3 4; do
  hyperfine --runs 3 \
    --export-json /tmp/postforge-docker-bench/first-v${v}.json \
    "DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain -f dockerfiles/versions/Dockerfile.v${v} -t postforge:v${v}-first-bench ."
done
```

리소스 변경 후 재빌드 측정:

```bash
for v in 0 1 2 3 4; do
  hyperfine --warmup 1 --runs 3 \
    --prepare 'date +%s%N > app/src/main/resources/docker-build-benchmark.txt' \
    --export-json /tmp/postforge-docker-bench/rebuild-v${v}.json \
    "DOCKER_BUILDKIT=1 docker build --progress=plain -f dockerfiles/versions/Dockerfile.v${v} -t postforge:v${v}-rebuild-bench ."
done
```

`docker-build-benchmark.txt`는 측정 후 원래 내용으로 복구했다.

#### 상세 측정값

| 버전 | 첫 빌드 mean ± σ | 첫 빌드 range | 재빌드 mean ± σ | 재빌드 range |
| --- | ---: | ---: | ---: | ---: |
| v0 | 38.942s ± 0.424s | 38.454s - 39.221s | 40.129s ± 0.520s | 39.591s - 40.630s |
| v1 | 34.647s ± 0.770s | 33.799s - 35.301s | 34.626s ± 0.560s | 34.055s - 35.175s |
| v2 | 37.013s ± 2.332s | 35.100s - 39.611s | 18.877s ± 0.297s | 18.534s - 19.051s |
| v3 | 37.567s ± 1.673s | 35.640s - 38.651s | 19.143s ± 0.401s | 18.680s - 19.386s |
| v4 | 36.863s ± 0.717s | 36.090s - 37.508s | 6.314s ± 0.917s | 5.533s - 7.324s |

---
### 기존 Dockerfile 단일 비교 기록

아래 기록은 버전별 Dockerfile 재측정이 아니라, 기존 단일 `Dockerfile`을 대상으로 했던 이전 측정이다.

#### 캐시 100퍼 빌드
> hyperfine --warmup 3 --runs 20 'DOCKER_BUILDKIT=1 docker build --progress=plain --target runtime -t postforge:local .'

Time (mean ± σ):     589.4 ms ±  21.0 ms    [User: 107.7 ms, System: 88.0 ms]
Range (min … max):   560.0 ms … 638.3 ms    20 runs

수정 후

Time (mean ± σ):     606.4 ms ±  24.7 ms    [User: 105.8 ms, System: 85.4 ms]
Range (min … max):   574.4 ms … 664.6 ms    20 runs

#### 리소스 변경 후 측정
> hyperfine --warmup 2 --runs 20 --prepare 'date +%s%N > app/src/main/resources/docker-build-benchmark.txt' 'DOCKER_BUILDKIT=1 docker build --progress=plain --target runtime -t postforge:local .'

Time (mean ± σ):     11.643 s ±  0.094 s    [User: 0.111 s, System: 0.095 s]
Range (min … max):   11.489 s … 11.862 s    20 runs

수정 후

Time (mean ± σ):      8.269 s ±  0.138 s    [User: 0.110 s, System: 0.093 s]
Range (min … max):    8.021 s …  8.482 s    20 runs

#### 캐시 없이 빌드
> hyperfine --runs 3 'DOCKER_BUILDKIT=1 docker build --no-cache --progress=plain --target runtime -t postforge:local .'

Time (mean ± σ):     46.320 s ±  1.181 s    [User: 0.130 s, System: 0.118 s]
Range (min … max):   45.413 s … 47.655 s    3 runs

수정 후

Time (mean ± σ):     43.608 s ±  1.176 s    [User: 0.133 s, System: 0.123 s]
Range (min … max):   42.265 s … 44.456 s    3 runs

|      측정 항목      | 수정 전    |  수정 후   |             변화             |
|:---------------:|:--------|:-------:|:--------------------------:|
|   캐시 100% 빌드    | 589.4ms | 606.4ms |    약 **+17ms**, 사실상 동일     |
|   리소스 변경 후 빌드   | 11.643s | 8.269s  | 약 **-3.374s**, **29% 개선**  |
| `--no-cache` 빌드 | 46.320s | 43.608s | 약 **-2.712s**, **5.9% 개선** |
