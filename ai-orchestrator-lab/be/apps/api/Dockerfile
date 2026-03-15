FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Pinpoint Agent
ARG PINPOINT_AGENT_VERSION=2.5.4
ADD https://github.com/pinpoint-apm/pinpoint/releases/download/v${PINPOINT_AGENT_VERSION}/pinpoint-agent-${PINPOINT_AGENT_VERSION}.tar.gz /opt/pinpoint-agent.tar.gz
RUN tar -xzf /opt/pinpoint-agent.tar.gz -C /opt && \
    mv /opt/pinpoint-agent-${PINPOINT_AGENT_VERSION} /opt/pinpoint-agent && \
    rm /opt/pinpoint-agent.tar.gz

ENV PINPOINT_AGENT_PATH=/opt/pinpoint-agent
ENV PINPOINT_COLLECTOR_HOST=pinpoint-collector
ENV PINPOINT_APPLICATION_NAME=ai-wiki-api
ENV PINPOINT_AGENT_ID=api-001

ENV JAVA_TOOL_OPTIONS="-javaagent:${PINPOINT_AGENT_PATH}/pinpoint-bootstrap.jar \
    -Dpinpoint.agentId=${PINPOINT_AGENT_ID} \
    -Dpinpoint.applicationName=${PINPOINT_APPLICATION_NAME} \
    -Dpinpoint.profiler.transport.grpc.collector.ip=${PINPOINT_COLLECTOR_HOST}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
