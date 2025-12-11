FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean :app:build -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]