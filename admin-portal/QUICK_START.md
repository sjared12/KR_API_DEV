# Quick Start: Testing Spring Boot Admin Portal

## Prerequisites

- Java 21 JDK installed
- Maven 3.9+ installed
- DigitalOcean API credentials (optional for testing UI without live data)

## Step 1: Build the Project

```bash
cd /Users/jaredsmith/Documents/GitHub/KR_API/admin-portal
mvn clean package
```

This will:
- Compile Java sources
- Package into a JAR file
- Output to `target/admin-portal-*.jar`

## Step 2: Run Locally (Development)

### Option A: Using Maven Spring Boot Plugin
```bash
mvn spring-boot:run
```

### Option B: Using Java JAR
```bash
java -jar target/admin-portal-*.jar
```

### With Environment Variables
```bash
export DO_API_TOKEN="your_digitalocean_token"
export DO_APP_ID="your_app_id"
export CONFIG_PATH="/tmp/config.json"
mvn spring-boot:run
```

## Step 3: Access the Application

Open your browser and navigate to:
```
http://localhost:8080/admin/
```

You should see the dark-themed Admin Portal dashboard with:
- Container Status table (will show "No services found" if DO credentials not set)
- Service Configuration section (for api, payment, logger, feedback, proxy)

## API Endpoint Testing

Use curl or your browser's DevTools to test endpoints:

### Health Check
```bash
curl http://localhost:8080/admin/api/health
```

### Load Configuration
```bash
curl http://localhost:8080/admin/api/config/api
```

### List Containers (requires DO_API_TOKEN and DO_APP_ID)
```bash
curl http://localhost:8080/admin/api/containers
```

### Validate DO Credentials
```bash
curl http://localhost:8080/admin/api/do/validate
```

## Troubleshooting

### Port Already in Use
Change the port in `application.properties`:
```properties
server.port=9090
```
Then access at `http://localhost:9090/admin/`

### Static Resources Not Loading
- Check that `src/main/resources/static/index.html` exists
- Verify `WebMvcConfig.java` is in the `config` package
- Check application logs for resource handler registration

### API Calls Return 404
- Ensure you're using `/admin/api/*` paths (not `/api/*`)
- Check that context-path is set to `/admin` in `application.properties`

### DigitalOcean API Errors
- Verify `DO_API_TOKEN` environment variable is set
- Verify `DO_APP_ID` environment variable is set
- Use `/admin/api/do/validate` endpoint to test credentials

## Project Structure

```
admin-portal/
├── src/main/
│   ├── java/com/example/adminportal/
│   │   ├── AdminPortalApplication.java       (Main entry point)
│   │   ├── config/
│   │   │   ├── ServiceConfig.java            (Config POJO)
│   │   │   └── WebMvcConfig.java             (Static resource config)
│   │   ├── controller/
│   │   │   ├── ConfigController.java         (Config endpoints)
│   │   │   ├── HealthController.java         (Health & DO endpoints)
│   │   │   └── ContainerController.java      (Container endpoints - placeholder)
│   │   └── service/
│   │       ├── ConfigService.java            (Persistent config management)
│   │       └── DigitalOceanService.java      (DO API client)
│   └── resources/
│       ├── static/
│       │   └── index.html                    (Dashboard UI - 1096 lines)
│       └── application.properties             (Spring Boot config)
├── pom.xml                                    (Maven config, Java 21, Spring Boot 3.4.10)
├── Dockerfile                                 (Eclipse Temurin JDK 21 + JRE 21)
└── README.md                                  (Deployment & architecture guide)
```

## Key Features

✅ Unified Spring Boot API Gateway
✅ Dark-themed responsive UI
✅ Real-time container status monitoring
✅ Service configuration editor
✅ DigitalOcean App Platform integration
✅ Persistent configuration file (`/data/config.json`)
✅ CORS enabled for `/api/**` routes
✅ Graceful error handling and validation

## Next Steps After Testing

1. **Archive old Node.js files** (no longer needed):
   ```bash
   # Backup original files
   mv admin-portal/public admin-portal/public.backup
   mv admin-portal/server.js admin-portal/server.js.backup
   mv api api.backup
   ```

2. **Update Docker Compose** (if used):
   - Remove `api` service entry
   - Update `admin-portal` build to use Maven instead of npm

3. **Deploy to Production**:
   - Build Docker image: `docker build -t kr-api-admin-portal .`
   - Push to registry
   - Update deployment configuration with env vars

4. **Documentation Updates**:
   - Update main README.md to document unified architecture
   - Remove references to separate API service
   - Add Spring Boot setup instructions

## Support

For issues or questions, refer to:
- `README.md` - Full architecture and setup guide
- `UI_MIGRATION_COMPLETE.md` - Migration details
- Spring Boot docs: https://spring.io/projects/spring-boot
- DigitalOcean API docs: https://docs.digitalocean.com/reference/api/
