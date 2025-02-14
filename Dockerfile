FROM maven:3.9-amazoncorretto-23 AS builder

ARG JAVA_OPTS
ENV JAVA_OPTS ${JAVA_OPTS}

WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:23-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY integration/grafana-opentelemetry-java-v2.12.0.jar ./grafana-opentelemetry-java-v2.12.0.jar

CMD ["java", "${JAVA_OPTS}", "-Xms128m", "-Xmx1024m", "-javaagent:grafana-opentelemetry-java-v2.12.0.jar", "-jar", "app.jar"]
