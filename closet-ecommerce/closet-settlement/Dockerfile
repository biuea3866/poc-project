FROM amazoncorretto:21-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
