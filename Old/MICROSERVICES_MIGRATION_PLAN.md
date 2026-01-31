# Microservices Migration Plan
**KR_API Modular Mono-Repo Restructuring**

*Date: December 6, 2025*  
*Current Branch: Development*  
*Status: Planning Phase*

---

## Executive Summary

Transform the current monolithic KR_API application into a **microservices-ready mono-repo** architecture where each major component (dashboards, webhooks, payment processing, pressbox, feedback) runs as an independent Spring Boot service. Services can run individually or as a unified application suite.

**Key Goals:**
- Independent service deployment and scaling
- Clear separation of concerns by business domain
- Shared code management via common library
- Flexible deployment: run all services together or individually
- Maintain backward compatibility during migration

---

## Current Architecture (Monolithic)

```
KR_API/
├── src/main/java/com/example/simpletixwebhook/
│   ├── controller/
│   │   ├── AnalyticsController.java
│   │   ├── AuthController.java
│   │   ├── DashboardController.java
│   │   ├── FeedbackAdminController.java
│   │   ├── FeedbackController.java
│   │   ├── WebhookController.java
│   │   ├── (15+ other controllers - all mixed together)
│   │   └── dto/ (various DTOs)
│   ├── model/ (30+ entities)
│   ├── repository/ (20+ repositories)
│   ├── service/ (15+ services)
│   ├── config/ (OpenAPIConfig, SecurityConfig, SquareDataSourceConfig)
│   └── util/ (various utilities)
├── src/main/resources/
│   ├── static/ (dashboard.html, gate.html, pressbox.html, etc.)
│   └── application.properties
├── payment-service/ (separate module, partially isolated)
├── feedback-app/ (Node.js - separate app)
└── pom.xml (main app POM)
```

**Issues:**
- All controllers and services in one deployment unit
- Cannot scale individual features independently
- Difficult to assign team ownership to specific domains
- Changes to any feature require full application redeployment
- Complex dependency graph - hard to understand what depends on what

---

## Proposed Architecture (Modular Mono-Repo)

```
KR_API/
├── pom.xml                              # Parent POM - orchestrates all modules
├── README.md                            # Architecture overview
├── docker-compose.yml                   # Run all services together
│
├── common-lib/                          # Shared code module
│   ├── pom.xml
│   ├── src/main/java/com/example/common/
│   │   ├── model/                       # Shared JPA entities
│   │   │   ├── User.java
│   │   │   ├── Permission.java
│   │   │   ├── Subscription.java
│   │   │   ├── TicketSale.java
│   │   │   └── (other shared entities)
│   │   ├── dto/                         # Shared DTOs
│   │   ├── config/                      # Shared configs
│   │   │   ├── BaseSecurityConfig.java
│   │   │   └── DatabaseConfig.java
│   │   ├── util/                        # Utilities
│   │   └── exception/                   # Custom exceptions
│   └── README.md
│
├── dashboard-service/                   # Analytics & Dashboard UI
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/main/java/com/example/dashboard/
│   │   ├── DashboardServiceApplication.java
│   │   ├── controller/
│   │   │   ├── DashboardController.java
│   │   │   └── AnalyticsController.java
│   │   ├── service/
│   │   │   ├── DashboardService.java
│   │   │   └── AnalyticsService.java
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── application.yml              # Port: 8082
│   │   ├── static/
│   │   │   ├── dashboard.html
│   │   │   └── analytics/
│   │   └── templates/
│   └── README.md
│
├── webhook-service/                     # Webhook receivers (Square, SimpleTix, etc.)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/main/java/com/example/webhook/
│   │   ├── WebhookServiceApplication.java
│   │   ├── controller/
│   │   │   ├── WebhookController.java
│   │   │   └── SimpleTixWebhookController.java
│   │   ├── service/
│   │   │   ├── WebhookProcessingService.java
│   │   │   └── EventPublisher.java
│   │   └── config/
│   │   │   └── RabbitMQConfig.java      # For event publishing
│   ├── src/main/resources/
│   │   └── application.yml              # Port: 8083
│   └── README.md
│
├── pressbox-service/                    # Pressbox & Gate Management
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/main/java/com/example/pressbox/
│   │   ├── PressboxServiceApplication.java
│   │   ├── controller/
│   │   │   ├── PressboxController.java
│   │   │   ├── GateController.java
│   │   │   └── ManualEntryController.java
│   │   ├── service/
│   │   │   ├── PressboxService.java
│   │   │   └── GateService.java
│   │   ├── model/                       # Pressbox-specific entities
│   │   └── repository/
│   ├── src/main/resources/
│   │   ├── application.yml              # Port: 8084
│   │   └── static/
│   │       ├── pressbox.html
│   │       └── gate.html
│   └── README.md
│
├── payment-service/                     # Payment & Subscription Management
│   ├── pom.xml                          # Already exists - minimal changes
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/main/java/com/example/payment/
│   │   ├── PaymentServiceApplication.java
│   │   ├── controller/
│   │   │   ├── PayController.java
│   │   │   ├── AuthController.java
│   │   │   ├── SubscriptionController.java
│   │   │   └── RefundController.java
│   │   ├── service/
│   │   │   ├── UserService.java
│   │   │   ├── SubscriptionService.java
│   │   │   └── RefundService.java
│   │   ├── model/
│   │   └── repository/
│   ├── src/main/resources/
│   │   ├── application.yml              # Port: 8081
│   │   └── static/
│   │       ├── subscription-manager.html
│   │       └── pay.html
│   ├── subscription-docs/
│   └── README.md
│
├── feedback-service/                    # Feedback & Survey Management
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/main/java/com/example/feedback/
│   │   ├── FeedbackServiceApplication.java
│   │   ├── controller/
│   │   │   ├── FeedbackController.java
│   │   │   ├── FeedbackAdminController.java
│   │   │   └── SurveyController.java
│   │   ├── service/
│   │   │   ├── FeedbackService.java
│   │   │   └── SurveyService.java
│   │   ├── model/
│   │   └── repository/
│   ├── src/main/resources/
│   │   ├── application.yml              # Port: 8085
│   │   └── static/
│   │       ├── feedback-admin.html
│   │       └── survey.html
│   └── README.md
│
└── scripts/
    ├── build-all.sh                     # Build all services
    ├── run-all-local.sh                 # Run all services locally
    └── deploy-all.sh                    # Deploy all services
```

