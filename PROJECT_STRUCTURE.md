# Project Structure - Admin-Controlled Architecture

## Complete Directory Tree

```
KR_API/
├── admin-service/                    [NEW] Central Admin Service
│   ├── pom.xml                       Module configuration
│   ├── Dockerfile                    Docker image for admin service
│   └── src/
│       ├── main/
│       │   ├── java/com/example/adminservice/
│       │   │   ├── AdminServiceApplication.java
│       │   │   ├── config/
│       │   │   │   └── AppConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── AdminUserController.java
│       │   │   │   ├── ServiceRegistryController.java
│       │   │   │   └── DashboardController.java
│       │   │   ├── model/
│       │   │   │   ├── AdminUser.java
│       │   │   │   └── ManagedService.java
│       │   │   ├── repository/
│       │   │   │   ├── AdminUserRepository.java
│       │   │   │   └── ManagedServiceRepository.java
│       │   │   └── service/
│       │   │       ├── AdminUserService.java
│       │   │       └── ServiceRegistryService.java
│       │   └── resources/
│       │       ├── application.properties
│       │       └── static/
│       │           └── dashboard.html          [Beautiful Admin Dashboard]
│       └── test/
│           └── java/com/example/adminservice/
│
├── payment-service/                  [Existing] Payment & Subscription Service
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/simpletixwebhook/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── static/
│   │   │       └── templates/
│   │   └── test/
│   └── subscription-docs/
│       ├── API_EXAMPLES.md
│       ├── DEPLOYMENT_GUIDE.md
│       ├── IMPLEMENTATION_COMPLETE.md
│       ├── IMPLEMENTATION_SUMMARY.md
│       ├── PROJECT_ARCHITECTURE.md
│       ├── README.md
│       ├── SUBSCRIPTION_GUI_GUIDE.md
│       ├── SUBSCRIPTION_MANAGEMENT.md
│       ├── SUBSCRIPTION_QUICK_START.md
│       └── SUBSCRIPTION_REORGANIZATION.md
│
├── Logger/                           [Existing] Logging Service
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/example/logapi/
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│
├── feedback-app/                     [Existing] Feedback Collection App
│   ├── Dockerfile
│   ├── package.json
│   ├── README.md
│   ├── server.js
│   └── public/
│       └── index.html
│
├── frontend/                         [Existing] Main Frontend
│   └── src/
│       └── pages/
│           └── pay/
│               └── PayStudent.jsx
│
├── admin-portal/                     [Existing - Can be deprecated]
│   ├── Dockerfile
│   ├── package.json
│   ├── server.js
│   └── public/
│       └── index.html
│
├── docker/                           [Infrastructure]
│   ├── db/
│   │   └── init.sql
│   ├── landing/
│   │   └── index.html
│   └── nginx/
│       ├── default.conf
│       └── Dockerfile
│
├── scripts/                          [Utilities]
│   ├── seed-issues.js
│   └── setup-forward-logs.sh
│
├── src/                              [Main Application - Can be refactored]
│   ├── main/
│   │   ├── java/com/example/simpletixwebhook/
│   │   │   ├── SimpletixWebhookApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       │   ├── dashboard.html
│   │       │   ├── feedback-admin.html
│   │       │   ├── gate.html
│   │       │   ├── index.html
│   │       │   ├── manual-entry.html
│   │       │   ├── manual-sale.html
│   │       │   ├── pay.html
│   │       │   ├── pressbox.html
│   │       │   ├── survey.html
│   │       │   ├── admin/
│   │       │   │   └── plans.html
│   │       │   └── analytics/
│   │       │       └── index.html
│   │       └── templates/
│   │           ├── pay-error.html
│   │           └── pay-form.html
│   └── test/
│       ├── java/com/example/simpletixwebhook/
│       │   ├── controller/
│       │   └── ...
│       └── resources/
│           └── application.properties
│
├── pom.xml                           [Parent POM - Updated]
├── compose.yaml                      [Docker Compose - Updated]
├── Dockerfile                        [Main App Dockerfile]
├── application.properties
├── .env.example
├── .gitignore
├── README.md
├── GETTING_STARTED.md
├── SECURITY.md
├── GATE_BACKGROUND_SETUP.md
├── PRESSBOX_SETUP.md
├── MICROSERVICES_MIGRATION_PLAN.md
├── ARCHITECTURE_V2.md                [NEW] Complete Architecture Guide
├── QUICKSTART_ADMIN_SERVICE.md       [NEW] Quick Start Guide
└── ADMIN_SERVICE_IMPLEMENTATION.md   [NEW] Implementation Details
```

## Key Modules

### 1. Admin Service [NEW]
**Purpose**: Central management and monitoring hub
- Service registration and discovery
- Health monitoring
- Log aggregation
- User authentication
- Beautiful dashboard UI

**Port**: 8080

### 2. Payment Service [EXISTING]
**Purpose**: Payment processing and subscription management
- Square API integration
- Subscription management
- Refund processing
- Payment tracking

**Port**: 8081

### 3. Logger Service [EXISTING]
**Purpose**: Centralized logging
- Event logging
- Log storage
- Log retrieval

**Port**: 8082

### 4. Feedback App [EXISTING]
**Purpose**: Event feedback collection
- Survey distribution
- Feedback collection
- Analytics

**Port**: 3000

