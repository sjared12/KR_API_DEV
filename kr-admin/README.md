# KR Admin - Unified Payment & Checkout Portal

A unified Spring Boot application combining payment management and e-commerce checkout functionality with local authentication. Deployed on **DigitalOcean App Platform**.

## Features

### Authentication & Authorization
- Local user authentication (no Keycloak required)
- JWT-based token authentication
- Role-based access control (ADMIN, PAYMENTS_MANAGER, CHECKOUT_MANAGER, USER)
- Password encryption using BCrypt

### Payment Management
- Payment plan creation and management
- Payment tracking and history
- Recurring payment processing
- Refund management

### Checkout & Inventory
- Item/product inventory management
- Shopping cart and order management
- Order tracking and status updates
- Order line items and pricing

### Single Database
- PostgreSQL backend (single `kr_admin` database)
- Consolidated entities for payments and checkout
- Referential integrity and transaction support

## DigitalOcean Deployment

### App Platform Configuration

This application is deployed on DigitalOcean App Platform using the `.do/app.yaml` configuration. The application runs as a stateless service with:

- **Instance Size:** apps-s-1vcpu-0.5gb (can be scaled up)
- **Port:** 8080 (internal)
- **HTTP Route:** `/` (root path)
- **Environment:** Production with automatic HTTPS

### Environment Variables

Configure these in DigitalOcean App Platform (Settings → Component → Edit):

```
SERVER_PORT=8080
SPRING_DATASOURCE_URL=postgresql://user:password@db-host:5432/kr_admin
SPRING_DATASOURCE_USERNAME=db_user
SPRING_DATASOURCE_PASSWORD=db_password
HIKARI_MAX_POOL_SIZE=10
JWT_SECRET=your-long-secure-secret-key-min-32-chars
JWT_EXPIRATION=86400
SPRING_PROFILES_ACTIVE=production
```

### Database Setup on DigitalOcean

1. Create a PostgreSQL database on DigitalOcean:
   - Cluster name: `kr-admin-db`
   - Database name: `kr_admin`
   - Create a dedicated database user with permissions

2. Connection details available in DigitalOcean console:
   - Host: `db-xyz.ondigitalocean.com`
   - Port: `25060`
   - User: `doadmin`
   - Database: `kr_admin`

3. Add the database connection string to App Platform environment variables

### Deployment Process

1. **Push to Development branch:**
   ```bash
   git add .
   git commit -m "Update kr-admin for deployment"
   git push origin Development
   ```

2. **DigitalOcean automatically deploys** on push (if auto-deploy is enabled)

3. **Monitor deployment:**
   - Go to DigitalOcean Dashboard → Apps
   - Select `kr-admin` app
   - View deployment logs and status

4. **Access application:**
   - Production URL: `https://kr-admin.ondigitalocean.com` (or custom domain)
   - Health check: `GET https://kr-admin.ondigitalocean.com/actuator/health`

### Docker Build

DigitalOcean builds the Docker image automatically using this Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY pom.xml .
COPY src src
RUN apt-get update && apt-get install -y maven && mvn package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/kr-admin-0.0.1-SNAPSHOT.jar"]
```

### Scaling

To scale the application on DigitalOcean:
1. Dashboard → Apps → `kr-admin` → Settings
2. Increase "Instance Count" for horizontal scaling
3. Increase "Instance Size" for vertical scaling

### Database Backups

DigitalOcean PostgreSQL automatically handles:
- Daily backups (retained for 7 days)
- Point-in-time recovery
- Automatic high availability (optional)

Configure in: Dashboard → Databases → `kr-admin-db` → Backup

## Architecture

```
kr-admin/
├── src/main/java/com/krhscougarband/krajdmin/
│   ├── KRAdminApplication.java
│   ├── controllers/
│   │   ├── AuthController.java
│   │   ├── PaymentController.java      (TODO)
│   │   └── CheckoutController.java     (TODO)
│   ├── entities/
│   │   ├── User.java
│   │   ├── payment/
│   │   │   ├── PaymentPlan.java
│   │   │   └── PaymentRecord.java
│   │   └── checkout/
│   │       ├── Item.java
│   │       ├── Order.java
│   │       └── OrderItem.java
│   ├── repositories/
│   │   ├── UserRepository.java
│   │   ├── payment/
│   │   │   ├── PaymentPlanRepository.java
│   │   │   └── PaymentRecordRepository.java
│   │   └── checkout/
│   │       ├── ItemRepository.java
│   │       └── OrderRepository.java
│   ├── services/        (TODO)
│   ├── dtos/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RegisterRequest.java
│   │   └── UserResponse.java
│   └── security/
│       ├── SecurityConfig.java
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       ├── JwtAuthenticationEntryPoint.java
│       ├── UserPrincipal.java
│       └── CustomUserDetailsService.java
└── resources/
    ├── application.properties
    └── templates/        (TODO - add UI)
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with email/password
- `POST /api/auth/register` - Create new account
- `GET /api/auth/me` - Get current user info
- `POST /api/auth/logout` - Logout