---

## Service Breakdown

### **1. Common Library (`common-lib/`)**
**Purpose:** Shared code and models used by all services

**Contents:**
- **Entities:** User, Permission, Subscription, SubscriptionRefund, TicketSale, etc.
- **DTOs:** Shared request/response objects
- **Configs:** Base security, database connection pools
- **Utilities:** Date helpers, validation, encryption
- **Exceptions:** Custom exception classes

**Dependencies:** Spring Boot Starter Data JPA, PostgreSQL Driver, Jackson
**Port:** N/A (library only)
**Database:** N/A (entities defined here, used by services)

---

### **2. Dashboard Service (`dashboard-service/`)**
**Purpose:** Real-time analytics, reporting, and dashboard UI

**Responsibilities:**
- Display analytics dashboards
- Generate reports on ticket sales, payments, subscriptions
- Real-time statistics aggregation
- Admin overview interface

**Controllers:**
- `DashboardController` - Main dashboard routes
- `AnalyticsController` - Analytics data APIs

**Key Features:**
- Real-time stats via WebSocket or SSE
- Listens to events from webhook-service and payment-service
- Aggregates data from multiple services

**Dependencies:** 
- `common-lib`
- Spring Web
- Spring Data JPA
- Spring Boot Actuator
- RabbitMQ or Kafka (event consumer)

**Port:** 8082
**Database:** Shared PostgreSQL (read-heavy)

---

### **3. Webhook Service (`webhook-service/`)**
**Purpose:** Receive and process external webhooks (Square, SimpleTix, etc.)

**Responsibilities:**
- Receive webhook events from external systems
- Validate webhook signatures
- Process payment notifications
- Publish domain events to message queue
- Store webhook logs for audit

**Controllers:**
- `WebhookController` - Main webhook receiver
- `SimpleTixWebhookController` - SimpleTix specific
- `SquareWebhookController` - Square specific

**Key Features:**
- Event-driven architecture (publishes to RabbitMQ/Kafka)
- Idempotent webhook processing (prevent duplicates)
- Webhook retry logic
- Signature validation

