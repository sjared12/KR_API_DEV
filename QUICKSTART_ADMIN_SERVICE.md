# Quick Start - Admin Service Architecture

## 5-Minute Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Git

### Option 1: Docker Compose (Recommended)

```bash
# Clone and navigate to project
cd KR_API

# Build all images
docker-compose build

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Access Admin Dashboard
# Open browser: http://localhost:8080
```

### Option 2: Local Development (Maven)

**Terminal 1 - Admin Service:**
```bash
cd admin-service
mvn spring-boot:run
```

**Terminal 2 - Payment Service:**
```bash
cd payment-service
mvn spring-boot:run
```

**Terminal 3 - Logger Service:**
```bash
cd Logger
mvn spring-boot:run
```

Then access: `http://localhost:8080`

## Accessing the Dashboard

1. **Go to**: `http://localhost:8080`
2. **Login with**:
   - Username: `admin`
   - Password: `changeit` (change this immediately!)

## First-Time Setup

### 1. Register Services

In the Admin Dashboard:

1. Click **"+ Register Service"**
2. Fill in service details:
   - **Service Name**: `payment-service`
   - **Service URL**: `http://localhost`
   - **Service Port**: `8081`
   - **Description**: `Payment and subscription management`
3. Click **Register**
4. Repeat for other services:
   - Logger (8082)
   - Feedback App (3000)

### 2. Check Service Health

1. Click **"🔄 Check All Health"** button
2. Wait for health checks to complete
3. Services should show green ✓ if healthy

### 3. View Logs

1. Click **"📋 Logs"** on any service card
2. View real-time logs from that service
3. Click **"🔄 Refresh"** for latest logs

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | changeit |

**⚠️ CHANGE THESE IMMEDIATELY IN PRODUCTION**

## Common Tasks

### Add a New Admin User
```bash
curl -X POST http://localhost:8080/api/admin/users/create \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "role": "ROLE_OPERATOR"
  }'
```

### Check Service Health
```bash
curl http://localhost:8080/api/services/1/health
```

### Get Service Logs
```bash
curl "http://localhost:8080/api/services/1/logs?lines=50"
```

### Enable/Disable Service
```bash
# Disable
curl -X POST http://localhost:8080/api/services/1/disable

# Enable
curl -X POST http://localhost:8080/api/services/1/enable
```

## Port Reference

| Service | Port | URL |
|---------|------|-----|
| Admin Service | 8080 | http://localhost:8080 |
| Payment Service | 8081 | http://localhost:8081 |
| Logger Service | 8082 | http://localhost:8082 |
| Feedback App | 3000 | http://localhost:3000 |
| Nginx Proxy | 80 | http://localhost |

## Database Ports

| Database | Port | User |
|----------|------|------|
| Admin DB | 5433 | admin_user |
| Payment DB | 5434 | payment_user |
| Logger DB | 5435 | logger_user |

## Troubleshooting

### Services show as "Unhealthy"
- Verify services are running: `docker-compose ps`
- Check service logs: Click "📋 Logs" on service card
- Ensure health endpoints exist: `http://localhost:8081/api/health`

### Can't connect to Admin Service
- Verify it's running: `curl http://localhost:8080/api/services`
- Check Docker logs: `docker-compose logs admin-service`
- Verify port 8080 is not in use

### Database connection errors
- Restart databases: `docker-compose restart admin-db payment-db logger-db`
- Check credentials in `compose.yaml`
- Verify PostgreSQL containers are healthy

### Forgot Admin Password
- Reset in `compose.yaml` environment variables
- Rebuild and restart: `docker-compose up -d --build admin-service`

## Next Steps

1. ✅ Services are running
2. ⏳ Register sub-services in dashboard
3. ⏳ Create additional admin users with different roles
4. ⏳ Configure monitoring alerts
5. ⏳ Set up automatic backups
6. ⏳ Enable HTTPS for production

## Documentation

- **Full Architecture**: See [ARCHITECTURE_V2.md](./ARCHITECTURE_V2.md)
- **API Documentation**: See endpoint descriptions in Admin Service
- **Payment Service Docs**: See [payment-service/subscription-docs/](./payment-service/subscription-docs/)

## Getting Help

**Check Admin Dashboard Logs:**
```bash
docker-compose logs -f admin-service
```

**Get Specific Service Logs:**
```bash
# From dashboard: Click service card → 📋 Logs
# Or via API:
curl "http://localhost:8080/api/services/1/logs?lines=100"
```

**Test Service Connectivity:**
```bash
# From admin-service container
docker-compose exec admin-service curl http://payment-service:8080/api/health
```

---

**You're all set! 🚀**

Go to `http://localhost:8080` and start managing your microservices!
