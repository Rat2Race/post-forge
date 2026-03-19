FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    gradle clean build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar crawl.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "crawl.jar"]
