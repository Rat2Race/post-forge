FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /workspace

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

COPY ai/build.gradle ./ai/
COPY ingest/build.gradle ./ingest/
COPY app/build.gradle ./app/
COPY auth/build.gradle ./auth/
COPY board/build.gradle ./board/
COPY source/build.gradle ./source/
COPY catalog/build.gradle ./catalog/
COPY price/build.gradle ./price/
COPY core/build.gradle ./core/
COPY support/build.gradle ./support/
COPY messaging/build.gradle ./messaging/

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :app:dependencies --configuration runtimeClasspath --no-daemon

COPY ai/src ./ai/src
COPY ingest/src ./ingest/src
COPY app/src ./app/src
COPY auth/src ./auth/src
COPY board/src ./board/src
COPY source/src ./source/src
COPY catalog/src ./catalog/src
COPY price/src ./price/src
COPY core/src ./core/src
COPY support/src ./support/src
COPY messaging/src ./messaging/src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    --mount=type=cache,target=/workspace/.gradle \
    gradle :app:bootJar -x test --parallel --build-cache --no-daemon
