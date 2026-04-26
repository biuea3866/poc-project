# Build context: project root (resilience-practice/)
# Build with: docker build -f payment-service/Dockerfile -t payment-service .
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY payment-service ./payment-service
COPY order-service ./order-service
COPY inventory-service ./inventory-service
RUN chmod +x ./gradlew && ./gradlew :payment-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/payment-service/build/libs/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
