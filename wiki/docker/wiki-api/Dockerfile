FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew :wiki-api:bootJar -x test --no-daemon

FROM pinpointdocker/pinpoint-agent:2.5.1 AS agent

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/wiki-api/build/libs/wiki-api-*.jar app.jar
COPY --from=agent /pinpoint-agent /pinpoint-agent
EXPOSE 8080
ENTRYPOINT ["java", \
  "-javaagent:/pinpoint-agent/pinpoint-bootstrap.jar", \
  "-Dpinpoint.agentId=wiki-api-1", \
  "-Dpinpoint.applicationName=wiki-api", \
  "-Dpinpoint.profiler.profiles.active=release", \
  "-Dprofiler.transport.grpc.collector.ip=pinpoint-collector", \
  "-jar", "/app/app.jar"]
