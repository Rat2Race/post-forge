FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

COPY app/build.gradle ./app/
COPY auth-service/build.gradle ./auth-service/
COPY board-service/build.gradle ./board-service/
COPY common-lib/build.gradle ./common-lib/
COPY security-api/build.gradle ./security-api/

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

COPY app/src ./app/src
COPY auth-service/src ./auth-service/src
COPY board-service/src ./board-service/src
COPY common-lib/src ./common-lib/src
COPY security-api/src ./security-api/src

RUN --mount=type=cache,target=/root/.gradle \
    gradle clean :app:build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]