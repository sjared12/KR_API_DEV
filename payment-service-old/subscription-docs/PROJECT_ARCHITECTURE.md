# Project Architecture

## Module Structure

This is a **multi-module Maven project** with two main applications:

### 1. Main Application (SimpleTix Webhook API)
**Location:** `src/main/java/com/example/simpletixwebhook/`

**Purpose:** Handles SimpleTix webhook events, ticket scanning, press box announcements, surveys, and analytics.

**Modules:**
- **Controllers:** Webhook handlers, dashboard pages, feedback management
- **Models:** TicketSale, TicketScan, PressBoxAnnouncement, FeedbackResponse
- **Services:** Event processing, analytics calculation
- **Repositories:** Data access for SimpleTix events

**Key Endpoints:**
- `/webhook/simpletix/*` - SimpleTix webhook ingestion
- `/dashboard`, `/gate`, `/pressbox`, `/survey` - Public pages
- `/api/feedback/*` - Survey feedback collection

---

### 2. Payment Service Module
**Location:** `payment-service/src/main/java/com/example/simpletixwebhook/`

**Purpose:** Handles all payment-related functionality including payment plans, Square integration, and subscription management.

**Sub-Features:**

#### A. Payment Plans & Square Integration
- **Models:** PaymentPlan, Square webhook events
- **Controllers:** PaymentPlanAdminController, PayController
- **Services:** SquareInvoiceService, payment processing
- **Endpoints:**
  - `/api/plans/*` - Payment plan management
  - `/api/payments/*` - Payment processing
  - `/pay/*` - Payment forms
  - `/api/square/*` - Square webhook handling

#### B. Subscription Management (NEW)
- **Models:** 
  - `User` - User with bcrypt password hashing
  - `Permission` - Enum with 8 permission types
  - `Subscription` - Square subscription tracking
  - `SubscriptionRefund` - Refund workflow with approval
  
- **Repositories:**
  - `UserRepository` - User authentication queries
  - `SubscriptionRepository` - Subscription queries with search/filter
  - `SubscriptionRefundRepository` - Complex refund queries
  
- **Services:**
  - `UserService` - Authentication, password management, permissions
  - `SubscriptionService` - CRUD, status management, payment tracking
  - `RefundService` - Refund request/approval/processing with fee calculation
  
- **Controllers:**
  - `AuthController` - User login, management, permissions (11 endpoints)
  - `SubscriptionController` - Subscription management (11 endpoints)
  - `RefundController` - Refund workflow (15+ endpoints)
  
- **GUI:**
  - `subscription-manager.html` - Complete web interface with login, dashboard, admin panel

- **Endpoints:**
  - `/api/auth/*` - User authentication and management
  - `/api/subscriptions/*` - Subscription CRUD and management
  - `/api/refunds/*` - Refund workflow management
  - `/subscription-manager` - GUI (pretty URL)

---

## Database Schema

### Main Database (SimpleTix)
Tables for ticket sales, scans, and feedback:
- `ticket_sale` - Ticket sales from SimpleTix
- `ticket_scan` - Gate scans
- `press_box_announcement` - Announcements
- `feedback_response` - Survey responses

### Payment Service Database
Tables for payments and subscriptions:
- `payment_plan` - Payment plans
- `users` - User accounts with bcrypt passwords
- `user_permissions` - Permission associations
- `subscriptions` - Square subscriptions
- `subscription_refunds` - Refund requests and status

---

## Deployment

### Two Separate Applications

The project deploys as **two separate Spring Boot applications**:

1. **SimpleTix Webhook API** (main module)
   - Jar: `target/simpletix-webhook-0.0.1-SNAPSHOT.jar`
   - Port: 8080
   - Handles: Webhooks, ticket scanning, analytics

2. **Payment Service** (payment-service module)
   - Jar: `payment-service/target/payment-service-0.0.1-SNAPSHOT.jar`
   - Port: 8081 (configurable)
   - Handles: Payments, subscriptions, refunds

### Build Commands

```bash
# Build entire project
mvn clean package -DskipTests

# Build only main app
mvn clean package -DskipTests -pl simpletix-webhook

# Build only payment service
mvn clean package -DskipTests -pl payment-service
```

### Docker Deployment

Each application has its own Dockerfile:
- `Dockerfile` - Main SimpleTix application
- `payment-service/Dockerfile` - Payment service

---

## Configuration

### Main Application (SimpleTix)
**File:** `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/simpletixdb
spring.datasource.username=postgres
spring.datasource.password=secret
APP_ADMIN_USER=admin
APP_ADMIN_PASS=changeit
```

