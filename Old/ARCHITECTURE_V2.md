# New Architecture: Admin-Controlled Microservices

## Overview

The project has been restructured to use a **central Admin Service** that monitors, controls, and manages all sub-applications. This provides a unified dashboard for monitoring service health, logs, configurations, and lifecycle management.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Nginx Reverse Proxy                       │
│                    (Entry point: port 80)                    │
└──────────┬──────────────────────┬───────────────────────────┘
           │                      │
    ┌──────▼─────────┐    ┌──────▼──────────┐
    │  Admin Service │    │  Other Services │
    │  (Port 8080)   │    │  (8081-8082)    │
    │  - Dashboard   │    │  - Payment      │
    │  - Registry    │    │  - Logger       │
    │  - Health      │    │  - Feedback     │
    │  - Logs        │    │  - Frontend     │
    │  - Users       │    │                 │
    └────────────────┘    └─────────────────┘
```

## Components

### 1. **Admin Service** (New - Main Application)
- **Purpose**: Central control hub for all microservices
- **Port**: 8080
- **Database**: PostgreSQL (admin_db)
- **Features**:
  - Service registration and discovery
  - Health monitoring for all sub-services
  - Real-time log streaming
  - Service enable/disable controls
  - User authentication and role-based access control
  - Beautiful dashboard UI

### 2. **Payment Service** (Sub-application)
- **Purpose**: Handles subscription management and payments
- **Port**: 8081
- **Database**: PostgreSQL (payment_db)
- **Status**: Independent deployment, monitored by Admin Service

### 3. **Logger Service** (Sub-application)
- **Purpose**: Centralized logging
- **Port**: 8082
- **Database**: PostgreSQL (logger_db)
- **Status**: Independent deployment, monitored by Admin Service

### 4. **Feedback App** (Sub-application)
- **Purpose**: Event feedback collection
- **Port**: 3000
- **Status**: Independent deployment, monitored by Admin Service

### 5. **Nginx Reverse Proxy**
- **Purpose**: Single entry point for all services
- **Port**: 80/443
- **Status**: Routes requests to appropriate services

## Database Structure

Each service has its own PostgreSQL database:
- **admin_db** (Port 5433) - Admin Service
- **payment_db** (Port 5434) - Payment Service
- **logger_db** (Port 5435) - Logger Service

## Key Features

### Admin Dashboard
Access at `http://localhost:8080/dashboard.html`

**Features:**
- 📊 **Statistics**: Total services, healthy/unhealthy count
- 🏥 **Health Checks**: Real-time health status of all services
- 📋 **Logs**: Stream logs from any service
- ⚙️ **Configuration**: View and manage service configurations
- 🎛️ **Control**: Enable/disable services
- 👥 **User Management**: Create users with different roles (Admin, Operator, Viewer)
- 🔔 **Notifications**: Real-time alerts and status updates

### Service Registry

Services register with the Admin Service by providing:
- Service Name
- Service URL
- Service Port
- Description
- Health Check Endpoint
- Logs Endpoint

### Role-Based Access Control
- **ROLE_ADMIN**: Full access to all features
- **ROLE_OPERATOR**: Can view and control services
- **ROLE_VIEWER**: Read-only access

## Deployment

### Build All Services
```bash
mvn clean package -DskipTests
```

### Build Specific Modules
```bash
# Admin Service only
mvn clean package -DskipTests -pl admin-service

# Payment Service only
mvn clean package -DskipTests -pl payment-service
```

### Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f admin-service