**Dependencies:**
- `common-lib`
- Spring Web
- Spring AMQP (RabbitMQ) or Spring Kafka
- Jackson for JSON processing

**Port:** 8083
**Database:** Shared PostgreSQL (write webhook logs)
**Message Queue:** RabbitMQ or Kafka

**Event Examples:**
```
PaymentReceivedEvent → payment-service, dashboard-service
TicketSaleEvent → pressbox-service, dashboard-service
RefundProcessedEvent → payment-service, dashboard-service
```

---

### **4. Pressbox Service (`pressbox-service/`)**
**Purpose:** Pressbox management, gate entry, manual ticket entry

**Responsibilities:**
- Manage pressbox credentials and access
- Gate entry scanning and validation
- Manual ticket entry for on-site sales
- Ticket verification

**Controllers:**
- `PressboxController` - Pressbox management
- `GateController` - Gate entry operations
- `ManualEntryController` - Manual ticket entry

**Key Features:**
- QR code/barcode scanning
- Real-time gate status
- Pressbox credential management
- On-site ticket sales

**Dependencies:**
- `common-lib`
- Spring Web
- Spring Data JPA
- Spring Security

**Port:** 8084
**Database:** Shared PostgreSQL (ticket data, credentials)

---

### **5. Payment Service (`payment-service/`)**
**Purpose:** Payment processing, subscription management, refunds (ALREADY EXISTS)

**Responsibilities:**
- Process payments via Square
- Subscription lifecycle management
- Refund requests and approval workflow
- User authentication for subscription portal

**Controllers:**
- `PayController` - Payment processing
- `AuthController` - User authentication
- `SubscriptionController` - Subscription CRUD
- `RefundController` - Refund management

**Key Features:**
- Square payment integration
- Subscription billing automation
- Multi-step refund approval
- Permission-based access control

**Dependencies:**
- `common-lib`
- Spring Web
- Spring Data JPA
- Spring Security
- Square SDK

**Port:** 8081
**Database:** Shared PostgreSQL (users, subscriptions, refunds)

---

### **6. Feedback Service (`feedback-service/`)**
**Purpose:** Customer feedback, surveys, issue tracking

**Responsibilities:**
- Collect customer feedback
- Survey management
- Issue tracking and resolution
- Admin feedback dashboard

**Controllers:**
- `FeedbackController` - Public feedback submission
- `FeedbackAdminController` - Admin management
- `SurveyController` - Survey operations

**Key Features:**
- Anonymous feedback collection
- Survey builder
- Issue categorization
- Admin response system

**Dependencies:**
- `common-lib`
- Spring Web
- Spring Data JPA

**Port:** 8085
**Database:** Shared PostgreSQL (feedback, surveys)

---

## Inter-Service Communication

### **Option 1: Event-Driven (Recommended)**

**Message Queue:** RabbitMQ or Apache Kafka

**Event Flow Example:**
1. Webhook Service receives Square payment webhook → validates → publishes `PaymentReceivedEvent`
2. Payment Service listens → updates subscription status
3. Dashboard Service listens → updates real-time analytics
4. Email Service listens → sends confirmation email

**Benefits:**
- Loose coupling between services
- Asynchronous processing
- Services can be offline temporarily
- Easy to add new consumers

**Implementation:**
```java
// Webhook Service - Publisher
@Service
public class EventPublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void publishPaymentEvent(PaymentReceivedEvent event) {
        rabbitTemplate.convertAndSend("payment.exchange", "payment.received", event);
    }
}

// Payment Service - Consumer
@Service
public class PaymentEventListener {
    @RabbitListener(queues = "payment.received.queue")
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        // Update subscription status
    }
}
```

### **Option 2: REST API Communication**

Services expose REST APIs that other services call directly.

**Example:**
- Dashboard Service needs subscription stats → calls `GET http://payment-service:8081/api/subscriptions/stats`
- Webhook Service needs to verify ticket → calls `GET http://pressbox-service:8084/api/tickets/{id}/verify`

**Implementation:**
```java
@Service
public class DashboardService {
    @Autowired
    private RestTemplate restTemplate;
    
    public SubscriptionStats getSubscriptionStats() {
        return restTemplate.getForObject(
            "http://payment-service:8081/api/subscriptions/stats",
            SubscriptionStats.class
        );
    }
}
```

**Benefits:**
- Simple to implement
- Synchronous responses
- Easy to debug