### Payments (TODO)
- `GET /api/payments/plans` - List payment plans
- `POST /api/payments/plans` - Create payment plan
- `PUT /api/payments/plans/{id}` - Update payment plan
- `DELETE /api/payments/plans/{id}` - Delete payment plan

### Checkout (TODO)
- `GET /api/checkout/items` - List items
- `POST /api/checkout/items` - Create item
- `GET /api/checkout/orders` - List orders
- `POST /api/checkout/orders` - Create order

## Database Schema

Single PostgreSQL database with tables for:
- `users` - User accounts and authentication
- `user_roles` - Role assignments
- `payment_plans` - Payment plan details
- `payment_records` - Payment transactions
- `items` - Inventory items
- `orders` - Customer orders
- `order_items` - Order line items

## Configuration

Environment variables:
```
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kr_admin
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=86400
```

## Running Locally

```bash
# Prerequisites
- Java 21
- Maven 3.8+
- PostgreSQL 14+

# Clone and navigate
cd kr-admin

# Build
mvn clean package

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kr_admin
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password
export JWT_SECRET=your-secret-key-min-32-chars

# Run
java -jar target/kr-admin-0.0.1-SNAPSHOT.jar

# Application runs on http://localhost:8080
```

## Running with Docker Locally

```bash
# Build Docker image
docker build -t kr-admin:latest .

# Run with Docker
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/kr_admin \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your-secret-key \
  kr-admin:latest
```

## Running with Docker Compose

```bash
# Create docker-compose.yml with PostgreSQL + kr-admin
docker-compose up

# Application runs on http://localhost:8080
# PostgreSQL available on localhost:5432
```

## Next Steps

Development roadmap:
1. ✅ Local authentication system
2. ✅ Consolidated database schema
3. ⏳ Payment service layer and controllers
4. ⏳ Checkout service layer and controllers
5. ⏳ Unified admin UI with tabs for payments/checkout
6. ⏳ Payment processor integration (Square/Stripe)
7. ⏳ Email notifications
8. ⏳ Audit logging
9. ⏳ Comprehensive test suite

## Support & Troubleshooting

### Common Issues

**Database Connection Error:**
- Verify `SPRING_DATASOURCE_URL` environment variable is correct
- Check PostgreSQL credentials
- Ensure database `kr_admin` exists

**JWT Token Expired:**
- JWT tokens expire after `JWT_EXPIRATION` seconds (default: 86400 = 24 hours)
- Users must login again to get new token

**Deployment Issues on DigitalOcean:**
- Check App Platform logs: Dashboard → Apps → `kr-admin` → Logs
- Verify environment variables are set correctly
- Check database connection and permissions
- Review build output for compilation errors

### Performance Tuning

**Database Connections:**
- Adjust `HIKARI_MAX_POOL_SIZE` based on traffic
- Default: 10 connections
- Recommended: 15-20 for high-traffic deployments

**JVM Memory:**
- DigitalOcean apps: 512MB - 2GB RAM
- Adjust as needed: `java -Xmx512m -jar app.jar`

## Contact & Documentation

- **Repository:** https://github.com/sjared12/KR_API
- **Branch:** Development
- **Documentation:** See inline code comments
- **Issues:** GitHub Issues

## Migration from Previous Services

This unified application consolidates:
- **payment-service** → Payment plan management, refunds, transaction tracking
- **checkout-service** → Item inventory, orders, shopping functionality

### For Existing Users:

**Payment Service Users:**
- Navigate to `/admin/payments` tab for payment plan management
- API endpoints now at `/api/payments/*` instead of `/payments/api/*`
- Refunds managed in payments tab

**Checkout Service Users:**
- Navigate to `/admin/checkout` tab for item and order management
- API endpoints now at `/api/checkout/*` instead of `/checkout/api/*`
- All orders consolidated in single database

### Data Migration:
If migrating existing data from old services:
1. Create PostgreSQL backup from old databases
2. Use migration scripts to import data to `kr_admin` schema
3. Verify referential integrity
4. Update any external service endpoints to point to new application