# Stop all services
docker-compose down
```

### Environment Variables
Create a `.env` file in the project root:
```env
APP_ADMIN_USER=admin
APP_ADMIN_PASSWORD=changeit
JWT_SECRET=your-secret-key-change-in-production
SQUARE_API_TOKEN=your_token
SQUARE_LOCATION_ID=your_location
API_ENDPOINT=http://localhost:8080
```

## API Endpoints

### Admin Service APIs

#### Services Management
- `GET /api/services` - List all services
- `POST /api/services/register` - Register a new service
- `GET /api/services/{id}/health` - Check service health
- `GET /api/services/{id}/logs?lines=100` - Get service logs
- `GET /api/services/{id}/metrics` - Get service metrics
- `POST /api/services/{id}/enable` - Enable a service
- `POST /api/services/{id}/disable` - Disable a service
- `DELETE /api/services/{id}` - Unregister a service

#### User Management
- `GET /api/admin/users` - List all users
- `POST /api/admin/users/create` - Create new user
- `PUT /api/admin/users/{id}` - Update user
- `DELETE /api/admin/users/{id}` - Delete user

## Registering Sub-Services

Each sub-service needs to expose these endpoints:

### Health Check Endpoint
```
GET /api/health
Response: { "status": "UP", "timestamp": "..." }
```

### Logs Endpoint
```
GET /api/logs?lines=100
Response: { "logs": "..." }
```

### Metrics Endpoint
```
GET /api/metrics
Response: { "memory": "...", "cpu": "..." }
```

## Development Workflow

### Local Development

1. **Start Admin Service**
   ```bash
   mvn spring-boot:run -pl admin-service
   ```

2. **Start Payment Service** (in another terminal)
   ```bash
   mvn spring-boot:run -pl payment-service
   ```

3. **Access Dashboard**
   - Navigate to `http://localhost:8080`
   - Login with default credentials (admin/changeit)
   - Register services manually in the dashboard

### Docker Development

1. **Build images**
   ```bash
   docker-compose build
   ```

2. **Start services**
   ```bash
   docker-compose up -d
   ```

3. **Monitor logs**
   ```bash
   docker-compose logs -f admin-service
   ```

## Migration Guide

If you're migrating from the old architecture:

### Old Structure
- Main app on port 8080
- Payment service on port 8081
- Logger on separate port
- Admin portal on port 4000
- Services didn't have central management

### New Structure
- **Admin Service** (main) on port 8080
- Services register with Admin Service
- All services have dedicated databases
- Centralized monitoring and control
- Unified dashboard

### Steps to Migrate

1. **Update sub-services** to expose required endpoints:
   - `/api/health`
   - `/api/logs`
   - `/api/metrics`

2. **Register services** in Admin dashboard:
   - Service name
   - Service URL
   - Service port
   - Description

3. **Update configurations**:
   - Each service gets own database
   - Update connection strings
   - Set environment variables

4. **Test health checks**:
   - Admin Service should automatically check health
   - Verify logs streaming works
   - Test enable/disable controls

## Troubleshooting

### Services Not Showing as Healthy
- Verify health check endpoints are accessible
- Check logs: `http://localhost:8080/api/services/{id}/logs`
- Ensure services are running on correct ports

### Database Connection Issues
- Verify PostgreSQL containers are healthy: `docker-compose ps`
- Check database credentials in compose file
- Review service logs for connection errors

### Admin Dashboard Not Loading
- Ensure Admin Service is running: `curl http://localhost:8080/api/services`
- Check browser console for errors
- Verify JWT token configuration

## Security Considerations

- Change default admin credentials immediately
- Use strong JWT secret in production
- Enable HTTPS in reverse proxy
- Implement API authentication for sub-services
- Use environment variables for sensitive data
- Regular security updates for all services

## Next Steps

1. ✅ Restructure to admin-controlled architecture
2. ⏳ Add authentication/authorization to Admin Service
3. ⏳ Implement advanced monitoring and alerting
4. ⏳ Add service auto-scaling capabilities
5. ⏳ Create mobile app for dashboard
6. ⏳ Implement backup and disaster recovery
7. ⏳ Set up CI/CD pipeline

## Support

For issues or questions:
1. Check service logs via dashboard
2. Review health status of each service
3. Consult individual service documentation
4. Check Admin Service logs at `/api/services/{id}/logs`

---

**Architecture Version**: 2.0 (Admin-Controlled Microservices)
**Last Updated**: December 14, 2025
