#### Dockerfile 설명
```
FROM gradle:8.10-jdk21-alpine AS build
- gradle 8.5 + JDK 17 이미지 사용해서 빌드
- `AS build` = 이 단계 이름은 "build"

WORKDIR /app
- 작업 폴더는 `/app`으로 설정

COPY . .
- 현재 폴더(.)의 모든 파일을 컨테이너의 WORKDIR(.)로 복사

RUN gradle clean build -x test
- gradle 빌드 실행 (테스트 제외)
- 빌드 단계 끝나면 `/app/build/libs/postforge-0.0.1-SNAPSHOT.jar` 만들어짐

FROM eclipse-temurin:17-jre
- 실행용 이미지는 JRE 17 사용 (JDK보다 가벼움)

COPY --from=build /app/build/libs/*.jar app.jar
- build 단계에서 만든 jar 파일 가져오기

**상세 설명:**
- `--from=build` → "build"라는 이름의 단계에서
- `/app/build/libs/*.jar` → 이 경로의 jar 파일 찾기
    - `*` = 와일드카드 (어떤 이름이든 .jar로 끝나는 파일)
- `app.jar` → 현재 단계의 `/app/app.jar`로 복사

EXPOSE 8080
- 8080 포트 쓴다고 문서화 (실제 열리는 건 아님)

ENTRYPOINT ["java", "-jar", "app.jar"]
- 컨테이너 시작하면 이 명령어 실행
```
---
#### 도커 파일 최적화
1. 멀티스테이징 (빌드/실행 분리로 최종 이미지 크기 감소)
2. 의존성 캐싱 (의존성 파일 먼저 복사해서 레이어 캐시 활용)
3. ignore (불필요한 파일 제외로 빌드 컨텍스트 감소)
4. BuildKit (--mount=type=cache로 Gradle 캐시 유지)

```
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

COPY app/build.gradle ./app/
COPY auth/build.gradle ./auth/
COPY board/build.gradle ./board/
COPY core/build.gradle ./core/

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

COPY app/src ./app/src
COPY auth/src ./auth/src
COPY board/src ./board/src
COPY core/src ./core/src

RUN --mount=type=cache,target=/root/.gradle \
    gradle clean :app:build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
---

### 최종 결과

| 버전 | 변경사항 | 첫 빌드 | 재빌드 | 이미지 크기 | 재빌드 개선율 |
| --- | --- | --- | --- | --- | --- |
| v0 | 단일 스테이지 | 1m 33s | 1m 32s | 1.77GB | - (기준) |
| v1 | 멀티 스테이지 | 1m 27s | 1m 29s | 556MB | 3% 느림 |
| v2 | 의존성 캐싱 | 1m 38s | **1m 6s** | 556MB | **28% 빠름** |
| v4 | BuildKit + Alpine | 1m 42s | **16초** | 556MB | **83% 빠름** |

- 똑같은 네트워크 속도로 측정한 것이 아니여서 정확한 측정이 불가능 했음
- 확실한건 용량 감소와 재빌드시 속도향상이 되는걸 느낄 수 있었음