### Payment Service
**File:** `payment-service/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentdb
spring.datasource.username=postgres
spring.datasource.password=secret
SQUARE_API_TOKEN=your_square_token
SQUARE_LOCATION_ID=your_location_id
subscription.refund.processing-fee-percent=2.5
```

---

## API Integration

### Calling Payment Service from Main App

If you need to call payment APIs from the main application, use HTTP clients:

```java
// Example: Call subscription API
RestTemplate restTemplate = new RestTemplate();
String paymentServiceUrl = "http://localhost:8081/api/subscriptions";

HttpHeaders headers = new HttpHeaders();
headers.setBasicAuth(username, password);
headers.setContentType(MediaType.APPLICATION_JSON);

HttpEntity<SubscriptionRequest> request = new HttpEntity<>(sub, headers);
ResponseEntity<Subscription> response = restTemplate.exchange(
    paymentServiceUrl, 
    HttpMethod.POST, 
    request, 
    Subscription.class
);
```

---

## File Organization

```
KR_API/
├── src/                           # Main SimpleTix Application
│   ├── main/java/
│   │   └── com/example/simpletixwebhook/
│   │       ├── controller/        # Webhook, dashboard, feedback
│   │       ├── model/             # TicketSale, TicketScan
│   │       ├── repository/        # Data access
│   │       └── service/           # Business logic
│   └── resources/
│       └── static/                # HTML pages (dashboard, gate, pressbox)
│
├── payment-service/               # Payment Service Module
│   ├── src/main/java/
│   │   └── com/example/simpletixwebhook/
│   │       ├── controller/
│   │       │   ├── AuthController.java
│   │       │   ├── SubscriptionController.java
│   │       │   ├── RefundController.java
│   │       │   ├── PaymentPlanAdminController.java
│   │       │   └── PayController.java
│   │       ├── model/
│   │       │   ├── User.java
│   │       │   ├── Permission.java
│   │       │   ├── Subscription.java
│   │       │   ├── SubscriptionRefund.java
│   │       │   └── PaymentPlan.java
│   │       ├── repository/
│   │       │   ├── UserRepository.java
│   │       │   ├── SubscriptionRepository.java
│   │       │   └── SubscriptionRefundRepository.java
│   │       ├── service/
│   │       │   ├── UserService.java
│   │       │   ├── SubscriptionService.java
│   │       │   ├── RefundService.java
│   │       │   └── PaymentService.java
│   │       └── config/
│   └── resources/
│       ├── static/
│       │   └── subscription-manager.html
│       └── application.properties
│
├── pom.xml                        # Parent POM
├── payment-service/pom.xml        # Payment Service POM
├── Dockerfile                     # Main app Docker
├── payment-service/Dockerfile     # Payment service Docker
└── README.md
```

---

## Development Workflow

### Local Development

1. **Start PostgreSQL**
   ```bash
   docker run -d --name postgres -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:12
   ```

2. **Build Both Applications**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run Main Application**
   ```bash
   java -jar target/simpletix-webhook-0.0.1-SNAPSHOT.jar
   ```
   - Access: `http://localhost:8080/`

4. **Run Payment Service** (in another terminal)
   ```bash
   java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar
   ```
   - Access: `http://localhost:8081/subscription-manager`

### Testing Subscription Features

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"changeit"}'

# View subscriptions (requires auth)
curl -u admin:changeit http://localhost:8081/api/subscriptions

# Request refund
curl -X POST -u admin:changeit \
  http://localhost:8081/api/refunds/request \
  -H "Content-Type: application/json" \
  -d '{"subscriptionId":1,"requestedAmount":10000}'
```

---

## Architecture Benefits

✅ **Separation of Concerns** - Payment logic isolated in payment-service  
✅ **Scalability** - Services can scale independently  
✅ **Independent Deployment** - Update payments without redeploying main app  
✅ **Technology Flexibility** - Different tech stacks possible (future)  
✅ **Team Parallelization** - Different teams work on different services  
✅ **Clear Responsibilities** - Each service has defined scope  

---

## Migration Notes

The subscription management system was built as part of the payment-service module to:
- Keep all payment-related functionality together
- Reuse payment service infrastructure
- Enable shared database (or separate as needed)
- Maintain clear API boundaries
- Support multi-module deployment

This structure supports future enhancements like:
- Mobile app integration (calls payment-service APIs)
- Third-party integrations (webhook to payment-service)
- Admin dashboard (calls both services as needed)