**Drawbacks:**
- Tight coupling
- Service must be online for calls to succeed
- Can create cascading failures

### **Option 3: Hybrid (Best of Both)**

- **Synchronous operations** (user-facing queries) → REST APIs
- **Asynchronous events** (background processing) → Message Queue

Example:
- User requests subscription details → Dashboard calls Payment Service REST API (immediate)
- Webhook receives payment → Publishes event to queue (async)

---

## Database Strategy

### **Option 1: Shared Database (Recommended for initial migration)**

**Single PostgreSQL instance** with all tables. Services access different tables.

**Pros:**
- Easy to maintain data consistency
- No data synchronization needed
- Simpler transaction management
- Lower infrastructure cost

**Cons:**
- Services coupled through database schema
- Difficult to scale individual databases
- Schema changes affect all services

**Schema Organization:**
```sql
-- User/Auth tables (used by payment-service, dashboard-service)
users, permissions

-- Payment tables (owned by payment-service)
subscriptions, subscription_refunds

-- Ticket tables (owned by pressbox-service)
ticket_sales, gate_entries

-- Feedback tables (owned by feedback-service)
feedback, surveys

-- Webhook tables (owned by webhook-service)
webhook_logs
```

### **Option 2: Database Per Service (Future state)**

Each service has its own PostgreSQL database.

**Migration path:**
1. Start with shared database
2. Identify service boundaries
3. Gradually split databases
4. Implement data synchronization via events

---

## Parent POM Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>kr-api-parent</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>KR API Parent</name>
    <description>Parent POM for KR API microservices</description>

    <modules>
        <module>common-lib</module>
        <module>dashboard-service</module>
        <module>webhook-service</module>
        <module>pressbox-service</module>
        <module>payment-service</module>
        <module>feedback-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring.boot.version>3.4.10</spring.boot.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common-lib</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring.boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <profiles>
        <!-- Build all services -->
        <profile>
            <id>all</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>

        <!-- Build only payment service -->
        <profile>
            <id>payment</id>
            <modules>
                <module>common-lib</module>
                <module>payment-service</module>
            </modules>
        </profile>

        <!-- Build only dashboard service -->
        <profile>
            <id>dashboard</id>
            <modules>
                <module>common-lib</module>
                <module>dashboard-service</module>
            </modules>
        </profile>

        <!-- Production build -->
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>

        <!-- Development build -->
        <profile>
            <id>dev</id>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
    </profiles>
</project>
```

---

## Docker Compose Configuration

### **Root `docker-compose.yml` - Run All Services**

```yaml
version: '3.8'

services:
  # Shared Infrastructure
  postgres:
    image: postgres:18
    container_name: kr-api-postgres
    environment:
      POSTGRES_DB: kr_api
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - kr-api-network

  rabbitmq:
    image: rabbitmq:3-management
    container_name: kr-api-rabbitmq
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
    networks:
      - kr-api-network

  # Microservices
  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    container_name: payment-service
    ports:
      - "8081:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - kr-api-network

  dashboard-service:
    build:
      context: ./dashboard-service
      dockerfile: Dockerfile
    container_name: dashboard-service
    ports:
      - "8082:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_RABBITMQ_HOST: rabbitmq
      PAYMENT_SERVICE_URL: http://payment-service:8080
    depends_on:
      - postgres
      - rabbitmq
      - payment-service
    networks:
      - kr-api-network

  webhook-service:
    build:
      context: ./webhook-service
      dockerfile: Dockerfile
    container_name: webhook-service
    ports:
      - "8083:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - kr-api-network

  pressbox-service:
    build:
      context: ./pressbox-service
      dockerfile: Dockerfile
    container_name: pressbox-service
    ports:
      - "8084:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - postgres
    networks:
      - kr-api-network

  feedback-service:
    build:
      context: ./feedback-service
      dockerfile: Dockerfile
    container_name: feedback-service
    ports:
      - "8085:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - postgres
    networks:
      - kr-api-network

networks:
  kr-api-network:
    driver: bridge

volumes:
  postgres-data:
```

### **Individual Service `docker-compose.yml` - Run Service Standalone**

Example: `payment-service/docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:18
    container_name: payment-postgres
    environment:
      POSTGRES_DB: kr_api
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  payment-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: payment-service
    ports:
      - "8081:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/kr_api
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - postgres

