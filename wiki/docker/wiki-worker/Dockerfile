FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew :wiki-worker:bootJar -x test --no-daemon

FROM pinpointdocker/pinpoint-agent:2.5.1 AS agent

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/wiki-worker/build/libs/wiki-worker-*.jar app.jar
COPY --from=agent /pinpoint-agent /pinpoint-agent
EXPOSE 8082
ENTRYPOINT ["java", \
  "-javaagent:/pinpoint-agent/pinpoint-bootstrap.jar", \
  "-Dpinpoint.agentId=wiki-worker-1", \
  "-Dpinpoint.applicationName=wiki-worker", \
  "-Dpinpoint.profiler.profiles.active=release", \
  "-Dprofiler.transport.grpc.collector.ip=pinpoint-collector", \
  "-jar", "/app/app.jar"]
