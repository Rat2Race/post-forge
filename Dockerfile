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