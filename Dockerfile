FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY integration/grafana-opentelemetry-java-v2.12.0.jar ./grafana-opentelemetry-java-v2.12.0.jar

CMD ["java", "-Xms128m", "-Xmx1024m", "-javaagent:grafana-opentelemetry-java-v2.12.0.jar", "-jar", "app.jar"]