volumes:
  postgres-data:
```

---

## Migration Strategy

### **Phase 1: Foundation (Week 1)**

**Goal:** Set up infrastructure without breaking existing functionality

**Tasks:**
1. Create `common-lib/` module
2. Create parent `pom.xml`
3. Extract shared entities to `common-lib`
4. Update existing `payment-service` to depend on `common-lib`
5. Test existing functionality still works

**Validation:**
- Existing application builds successfully
- Payment service still works
- All tests pass

---

### **Phase 2: Service Extraction (Week 2-3)**

**Goal:** Create new service modules one at a time

**Order:**
1. **Dashboard Service** (least complex, high value)
   - Move `DashboardController`, `AnalyticsController`
   - Move dashboard UI files
   - Configure separate port (8082)
   - Test dashboard independently

2. **Webhook Service** (critical path)
   - Move `WebhookController`
   - Add RabbitMQ configuration
   - Implement event publishing
   - Test webhook reception

3. **Pressbox Service**
   - Move `PressboxController`, `GateController`
   - Move pressbox/gate UI files
   - Test gate operations

4. **Feedback Service**
   - Move `FeedbackController`, `FeedbackAdminController`
   - Move feedback UI files
   - Test feedback submission

**Validation:**
- Each service runs independently
- Each service passes health checks
- UI pages load correctly from each service

---

### **Phase 3: Integration (Week 4)**

**Goal:** Connect services via events and APIs

**Tasks:**
1. Set up RabbitMQ/Kafka
2. Implement event publishers in webhook-service
3. Implement event consumers in payment-service, dashboard-service
4. Add REST client calls where needed
5. Test end-to-end flows

**Test Scenarios:**
- Webhook received → Event published → Payment updated → Dashboard refreshed
- User creates subscription → Payment processed → Dashboard shows new subscription
- Gate entry scanned → Ticket verified → Entry logged

---

### **Phase 4: Deployment & Documentation (Week 5)**

**Goal:** Production-ready deployment

**Tasks:**
1. Create Dockerfiles for all services
2. Create root `docker-compose.yml`
3. Set up CI/CD pipelines per service
4. Write service-specific READMEs
5. Update main README with architecture overview
6. Create runbooks for common operations

**Deliverables:**
- All services containerized
- Docker compose brings up entire system
- Documentation complete
- Monitoring/logging configured

---

## Build & Run Commands

### **Build All Services**
```bash
# From root directory
mvn clean install

# Build specific service
mvn clean install -pl payment-service -am

# Build with profile
mvn clean install -P prod
```

### **Run Services - Development (Local)**
```bash
# Run all services with Docker Compose
docker-compose up -d

# Run single service
cd payment-service
docker-compose up -d

# Run without Docker (requires local PostgreSQL)
cd payment-service
mvn spring-boot:run
```

### **Run Services - Production**
```bash
# Build production images
mvn clean package -P prod
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Or deploy to Kubernetes
kubectl apply -f k8s/
```

### **Access Services**

| Service | URL | Health Check |
|---------|-----|--------------|
| Payment Service | http://localhost:8081 | http://localhost:8081/actuator/health |
| Dashboard Service | http://localhost:8082 | http://localhost:8082/actuator/health |
| Webhook Service | http://localhost:8083 | http://localhost:8083/actuator/health |
| Pressbox Service | http://localhost:8084 | http://localhost:8084/actuator/health |
| Feedback Service | http://localhost:8085 | http://localhost:8085/actuator/health |
| RabbitMQ Management | http://localhost:15672 | admin/admin |
| PostgreSQL | localhost:5432 | kr_api/postgres/postgres |

---

## Testing Strategy

### **Unit Tests**
Each service has its own unit tests in `src/test/java`

```bash
# Run all tests
mvn test

# Run tests for specific service
mvn test -pl payment-service
```

### **Integration Tests**
Test inter-service communication

```bash
# Start all services
docker-compose up -d