### 5. Frontend [EXISTING]
**Purpose**: Main user interface
- Ticket purchasing
- Event management
- Student payments

### 6. Admin Portal [EXISTING - DEPRECATED]
**Note**: Can be replaced by the new Admin Service Dashboard

## Database Structure

```
admin_db (Port 5433)
├── admin_users
└── managed_services

payment_db (Port 5434)
├── payment-related tables
├── subscription tables
└── refund tables

logger_db (Port 5435)
└── log tables
```

## API Architecture

```
Nginx Reverse Proxy (Port 80)
    │
    ├── /                      → Admin Service (8080)
    ├── /api/services          → Admin Service API
    ├── /api/admin/users       → Admin Service API
    ├── /payment/*             → Payment Service (8081)
    ├── /logger/*              → Logger Service (8082)
    ├── /feedback/*            → Feedback App (3000)
    └── /api/*                 → Route to appropriate service
```

## Service Deployment

### Docker Compose Services

```yaml
Services:
  1. admin-service      - Spring Boot, Port 8080
  2. payment-service    - Spring Boot, Port 8081
  3. logger-service     - Spring Boot, Port 8082
  4. feedback-app       - Node.js, Port 3000
  5. nginx              - Reverse Proxy, Port 80/443

Databases:
  1. admin-db          - PostgreSQL, Port 5433
  2. payment-db        - PostgreSQL, Port 5434
  3. logger-db         - PostgreSQL, Port 5435

Networks:
  - simpletix-network  - Internal communication
```

## Configuration Files

### Root Level
- `pom.xml` - Parent Maven configuration
- `compose.yaml` - Docker Compose orchestration
- `Dockerfile` - Main app Docker image
- `.env.example` - Environment variable template
- `application.properties` - Root app configuration

### Admin Service
- `admin-service/pom.xml` - Module configuration
- `admin-service/Dockerfile` - Admin service Docker image
- `admin-service/src/main/resources/application.properties` - Admin service config
- `admin-service/src/main/resources/static/dashboard.html` - UI

### Payment Service
- `payment-service/pom.xml` - Module configuration
- `payment-service/Dockerfile` - Docker image
- `payment-service/src/main/resources/application.properties` - Configuration

### Logger Service
- `Logger/pom.xml` - Module configuration
- `Logger/Dockerfile` - Docker image

### Feedback App
- `feedback-app/package.json` - Node dependencies
- `feedback-app/Dockerfile` - Docker image
- `feedback-app/server.js` - Main server file

## Documentation Files

### Architecture & Design
- `ARCHITECTURE_V2.md` - New admin-controlled architecture
- `MICROSERVICES_MIGRATION_PLAN.md` - Migration guide
- `payment-service/subscription-docs/PROJECT_ARCHITECTURE.md` - Payment service architecture

### Quick Starts & Getting Started
- `QUICKSTART_ADMIN_SERVICE.md` - 5-minute setup guide
- `GETTING_STARTED.md` - General getting started
- `payment-service/subscription-docs/SUBSCRIPTION_QUICK_START.md` - Payment quick start

### Deployment Guides
- `payment-service/subscription-docs/DEPLOYMENT_GUIDE.md` - Deployment instructions
- `SECURITY.md` - Security best practices

### Feature Specific
- `GATE_BACKGROUND_SETUP.md` - Gate background setup
- `PRESSBOX_SETUP.md` - Pressbox configuration

### Subscription System
- `payment-service/subscription-docs/SUBSCRIPTION_MANAGEMENT.md`
- `payment-service/subscription-docs/SUBSCRIPTION_GUI_GUIDE.md`
- `payment-service/subscription-docs/IMPLEMENTATION_COMPLETE.md`

## Build & Deployment

### Build All
```bash
mvn clean package -DskipTests
```

### Build Individual Modules
```bash
mvn clean package -DskipTests -pl admin-service
mvn clean package -DskipTests -pl payment-service
```

### Docker Compose
```bash
docker-compose build
docker-compose up -d
```

## Port Usage Summary

| Component | Port | Type | Purpose |
|-----------|------|------|---------|
| Nginx Proxy | 80 | Public | Single entry point |
| Admin Service | 8080 | Internal | Dashboard & APIs |
| Payment Service | 8081 | Internal | Payments |
| Logger Service | 8082 | Internal | Logging |
| Feedback App | 3000 | Internal | Feedback |
| Admin DB | 5433 | Internal | Admin data |
| Payment DB | 5434 | Internal | Payment data |
| Logger DB | 5435 | Internal | Log data |

## What to Update

### For Existing Sub-Services
To integrate with the new architecture, ensure each service exposes:

1. **Health Check Endpoint**
   ```
   GET /api/health
   Response: { "status": "UP" }
   ```

2. **Logs Endpoint**
   ```
   GET /api/logs?lines=100
   Response: { "logs": "..." }
   ```

3. **Metrics Endpoint**
   ```
   GET /api/metrics
   Response: { "memory": "...", "cpu": "..." }
   ```

## Next Steps

1. ✅ Admin Service created and fully functional
2. ✅ Docker Compose updated for new architecture
3. ✅ Documentation complete
4. ⏳ Implement endpoints in sub-services
5. ⏳ Register services in dashboard
6. ⏳ Test health checks and monitoring
7. ⏳ Set up production deployment

---

**Project Structure Version**: 2.0 (Admin-Controlled Microservices)
**Last Updated**: December 14, 2025
