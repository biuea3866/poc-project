FROM amazoncorretto:17-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
