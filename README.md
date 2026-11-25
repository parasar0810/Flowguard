# FlowGuard - Distributed API Rate Limiting Middleware

FlowGuard is a high-performance, distributed rate-limiting and abuse detection middleware built with Spring Boot, Redis, and PostgreSQL. It protects APIs from traffic spikes, abuse, and bot attacks.

## Features
- **Rate Limiting**: Token Bucket & Sliding Window algorithms backed by Redis Lua scripts.
- **Distributed**: Stateless architecture using Redis for counters.
- **API Key Management**: tiered plans (Free, Pro, Enterprise).
- **Abuse Detection**: Automatically blocks IPs exhibiting suspicious behavior.
- **Monitoring**: Dashboard API for real-time stats (Requests, Blocked Users).
- **Asynchronous Logging**: Non-blocking request logging to PostgreSQL.

## Tech Stack
- **Java 17**
- **Spring Boot 3.2.3** (Web, Data JPA, Security, Actuator, Validation)
- **Redis** (Lettuce client)
- **PostgreSQL**
- **Docker Compose**

## Tests
`mvn test` runs 26 tests: the filter, services, API-key handling and an in-memory limiter, plus `RedisLuaScriptsIntegrationTest`, which runs the production Lua scripts against a real Redis. Set `REDIS_HOST`, for example by running `docker run -p 6379:6379 redis:7-alpine` and then `REDIS_HOST=localhost mvn test`. It checks that each algorithm allows exactly the limit, including when 200 concurrent requests hit one key (10 allowed, never more). CI runs it with a Redis service container.

## Setup & Running

### 1. Prerequisites
- **Java 17+**: Required to run the application.
- **Maven 3.8+**: Required to build the project.
- **Docker & Docker Compose**: Required for Redis and PostgreSQL.

### 2. Troubleshooting
If you encounter `command not found` errors:
1.  **Docker**: Install Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop/). Ensure it is running.
2.  **Maven**: Install Maven from [maven.apache.org](https://maven.apache.org/download.cgi) and add `bin` to your PATH.
    - *Alternative*: If `mvnw` is present, use `./mvnw spring-boot:run`.

### 3. Start Infrastructure
Start PostgreSQL and Redis containers:
```bash
docker compose up -d
```

### 4. Run Locally (Without Docker)
To run the application using the embedded H2 database and in-memory rate limiter (development mode).
**Note**: We have installed a portable Maven for you in `apache-maven-3.9.6`.

1.  Use the provided local Maven distribution:
    ```bash
    .\apache-maven-3.9.6\bin\mvn spring-boot:run -Dspring.profiles.active=dev
    ```
2.  Or launch the JAR directly after building:
    ```bash
    java -jar target/flowguard-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
    ```

### 3. Run Application
```bash
mvn spring-boot:run
```
The application will start on `http://localhost:8080`.

## API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new user and get an API Key.
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```

### Dashboard (Admin)
- `GET /api/dashboard/stats`: Get system metrics.

### Protected Resources
- All other endpoints require `X-API-KEY` header.
- Rate limits are applied based on the plan assigned to the key.

## Configuration
- **Rate Limits**: defined in `DataInitializer` (Free: 50/min, Pro: 500/min).
- **Abuse Detection**: Blocks IPs after 5 failed attempts (configurable in `AbuseDetectionService`).
