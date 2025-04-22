FROM maven:3.9-amazoncorretto-23 AS builder
ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /app
COPY pom.xml ./
COPY settings.xml ./
COPY src ./src

RUN mvn clean install -U -s settings.xml && \
    mvn package -Dmaven.test.skip=true -s settings.xml

FROM openjdk:23-jdk-slim
ARG GRAFANA_OTEL_VERSION=v2.15.0
ENV GRAFANA_OTEL_JAR=grafana-opentelemetry-java-${GRAFANA_OTEL_VERSION}.jar

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Install curl, download the agent and clean up
RUN apt-get update && \
    apt-get install -y curl && \
    curl -s -L https://github.com/grafana/grafana-opentelemetry-java/releases/download/${GRAFANA_OTEL_VERSION}/grafana-opentelemetry-java.jar \
     -o ${GRAFANA_OTEL_JAR} && \
    apt-get purge -y --auto-remove curl && \
    rm -rf /var/lib/apt/lists/*

CMD ["sh", "-c", "java -Xms128m -Xmx1024m -javaagent:${GRAFANA_OTEL_JAR} -jar app.jar"]
