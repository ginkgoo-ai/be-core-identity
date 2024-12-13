# Ginkgoo Core Identity Service

## Features

### Completed ✅

* OAuth 2.0 Core Implementation
* Google Social Login Integration

### In Progress 🚧

* Additional Social Login Integration
* Self-Registration
* Password Recovery
* Multi-Factor Authentication (SMS)
* Multi-Factor Authentication (Email)

## Tech Stack

* Java 21
* Spring Boot 3.x
* Spring Security & Spring Authentication Server
* PostgreSQL
* JWT

## Getting Started

```bash
git clone <repository-url>
cd ginkgoo-identity
mvn clean install
mvn spring-boot:run
```

## Health Check

Service health can be monitored at:

```bash
GET /health

# Response:
{
    "status": "UP",
}
```

## Configuration

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

  datasource:
    url: ${POSTGRES_URL}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

## Requirements

* JDK 21+
* PostgreSQL 14+
* Maven 3.8+

## License

MIT License

---

© 2024 Ginkgoo Core Identity Service

```
