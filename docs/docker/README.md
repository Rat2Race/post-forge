# Docker Docs

| 문서 | 내용 |
| --- | --- |
| [build.md](build.md) | Dockerfile 버전별 빌드 시간과 재빌드 성능 기록 |
| [image-tests.md](image-tests.md) | `dockerfiles/versions` 이미지별 빌드/실행 smoke 결과 |
| [cache-ab.md](cache-ab.md) | git 기록의 Docker/Gradle 캐시 적용 전후 A/B 비교 |

## 테스트/측정 도구 요약

| 도구 | 사용 목적 | 근거 문서 |
| --- | --- | --- |
| Docker / Docker BuildKit | Dockerfile 버전별 이미지 빌드, `--no-cache`/warm cache 조건 비교, `--mount=type=cache` 검증 | [build.md](build.md), [cache-ab.md](cache-ab.md), [image-tests.md](image-tests.md) |
| Docker Buildx | BuildKit 기반 빌드 환경 확인 | [cache-ab.md](cache-ab.md), [image-tests.md](image-tests.md) |
| `hyperfine` | Docker 빌드 시간 반복 측정, 평균/표준편차/range 기록 | [build.md](build.md), [cache-ab.md](cache-ab.md), [image-tests.md](image-tests.md) |
| Gradle Wrapper | `:app:bootJar` 생성, Gradle build cache 적용 전후 비교 | [cache-ab.md](cache-ab.md), [image-tests.md](image-tests.md) |
| Spring Boot `jarmode=tools` | executable jar layer 구조 확인과 layered runtime extraction 검증 | [image-tests.md](image-tests.md) |
| 임시 PostgreSQL/PgVector 컨테이너 | runtime 이미지가 실제 DB 의존성과 함께 기동되는지 smoke 검증 | [image-tests.md](image-tests.md) |
| 임시 Redis 컨테이너 | runtime 이미지가 Redis 의존성과 함께 `/actuator/health`를 통과하는지 검증 | [image-tests.md](image-tests.md) |
| Spring Boot Actuator `/actuator/health` | 빌드된 이미지가 앱/DB/Redis 연결 후 `UP` 상태가 되는지 확인 | [image-tests.md](image-tests.md) |
| Docker image/inspect 계열 명령 | 이미지 크기, layer 산출물, Java runtime 존재 여부 확인 | [build.md](build.md), [image-tests.md](image-tests.md) |

## 트레이드오프 위치

- Docker/Gradle 캐시 분리의 이득과 손해는 [cache-ab.md](cache-ab.md)의 `해석`과 `결론`을 본다.
- Spring Boot layered jar runtime의 이득과 손해는 [image-tests.md](image-tests.md)의 `fat jar runtime 방식과 비교`를 본다.
