#FROM gradle:8.10-jdk21-alpine AS build
#WORKDIR /workspace
#
#ENV GRADLE_USER_HOME=/home/gradle/.gradle
#
#COPY build.gradle settings.gradle ./
#COPY gradle ./gradle
#
#COPY ai/build.gradle ./ai/
#COPY ingest/build.gradle ./ingest/
#COPY app/build.gradle ./app/
#COPY auth/build.gradle ./auth/
#COPY board/build.gradle ./board/
#COPY core/build.gradle ./core/
#
#RUN --mount=type=cache,target=/home/gradle/.gradle \
#    gradle :app:dependencies --configuration runtimeClasspath --no-daemon
#
#COPY ai/src ./ai/src
#COPY ingest/src ./ingest/src
#COPY app/src ./app/src
#COPY auth/src ./auth/src
#COPY board/src ./board/src
#COPY core/src ./core/src
#
#RUN --mount=type=cache,target=/home/gradle/.gradle \
#    --mount=type=cache,target=/workspace/.gradle \
#    gradle :app:bootJar -x test --parallel --build-cache --no-daemon

FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

COPY ai/build.gradle ./ai/
COPY app/build.gradle ./app/
COPY auth/build.gradle ./auth/
COPY board/build.gradle ./board/
COPY core/build.gradle ./core/

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

COPY ai/src ./ai/src
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