# Dockerfile 설명
***

#### FROM gradle:8.5-jdk17 AS build
- gradle 8.5 + JDK 17 이미지 사용해서 빌드
- `AS build` = 이 단계 이름은 "build"

## WORKDIR /app
- 작업 폴더는 /app으로 설정

## COPY . .
- 현재 폴더(.)의 모든 파일을 컨테이너의 .(현재=WORKDIR)로 복사

## RUN gradle clean build -x test
- gradle 빌드 실행 (테스트 제외)
- 빌드 단계 끝나면 `/app/build/libs/postforge-0.0.1-SNAPSHOT.jar` 만들어짐

## FROM eclipse-temurin:17-jre
- 실행용 이미지는 JRE 17 사용 (JDK보다 가벼움)

## COPY --from=build /app/build/libs/*.jar app.jar
build 단계에서 만든 jar 파일 가져오기

**상세 설명:**
- `--from=build` → "build"라는 이름의 단계에서
- `/app/build/libs/*.jar` → 이 경로의 jar 파일 찾기
    - `*` = 와일드카드 (어떤 이름이든 .jar로 끝나는 파일)
- `app.jar` → 현재 단계의 `/app/app.jar`로 복사

## EXPOSE 8080
- 8080 포트 쓴다고 문서화 (실제 열리는 건 아님)

## ENTRYPOINT ["java", "-jar", "app.jar"]
- 컨테이너 시작하면 이 명령어 실행


