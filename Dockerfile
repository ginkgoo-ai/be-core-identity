FROM amazoncorretto:21-alpine

WORKDIR /app

COPY target/*.jar app.jar

# Database Configuration
ENV POSTGRES_HOST=localhost
ENV POSTGRES_PORT=15432
ENV POSTGRES_DB=identity
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

# Redis Configuration
ENV REDIS_HOST=localhost
ENV REDIS_PORT=16379

# Mail Configuration
ENV MAIL_HOST=smtp.mailtrap.io
ENV MAIL_PORT=2525
ENV MAIL_USERNAME=fd21a8aeaf0fbc
ENV MAIL_PASSWORD=3980d9842d1fb5
ENV MAIL_FROM=sandbox.smtp.mailtrap.io
ENV APP_URL=http://localhost:9000

# Server Configuration
ENV SERVER_PORT=9000

# App Configuration
ENV DEFAULT_REDIRECT_URI=http://localhost:3000
ENV APP_VERIFICATION_SERVER_URL=http://localhost:9000
ENV APP_VERIFICATION_REDIRECT_URL=http://localhost:9000

# Security Configuration
ENV ADMIN_API_KEY=dBj8kX7vYp4mN9qR2tW5hL1cF3nM6sQ9wZ0xV8yU4jK7gH2bP5
ENV ALLOWED_ORIGINS=http://localhost:3000

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