# Run integration test suite
mvn verify -P integration-tests
```

### **End-to-End Tests**
Simulate real user workflows across services

**Example Test Scenarios:**
1. User submits payment → Webhook received → Subscription created → Dashboard updated
2. Admin approves refund → Refund processed → Event published → Dashboard shows refund
3. Gate scans ticket → Ticket verified → Entry logged → Analytics updated

---

## Monitoring & Observability

### **Health Checks**
Each service exposes Spring Boot Actuator endpoints:
- `/actuator/health` - Service health
- `/actuator/info` - Service info
- `/actuator/metrics` - Metrics

### **Logging**
Centralized logging with ELK stack or similar:
- Each service logs to stdout
- Logs aggregated by log collector (Fluentd, Logstash)
- Stored in Elasticsearch
- Visualized in Kibana

**Log Format:**
```json
{
  "timestamp": "2025-12-06T03:14:36.420Z",
  "service": "payment-service",
  "level": "INFO",
  "message": "User authenticated successfully",
  "traceId": "abc123",
  "spanId": "xyz789"
}
```

### **Distributed Tracing**
Use Spring Cloud Sleuth + Zipkin to trace requests across services

**Example:**
```
User Request → Dashboard Service (span1)
  → Calls Payment Service API (span2)
    → Queries Database (span3)
  → Returns to Dashboard (span4)
```

### **Metrics**
Use Prometheus + Grafana:
- Service-level metrics (request count, latency, error rate)
- JVM metrics (heap, GC, threads)
- Database connection pool metrics
- RabbitMQ queue depth

---

## Security Considerations

### **Service-to-Service Authentication**
**Option 1:** Shared secret (simple, for initial implementation)
```yaml
# application.yml
service:
  auth:
    secret: ${SERVICE_AUTH_SECRET}
