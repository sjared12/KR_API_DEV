# Admin Portal - Unified API Service

## Overview

Admin Portal is a Spring Boot microservice that provides a **single consolidated API** for:
- Infrastructure and container management (via DigitalOcean App Platform)
- Service configuration management
- Cross-service settings and environment variables

This service serves as the **central API gateway** for admin operations, reducing exposure by routing all administrative API interactions through a single endpoint.

## Architecture

```
Admin Portal (Spring Boot)
├── /api/health          - Service health and status
├── /api/config/*        - Configuration management
├── /api/containers/*    - Container/service management
├── /api/do/*            - DigitalOcean API integration
└── /api/admin/*         - Admin operations
```

All other services (payment-service, feedback-app, etc.) have their own domains and communicate with this admin API as needed.

## Environment Variables

### Required

| Variable | Description | Example |
|----------|-------------|---------|
| `DO_API_TOKEN` | DigitalOcean API token | `dop_v1_xxxxxxxxxxxxx` |
| `DO_APP_ID` | DigitalOcean App ID | `12345678-1234-1234-1234-123456789012` |

### Optional

| Variable | Description | Default |
|----------|-------------|---------|
| `CONFIG_PATH` | Persistent config file location | `/data/config.json` |

## API Endpoints

### Health & Status

#### `GET /api/health`
Service health check.

**Response:**
```json
{
  "status": "ok",
  "doConfigured": true,
  "appIdConfigured": true,
  "appIdPreview": "123456…9012"
}
```

#### `GET /api/do/validate`
Validate DigitalOcean configuration.

**Response:**
```json
{
  "ok": true,
  "name": "my-app",
  "id": "12345678-1234-1234-1234-123456789012"
}
```

### Configuration Management

#### `GET /api/config`
Get all service configurations.

**Response:**
```json
{
  "api": { ... },
  "payment": { ... },
  "feedback": { ... }
}
```

#### `GET /api/config/:service`
Get config for a specific service.

**Parameters:**
- `service` (string) – Service name (api, payment, feedback, etc.)

**Response:**
```json
{
  "SPRING_DATASOURCE_URL": "jdbc:postgresql://db:5432/simpletixdb",
  "SPRING_DATASOURCE_USERNAME": "postgres",
  "SPRING_DATASOURCE_PASSWORD": "postgres"
}
```

#### `PUT /api/config/:service`
Update service configuration.

**Request:**
```json
{
  "SPRING_DATASOURCE_PASSWORD": "newpassword"
}
```

**Response:**
```json
{
  "ok": true,
  "config": { ... }
}
```

#### `GET /api/public-config/:service`
Public endpoint for service config discovery (used by client apps).

### DigitalOcean Integration

#### `GET /api/do/apps`
List all accessible DigitalOcean applications.

#### `GET /api/admin/users`
Admin users list (placeholder).

## Building & Running

### Local Development

```bash
# Build
mvn clean package

# Run with environment variables
export DO_API_TOKEN="your-token"
export DO_APP_ID="your-app-id"
java -jar target/admin-portal-*.jar

# Server runs on http://localhost:8080/admin
```

### Docker

```bash
# Build image
docker build -t kr-admin-portal .

# Run container
docker run -d \
  --name admin-portal \
  -p 8080:8080 \
  -v admin-data:/data \
  -e DO_API_TOKEN="your-token" \
  -e DO_APP_ID="your-app-id" \
  -e CONFIG_PATH=/data/config.json \
  kr-admin-portal
```

### Docker Compose

```yaml
version: '3.8'

services:
  admin-portal:
    build: ./admin-portal
    ports:
      - "8080:8080"
    environment:
      - DO_API_TOKEN=your-token
      - DO_APP_ID=your-app-id
      - CONFIG_PATH=/data/config.json
    volumes:
      - admin-data:/data
    restart: unless-stopped

  payment-service:
    build: ./payment-service
    ports:
      - "8081:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payments
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    depends_on:
      - postgres

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  admin-data:
  postgres-data:
```

### DigitalOcean App Platform

In your `app.yaml`:

```yaml
spec:
  services:
    - name: admin-portal
      source_dir: admin-portal
      build_command: mvn clean package -DskipTests
      run_command: java -jar target/*.jar
      http_port: 8080
      environment_slug: java
      envs:
        - key: DO_API_TOKEN
          scope: RUN_TIME
          value: ${DO_API_TOKEN}
        - key: DO_APP_ID
          scope: RUN_TIME
          value: ${DO_APP_ID}
        - key: CONFIG_PATH
          scope: RUN_TIME
          value: /data/config.json
      envs_slug: java

    - name: payment-service
      source_dir: payment-service
      build_command: mvn clean package -DskipTests
      run_command: java -jar target/*.jar
      http_port: 8080
      environment_slug: java
```

## Service Configuration

The admin portal maintains a persistent configuration file at `/data/config.json`:

```json
{
  "api": { ... },
  "payment": { ... },
  "feedback": { ... },
  "admin": { ... },
  "proxy": { ... }
}
```

Each service section stores service-specific configuration that can be updated via the REST API.

## Technology Stack

- **Java 21**
- **Spring Boot 3.4.10**
- **Spring MVC** for REST APIs
- **Jackson** for JSON processing
- **Apache HttpClient 5** for API calls
- **Lombok** for reducing boilerplate

## Notes

- **No built-in authentication**. Deploy behind an API gateway or proxy with auth.
- **Single point of exposure**. All admin API interactions go through this service.
- **Persistent config**. Configuration is stored locally; use shared storage in multi-instance deployments.
- **CORS enabled** for all origins. Adjust in `AdminPortalApplication.java` as needed.

## Troubleshooting

**Empty app list:**
- Verify `DO_API_TOKEN` is set and valid
- Verify `DO_APP_ID` matches an accessible app
- Call `GET /api/do/validate` to test credentials

**Config not persisting:**
- Ensure `/data` volume exists and is writable
- Check `CONFIG_PATH` environment variable

## Next Steps

- Add UI dashboard (served as static resources or Thymeleaf templates)
- Implement container management endpoints (list, restart, delete, create)
- Add request logging and audit trails
- Implement authentication and authorization