```

**Option 2:** JWT tokens (more secure)
- Each service has its own key pair
- Services validate JWT tokens from other services

**Option 3:** mTLS (most secure)
- Services authenticate via SSL certificates
- Requires certificate management infrastructure

### **API Gateway (Future Enhancement)**
Add Spring Cloud Gateway or Kong as single entry point:
```
User → API Gateway (8080) → Routes to services
  → /api/payments/** → payment-service:8081
  → /api/dashboard/** → dashboard-service:8082
  → /webhooks/** → webhook-service:8083
```

**Benefits:**
- Single SSL termination point
- Centralized rate limiting
- Request authentication
- Response caching

---

## Rollback Plan

If migration causes issues:

1. **Immediate Rollback:**
   - Revert to main branch (monolithic version)
   - Deploy monolithic version
   - All features work as before

2. **Partial Rollback:**
   - Keep working services deployed
   - Rollback problematic service to monolithic version
   - Gradually fix issues

3. **Data Consistency:**
   - Database schema compatible with both versions
   - No destructive migrations during initial rollout
   - Always keep backup before major changes

---

## Success Metrics

### **Technical Metrics**
- [ ] All services build successfully
- [ ] All services pass health checks
- [ ] 100% test coverage maintained
- [ ] No performance regression (response time < baseline + 10%)
- [ ] All existing features work correctly

### **Operational Metrics**
- [ ] Services can be deployed independently
- [ ] Zero-downtime deployments achieved
- [ ] Services can scale independently
- [ ] Clear ownership per service documented

### **Developer Experience**
- [ ] Onboarding time reduced (clear service boundaries)
- [ ] Build time improved (only rebuild changed service)
- [ ] Debugging easier (isolated logs per service)
- [ ] Documentation complete and accurate

---

## Estimated Effort

| Phase | Duration | Resources | Risk Level |
|-------|----------|-----------|------------|
| Phase 1: Foundation | 1 week | 1 developer | Low |
| Phase 2: Service Extraction | 2-3 weeks | 1-2 developers | Medium |
| Phase 3: Integration | 1 week | 1-2 developers | High |
| Phase 4: Deployment & Docs | 1 week | 1 developer | Low |
| **TOTAL** | **5-6 weeks** | **1-2 developers** | **Medium** |

**Assumptions:**
- Developers familiar with Spring Boot
- No major feature development during migration
- Database schema relatively stable
- Adequate testing environment available

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Service communication failures | High | Medium | Implement circuit breakers, fallbacks, comprehensive error handling |
| Data consistency issues | High | Medium | Start with shared database, add distributed transactions only if needed |
| Increased complexity | Medium | High | Comprehensive documentation, clear service boundaries, monitoring |
| Performance degradation | Medium | Low | Load testing before rollout, optimize hot paths, caching |
| Team learning curve | Medium | Medium | Training sessions, pair programming, gradual rollout |
| Infrastructure costs | Low | High | Run only necessary services in dev, optimize container resources |

---

## Next Steps

### **Immediate Actions (Before Starting Migration)**
1. **Review & Approve Architecture** - Team review of this plan
2. **Create Feature Branch** - `feature/microservices-migration`
3. **Set Up Dev Environment** - Ensure Docker, Maven, PostgreSQL available
4. **Backup Current State** - Tag current version as `v1.0-monolithic`

### **Phase 1 Kickoff (Week 1)**
1. Create `common-lib/` module
2. Set up parent `pom.xml`
3. Run existing tests to establish baseline
4. Begin extracting shared models

### **Communication Plan**
- Daily standups to track progress
- Weekly demos of working services
- Documentation updates with each phase
- Stakeholder updates on milestones

---

## Appendix A: Service Port Assignments

| Service | Port | Protocol | Purpose |
|---------|------|----------|---------|
| Payment Service | 8081 | HTTP | Payment processing, subscriptions |
| Dashboard Service | 8082 | HTTP | Analytics dashboards |
| Webhook Service | 8083 | HTTP | External webhook reception |
| Pressbox Service | 8084 | HTTP | Pressbox, gate management |
| Feedback Service | 8085 | HTTP | Feedback, surveys |
| PostgreSQL | 5432 | PostgreSQL | Database |
| RabbitMQ AMQP | 5672 | AMQP | Message queue |
| RabbitMQ Management | 15672 | HTTP | Queue management UI |

---

## Appendix B: Event Catalog

### **Payment Events**
- `payment.received` - Square payment received
- `subscription.created` - New subscription created
- `subscription.canceled` - Subscription canceled
- `refund.requested` - Refund request submitted
- `refund.approved` - Refund approved by admin
- `refund.completed` - Refund processed in Square

### **Ticket Events**
- `ticket.sold` - Ticket sale completed
- `ticket.scanned` - Ticket scanned at gate
- `ticket.verified` - Ticket verification completed
- `ticket.invalidated` - Ticket marked invalid

### **Webhook Events**
- `webhook.received` - External webhook received
- `webhook.processed` - Webhook processing completed
- `webhook.failed` - Webhook processing failed

### **User Events**
- `user.created` - New user registered
- `user.login` - User logged in
- `user.permissions.updated` - User permissions changed

---

## Appendix C: API Documentation Standards

Each service should expose OpenAPI/Swagger documentation:

**URL Pattern:** `http://localhost:{port}/swagger-ui.html`

**Example:**
- Payment Service: http://localhost:8081/swagger-ui.html
- Dashboard Service: http://localhost:8082/swagger-ui.html

**Documentation Requirements:**
- All endpoints documented
- Request/response examples
- Authentication requirements
- Error responses
- Rate limiting info

---

## Appendix D: Database Schema Ownership

| Table Name | Owner Service | Read Access | Write Access |
|------------|---------------|-------------|--------------|
| users | payment-service | All | payment-service only |
| permissions | payment-service | All | payment-service only |
| subscriptions | payment-service | dashboard, webhook | payment-service only |
| subscription_refunds | payment-service | dashboard | payment-service only |
| ticket_sales | pressbox-service | dashboard, webhook | pressbox-service only |
| gate_entries | pressbox-service | dashboard | pressbox-service only |
| feedback | feedback-service | dashboard | feedback-service only |
| surveys | feedback-service | dashboard | feedback-service only |
| webhook_logs | webhook-service | dashboard | webhook-service only |

**Convention:** Service that "owns" table is the only one that writes to it. Others can read via APIs.

---

## References & Resources

### **Spring Boot Microservices**
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Boot Best Practices](https://spring.io/guides)
- [Microservices Patterns](https://microservices.io/patterns/)

### **Event-Driven Architecture**
- [RabbitMQ Tutorial](https://www.rabbitmq.com/tutorials/)
- [Spring AMQP Guide](https://spring.io/guides/gs/messaging-rabbitmq/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

### **Docker & Deployment**
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)

---

**Document Version:** 1.0  
**Last Updated:** December 6, 2025  
**Status:** Planning - Pending Approval  
**Next Review:** Before Phase 1 Kickoff
